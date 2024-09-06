package gamecodefest2024;
import io.socket.emitter.Emitter;
import jsclub.codefest2024.sdk.*;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;
import jsclub.codefest2024.sdk.model.enemies.Enemy;
import jsclub.codefest2024.sdk.model.equipments.HealingItem;
import jsclub.codefest2024.sdk.model.obstacles.Obstacle;
import jsclub.codefest2024.sdk.model.players.Player;
import jsclub.codefest2024.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.*;

public class Main {
    private static final String SERVER_URL = "https://cf-server.jsclub.dev";
    private static final String GAME_ID = "120568";
    private static final String PLAYER_NAME = "TuanVM";

    private static final int CHESTGUN=30;
    private static final int CHESTPLAYER=35;
    private static final int ENEMY=3;
    private static final int MAX=123456;

    private static int distance(Node x, Node y) {
        return Math.abs(x.x - y.x) + Math.abs(x.y - y.y);}

    private static int dis_weapon(Node x, Weapon y) {
        return (Math.abs(x.x - y.x) + Math.abs(x.y - y.y))*360/y.getDamage();
    } // ưu tiên 1 chút với súng có dame to hơn

    private static Node add(Node x, Node y) {

        return new Node(x.x + y.x, x.y + y.y);
    } // (node x + node y)

    public static boolean checkInsideSafeArea(Node x, int darkAreaSize, int mapSize) {
        return x.x >= darkAreaSize && x.y >= darkAreaSize && x.x < mapSize - darkAreaSize && x.y < mapSize - darkAreaSize;
    }

    private static Obstacle findNearestChest(Node currentNode, List<Obstacle> ListChest) {
        Obstacle nearestChest = null;
        int minDistance = MAX;

        for (Obstacle chest : ListChest) {
            int distanceToChest = distance(currentNode, chest);
            if (distanceToChest < minDistance) {
                minDistance = distanceToChest;
                nearestChest = chest;
            }
        }

        return nearestChest;
    }// find nearest chest

    private static void usingHealingItem(Hero hero, GameMap gameMap) throws IOException {
        Player Me=gameMap.getCurrentPlayer();
        if(hero.getInventory().getListHealingItem().isEmpty()) return;
        if(!checkInsideSafeArea(new Node(Me.getX(),Me.getY()), gameMap.getDarkAreaSize(), gameMap.getMapSize())) return;
        List<HealingItem> ListHealItem = hero.getInventory().getListHealingItem();

        HealingItem bestHeal= null;
        int maxHP=0;

        if(Me.getHp()<40){
            for(HealingItem healingItem : ListHealItem){
                if(healingItem.getHealingHP()>maxHP){
                    bestHeal=healingItem;
                    maxHP=bestHeal.getHealingHP();
                }
            }
        }
        else if(Me.getHp()<60)
        {
            for(HealingItem healingItem : ListHealItem){
                if(healingItem.getHealingHP()>maxHP && healingItem.getHealingHP()<=50){
                    bestHeal=healingItem;
                    maxHP=bestHeal.getHealingHP();
                }
            }
        }
        else if(Me.getHp()<=90)
        {
            for(HealingItem healingItem : ListHealItem){
                if(healingItem.getHealingHP()>maxHP && healingItem.getHealingHP()<=20){
                    bestHeal=healingItem;
                    maxHP=bestHeal.getHealingHP();
                }
            }
        }
        if(bestHeal!=null){
            hero.useItem(bestHeal.getId());
        }

    }

    // main tactic
    private static int try_gun_and_chest( Node currentNode, Hero hero,List<Node> OtherPlayerNodes, List<Node> BlocksNodes,
                                           GameMap gameMap, boolean is_enemy_coming) throws IOException {
        Player Me=gameMap.getCurrentPlayer();
        List<Weapon> AllListGun=gameMap.getAllGun();
        List<Weapon> ListGun= new ArrayList<>();
        List<Obstacle> CHESTLIST=gameMap.getListChests();
        List<Node> CHESTLISTNODE=new ArrayList<>();
        for(Obstacle P:CHESTLIST){
            CHESTLISTNODE.add(new Node(P.getX(),P.getY()));
        }
        for(Weapon weapon:AllListGun) if(checkInsideSafeArea(weapon, gameMap.getDarkAreaSize(), gameMap.getMapSize())){
            ListGun.add(weapon);
        }
        Weapon HaveGun = hero.getInventory().getGun(); // Gun
        boolean pickedUpGun = HaveGun != null;
        System.out.println("Have gun?: " + pickedUpGun);

        if (!pickedUpGun) {

            HealingItem nearestHealingItem = findNearestHealingItem(currentNode,gameMap.getListHealingItems());
            if(nearestHealingItem!=null&&hero.getInventory().getListHealingItem().size()<4) {
                if (currentNode.getX() == nearestHealingItem.getX() && currentNode.getY() == nearestHealingItem.getY()) {
                    System.out.println("Picked up HealingItem");
                    hero.pickupItem();
                }
                else {
                    System.out.println("Move to healing item: " + nearestHealingItem);
                    BlocksNodes.addAll(CHESTLISTNODE);
                    hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestHealingItem, false));
                }
                return 1;
            } // no gun, move to healing item and pick up if (distance <= 3)
            List<Weapon> weaponsList = new ArrayList<>(ListGun);

            Weapon nearestWeapon = null;

            for (Weapon weapon : weaponsList) if(PathUtils.getShortestPath(gameMap,BlocksNodes,currentNode,weapon,false)!=null){
                if(nearestWeapon==null) {
                    nearestWeapon=weapon;
                }
                else if (dis_weapon(currentNode, weapon) < dis_weapon(currentNode, nearestWeapon)) {
                    nearestWeapon = weapon;
                }
            } // find gun
            if (nearestWeapon!=null&& currentNode.getX() == nearestWeapon.getX() && currentNode.getY() == nearestWeapon.getY()) {

                System.out.println("Picked up Gun");
                hero.pickupItem();
            } // pick up gun

            // Tìm rương gần nhất
            List<Obstacle> ChestList=gameMap.getListChests();
            List<Obstacle> InsideChestList = new ArrayList<>();
            for(Obstacle Chest : ChestList) {
                if(checkInsideSafeArea(Chest, gameMap.getDarkAreaSize(), gameMap.getMapSize())){
                    InsideChestList.add(Chest);
                }
            }
            Obstacle nearestChest = null;
            if(!InsideChestList.isEmpty()){
                nearestChest=findNearestChest(currentNode,InsideChestList);
            }
            int distToChest = MAX;
            if(nearestChest!=null) {
                distToChest=distance(currentNode, nearestChest);  // Khoảng cách đến rương
            }

            int distToGun;
            if(nearestWeapon==null) distToGun=MAX;
            else distToGun= distance(currentNode, nearestWeapon);  // Khoảng cách đến súng
            System.out.println("[Size of InsideChestList]:"+InsideChestList.size());
            System.out.println("[Chest: "+nearestChest+"]");
            // Quyết định dựa trên khoảng cách đến rương và súng
            if (nearestChest!=null &&( (Me.getHp()<=50) ||(distToChest + CHESTGUN <= distToGun)) ) {
                boolean is_remove=false;
                int cnt=0;
                for(Node p:BlocksNodes) {
                    if(p.x==nearestChest.getX() && p.y==nearestChest.getY()) {
                        BlocksNodes.remove(cnt);
                        System.out.println("Removed BlocksNode: "+p);
                        is_remove=true;
                        break;
                    }
                    cnt++;
                }
                System.out.println("Chest removed?:"+is_remove );

                // Di chuyển đến vị trí liền kề với rương
                if (distToChest > 1) {
                    System.out.println("Moving to nearest chest...");
                    for(Node p:CHESTLISTNODE) {
                        if(p.getX()!=nearestChest.getX() && p.getY()!=nearestChest.getY()) {
                            BlocksNodes.add(new Node(p.getX(), p.getY()));
                        }
                    }
                    System.out.println("[Path to chest] "+PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, false));
                    hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, false));  // Di chuyển đến vị trí liền kề rương
                } else {
                    System.out.println("Breaking chest...");

                    // Xác định hướng tấn công rương
                    String attackDirection = determineAttackDirection(currentNode, nearestChest);
                    hero.attack(attackDirection);  // Tấn công rương theo hướng xác định
                }
            } // move to chest and break
            else {
                System.out.println("Move to nearest Gun");
                BlocksNodes.addAll(CHESTLISTNODE);
                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestWeapon, false));
            } //move to gun
        } // no gun, find near healing item or chest or gun (not find player)
        else {
            // Đã có súng
            Weapon curGun = hero.getInventory().getGun();
            boolean fire_or_move = false;

            if (!is_enemy_coming) {
                int cnt = 0;
                for (Node P : OtherPlayerNodes) {
                    cnt++;
                    System.out.println("[Player" + cnt + ": " + P.getX() + " " + P.getY() + "]");

                    if (distance(P, currentNode) <= curGun.getRange()) { //curGun.getRange()
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
            } // fire if we can

            if (fire_or_move) {
                System.out.println("Fired");
            } else {
                System.out.println("Move to player");

                HealingItem nearestHealingItem = findNearestHealingItem(currentNode,gameMap.getListHealingItems());

                if(nearestHealingItem!=null&&hero.getInventory().getListHealingItem().size()<4) {
                    if (currentNode.getX() == nearestHealingItem.getX() && currentNode.getY() == nearestHealingItem.getY()) {

                        System.out.println("Picked up HealingItem");
                        hero.pickupItem();
                        return 1;
                    }
                    System.out.println("Move to healing item: " + nearestHealingItem);
                    BlocksNodes.addAll(CHESTLISTNODE);
                    hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestHealingItem, false));
                    return 1;
                } // have gun, move to healing item and pick up if near

                // Move to the nearest player if not shooting
                Node nearestPlayerNode = OtherPlayerNodes.getFirst();
                int minDisToPlayer = Integer.MAX_VALUE;

                for (Node P : OtherPlayerNodes) {
                    int disToPlayer = distance(P, currentNode);
                    if (disToPlayer < minDisToPlayer) {
                        minDisToPlayer = disToPlayer;
                        nearestPlayerNode = P;
                    }
                } // find nearest player
                if(minDisToPlayer>10&&!is_enemy_coming) usingHealingItem( hero,  gameMap);

                List<Obstacle> ChestList=gameMap.getListChests();
                List<Obstacle> InsideChestList = new ArrayList<>();
                for(Obstacle Chest : ChestList) {
                    if(checkInsideSafeArea(Chest, gameMap.getDarkAreaSize(), gameMap.getMapSize())){
                        InsideChestList.add(Chest);
                    }
                }
                Obstacle nearestChest = null;
                if(!InsideChestList.isEmpty()){
                    nearestChest=findNearestChest(currentNode,InsideChestList);
                }
                int distToChest = MAX;
                if(nearestChest!=null){
                    distToChest=distance(currentNode, nearestChest);  // Khoảng cách đến rương
                }
                System.out.println("[Size of InsideChestList]:"+InsideChestList.size());

                System.out.println("[Chest: "+nearestChest+"]");

                int distToPlayer = distance(currentNode, nearestPlayerNode);
                if (nearestChest!=null &&( (Me.getHp()<=50) ||(distToChest + CHESTGUN <= distToPlayer)) ) {

                    int cnt=0;
                    boolean is_remove=false;
                    for(Node p:BlocksNodes) {
                        if(p.x==nearestChest.getX() && p.y==nearestChest.getY()) {
                            BlocksNodes.remove(cnt);
                            System.out.println("Removed BlocksNode: "+p);
                            is_remove=true;
                            break;
                        }
                        cnt++;
                    }
                    System.out.println("Chest removed?:"+is_remove );                    // Di chuyển đến vị trí liền kề với rương
                    if (distToChest > 1) {
                        System.out.println("Moving to nearest chest...");
                        for(Node p:CHESTLISTNODE) {
                            if(p.getX()!=nearestChest.getX() && p.getY()!=nearestChest.getY()) {
                                BlocksNodes.add(new Node(p.getX(), p.getY()));
                            }
                        }
                        BlocksNodes.remove( new Node(nearestChest.getX(),nearestChest.getY())); // remove nearest chest from block node to find path to it

                        System.out.println("[Path to chest] "+PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, false));
                        hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, false));  // Di chuyển đến vị trí liền kề rương
                    } else {
                        System.out.println("Breaking chest...");

                        // Xác định hướng tấn công rương
                        String attackDirection = determineAttackDirection(currentNode, nearestChest);
                        hero.attack(attackDirection);  // Tấn công rương theo hướng xác định
                    }
                    return 1;
                }

                //System.out.println("[Current Hero]:" + currentNode.getX() + " " + currentNode.getY() + "] ");
                System.out.println("[Nearest player:" + minDisToPlayer + "] ");
                System.out.println("[Players: " + nearestPlayerNode.getX() + " " + nearestPlayerNode.getY()+"]");


                BlocksNodes.remove(nearestPlayerNode);// xoá thằng này khỏi block để tìm đường đi
                BlocksNodes.addAll(CHESTLISTNODE);
                System.out.println("[Path]: " + PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
                return 1;
            }

        } // have gun, find near healing item or near player (not find chest)
        return 0;
    }

    private static String determineAttackDirection(Node currentNode, Obstacle nearestChest) {
        if (nearestChest.getY() < currentNode.getY()) {
            return "d";  // Rương nằm dưới
        } else if (nearestChest.getY() > currentNode.getY()) {
            return "u";  // Rương nằm trên
        } else if (nearestChest.getX() < currentNode.getX()) {
            return "l";  // Rương nằm bên trái
        } else {
            return "r";  // Rương nằm bên phải
        }
    } // hướng để phá rương
    private static HealingItem findNearestHealingItem(Node currentNode, List<HealingItem> healthItems) {
        HealingItem nearestHealingItem = null;
        int minDistance = MAX;
        for (HealingItem item : healthItems) {
            int distanceToItem = distance(currentNode, item);
            if (distanceToItem <= 3 && distanceToItem < minDistance) {
                minDistance = distanceToItem;
                nearestHealingItem = item;
            }
        }

        return nearestHealingItem;
    }


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

                    List<Player> OtherPlayerList = gameMap.getOtherPlayerInfo(); // Other Players List
                    List<Node> OtherPlayerNodes = new ArrayList<>(); // OtherPlayerNodes

                    for (Player p : OtherPlayerList) {
                        if(p.getIsAlive()){
                            OtherPlayerNodes.add(new Node(p.getX(), p.getY()));
                        }
                    } //  create OtherPlayerNodes by alive players

                    List<Obstacle> Blocks = gameMap.getListIndestructibleObstacles(); // Blocks(Wall+Chest+Trap)
//                    Blocks.addAll(gameMap.getListChests());
                    Blocks.addAll(gameMap.getListTraps());

                    List<Enemy> ListEnemies = gameMap.getListEnemies(); // List of Enemies

                    List<Node> BlocksNodes = new ArrayList<>(); // BlocksNodes
//                    for(int i=1;i<= gameMap.getMapSize();i++)
//                        for(int j=1;j<=gameMap.getMapSize();j++)
//                        {
//                            if(!checkInsideSafeArea(new Node(i,j), gameMap.getDarkAreaSize(),gameMap.getMapSize())){
//                                BlocksNodes.add(new Node(i,j));
//                            }
//                        }
                    boolean is_enemy_coming=false;
                    for(Enemy E : ListEnemies){ //cách enemies ít nhất 4 đơn vị
                        Node enemy=new Node(E.getX(),E.getY());
                        for(int i=-ENEMY;i<=ENEMY;++i)
                        {
                            for(int j=-ENEMY;j<=ENEMY;++j)
                                if(Math.abs(i)+Math.abs(j)<=ENEMY)
                                {
                                    if(currentNode.x-i==E.x && currentNode.y-j==E.y){
                                        is_enemy_coming=true;
                                    }
                                    BlocksNodes.add(add(new Node(i,j),enemy));
                                }
                        }
                    } // add block around enemies and check if they are coming
                    BlocksNodes.addAll(OtherPlayerNodes);

                    for (Obstacle O : Blocks) {

                        BlocksNodes.add(new Node(O.getX(), O.getY()));
                    } // creat Blocknodes


//                    Weapon MeleeName = hero.getInventory().getMelee(); // Melee
//                    boolean pickedUpMelee = !MeleeName.getId().equals("HAND");
//                    System.out.println("Have melee?: " + pickedUpMelee);
//
//                    List<Weapon> HaveThrowable = hero.getInventory().getListThrowable(); // Throwable
//                    boolean pickedUpThrowable = !HaveThrowable.isEmpty();
//                    System.out.println("Have throwable?: " + pickedUpThrowable);


                    System.out.println("Return: "+try_gun_and_chest( currentNode, hero,OtherPlayerNodes,BlocksNodes, gameMap, is_enemy_coming));


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
