import io.socket.emitter.Emitter;
import jsclub.codefest2024.sdk.*;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;
import jsclub.codefest2024.sdk.model.enemies.Enemy;
import jsclub.codefest2024.sdk.model.obstacles.Obstacle;
import jsclub.codefest2024.sdk.model.players.Player;
import jsclub.codefest2024.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.*;

import static jsclub.codefest2024.sdk.model.ElementType.ENEMY;

public class Main {
    private static final String SERVER_URL = "https://cf-server.jsclub.dev";
    private static final String GAME_ID = "182052";
    private static final String PLAYER_NAME = "Nguoi Lua Gian Doi";

    private static int distance(Node x, Node y) {
        return Math.abs(x.x - y.x) + Math.abs(x.y - y.y);
    }
    private static int dis_weapon(Node x, Weapon y) {
        return (Math.abs(x.x - y.x) + Math.abs(x.y - y.y))*360/y.getRange();
    }
    private static Node add(Node x, Node y) {

        return new Node(x.x + y.x, x.y + y.y);
    }
    private static final List<Node> DIRECTIONS=Arrays.asList(new Node(0,1),new Node(0,-1),new Node(1,0),new Node(-1,0));
    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME);             // Our hero

        Emitter.Listener onMapUpdate = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    System.out.println("Start a try");
                    GameMap gameMap = hero.getGameMap(); // map
                    gameMap.updateOnUpdateMap(args[0]);

                    Player Me = gameMap.getCurrentPlayer(); // Me
                    Node currentNode = new Node(Me.getX(), Me.getY());

                    List<Player> OtherPlayer = gameMap.getOtherPlayerInfo(); // Other Players
                    List<Node> OtherPlayerNodes = new ArrayList<>(); // OtherPlayerNodes
                    for (Player p : OtherPlayer) {
                        if(p.getIsAlive()){
                            OtherPlayerNodes.add(new Node(p.getX(), p.getY()));
                        }
                    }
                    List<Weapon> ListGun = gameMap.getAllGun();
                    //List<Weapon> ListMelee = gameMap.getAllMelee();
                    //List<Weapon> ListThrowable = gameMap.getAllThrowable();

                    List<Obstacle> Blocks = gameMap.getListIndestructibleObstacles(); // Blocks(Wall+Chest+Trap)
                    Blocks.addAll(gameMap.getListChests());
                    Blocks.addAll(gameMap.getListTraps());
                    //Blocks.addAll(gameMap.getListIndestructibleObstacles());

                    List<Enemy> ListEnemies = gameMap.getListEnemies(); // List of Enemies

                    List<Node> BlocksNodes = new ArrayList<>(); // BlocksNodes
                    for(Enemy E : ListEnemies){
                        Node enemy=new Node(E.getX(),E.getY());
                        for(int i=-2;i<=2;++i)
                        {
                            for(int j=-2;j<=2;++j)
                            {
                                if(Math.abs(i)+Math.abs(j)<=3)
                                {
                                    BlocksNodes.add(add(new Node(i,j),enemy));
                                }
                            }
                        }
                    }
                    for (Obstacle O : Blocks) {

                        BlocksNodes.add(new Node(O.getX(), O.getY()));
                    }
                    Weapon HaveGun = hero.getInventory().getGun(); // Gun
                    boolean pickedUpGun = HaveGun != null;
                    System.out.println("Have gun?: " + pickedUpGun);

//                    Weapon MeleeName = hero.getInventory().getMelee(); // Melee
//                    boolean pickedUpMelee = !MeleeName.getId().equals("HAND");
//
//                    System.out.println("Have melee?: " + pickedUpMelee);
//
//                    List<Weapon> HaveThrowable = hero.getInventory().getListThrowable(); // Throwable
//                    boolean pickedUpThrowable = !HaveThrowable.isEmpty();
//                    System.out.println("Have throwable?: " + pickedUpThrowable);



                    if (!pickedUpGun ) {
                        List<Weapon> weaponsList = new ArrayList<>();
                        weaponsList.addAll(ListGun);
                        //weaponsList.addAll(ListMelee);
                        //weaponsList.addAll(ListThrowable);
                        Weapon nearestWeapon = weaponsList.getFirst();
                        for(Weapon weapon: weaponsList){
                            if(dis_weapon(currentNode,weapon)< dis_weapon(currentNode,nearestWeapon))
                            {
                                nearestWeapon=weapon;
                            }

                        }

                        System.out.println("Weapon: "+nearestWeapon.getX()+" "+nearestWeapon.getY());

                        if (currentNode.getX() == nearestWeapon.getX() && currentNode.getY() == nearestWeapon.getY()) {
                            hero.pickupItem();
                            System.out.println("Picked up Weapon");
                            //pickedUpGun = true;
                        } else {
                            for(Player P : OtherPlayer) {

                                if(P.getBulletNum()>0){

                                    for(int indexDiretions=0;indexDiretions<4;++indexDiretions){
                                        // Gun speed=3
                                        Node nearOthers=add(P,DIRECTIONS.get(indexDiretions));

                                        OtherPlayerNodes.add(nearOthers);

                                        nearOthers=add(nearOthers,DIRECTIONS.get(indexDiretions));

                                        OtherPlayerNodes.add(nearOthers);

                                        nearOthers=add(nearOthers,DIRECTIONS.get(indexDiretions));

                                        OtherPlayerNodes.add(nearOthers);

                                    }



                                }

                            }
                            BlocksNodes.addAll(OtherPlayerNodes); // add all other players
                            System.out.println("Move to nearest Weapon");
                            hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestWeapon, false));
                        }
                    }
                    else {

                        Weapon curGun = hero.getInventory().getGun();
                        boolean fire_or_move=false;
                        int cnt=0;
                        for(Node P : OtherPlayerNodes) {
                            cnt++;

                            System.out.println("[Player"+cnt+": "+P.getX() + " " + P.getY()+"]");

                            if (distance(P, currentNode) <= curGun.getRange()) {
                                if (P.getX() == currentNode.getX()) {
                                    if (P.getY() < currentNode.getY()) {
                                        hero.shoot("d");
                                    } else {
                                        hero.shoot("u");
                                    }
                                    fire_or_move = true;

                                } else if (P.getY() == currentNode.getY()) {
                                    if (P.getX() < currentNode.getX()) {
                                        hero.shoot("l");
                                    } else {
                                        hero.shoot("r");
                                    }
                                    fire_or_move = true;

                                }
                            }
                        }

                        if (fire_or_move) {
                            System.out.println("Fire");
                        }
                        else {
                            System.out.println("Move");
                        }

                        if(!fire_or_move) { // move
                            Node nearestPlayerNode = OtherPlayerNodes.getFirst();
                            int min_dis_to_player = 20052024;
                            for (Node P : OtherPlayerNodes) {
                                int dis_to_player_x = distance(P, currentNode);
                                if (dis_to_player_x < min_dis_to_player) {
                                    min_dis_to_player = dis_to_player_x;
                                    nearestPlayerNode = P;
                                }
                            }
                            System.out.println("[Current Hero]:"+currentNode.getX()+" "+currentNode.getY()+"] ");
                            System.out.println("[Nearest player:"+min_dis_to_player+"] ");
                            System.out.println("Players: "+nearestPlayerNode.getX() + " " + nearestPlayerNode.getY());

                            for(Node P: OtherPlayerNodes) {
                                // add all other players except the nearest one
                                if(P.getX() != nearestPlayerNode.getX() || P.getY() != nearestPlayerNode.getY()){
                                    BlocksNodes.add(P);
                                    System.out.println("[Add to Block:"+": "+P.getX() + " " + P.getY()+"]");
                                }
                            }
                            gameMap = hero.getGameMap();
                            System.out.println("[Path]: "+PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
                            hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
                        }


                    }


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("===================");
            }
        };

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}
