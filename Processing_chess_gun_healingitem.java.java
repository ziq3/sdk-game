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
    private static final String GAME_ID = "152167";
    private static final String PLAYER_NAME = "Nguoi Lua Gian Doi";

    private static int distance(Node x, Node y) {
        return Math.abs(x.x - y.x) + Math.abs(x.y - y.y);}

    private static int dis_weapon(Node x, Weapon y) {
        return (Math.abs(x.x - y.x) + Math.abs(x.y - y.y))*360/y.getDamage();
    } // ưu tiên 1 chút với súng có dame to hơn

    private static Node add(Node x, Node y) {

        return new Node(x.x + y.x, x.y + y.y);
    } // (node x + node y)

    private static final List<Node> DIRECTIONS=Arrays.asList(new Node(0,1),new Node(0,-1),new Node(1,0),new Node(-1,0));

    private static Obstacle findNearestChest(Node currentNode, List<Obstacle> ListChest) {
        Obstacle nearestChest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Obstacle chest : ListChest) {
            int distanceToChest = distance(currentNode, chest);
            if (distanceToChest < minDistance) {
                minDistance = distanceToChest;
                nearestChest = chest;
            }
        }

        return nearestChest;
    }// find nearest chest

    // main tactic
    private static void try_gun_and_chest( Node currentNode, Hero hero,List<Node> OtherPlayerNodes, List<Node> BlocksNodes,
                                           GameMap gameMap, boolean is_enemy_coming) throws IOException {

        List<Weapon> ListGun=gameMap.getAllGun();
        Weapon HaveGun = hero.getInventory().getGun(); // Gun
        boolean pickedUpGun = HaveGun != null;
        System.out.println("Have gun?: " + pickedUpGun);

        if (!pickedUpGun) {
            // Không có súng
            HealingItem nearestHealingItem = findNearestHealingItem(currentNode,gameMap.getListHealingItems());
            if(nearestHealingItem!=null&&hero.getInventory().getListHealingItem().size()<4) {
                if (currentNode.getX() == nearestHealingItem.getX() && currentNode.getY() == nearestHealingItem.getY()) {
                    hero.pickupItem();
                    System.out.println("Picked up HealingItem");
                    return;
                }
                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestHealingItem, false));
                System.out.println("Move to healing item: " + nearestHealingItem);
                return;
            } // no gun, move to healing item and pick up if near
            List<Weapon> weaponsList = new ArrayList<>(ListGun);
            Weapon nearestWeapon = weaponsList.getFirst();

            for (Weapon weapon : weaponsList) {
                if (dis_weapon(currentNode, weapon) < dis_weapon(currentNode, nearestWeapon)) {
                    nearestWeapon = weapon;
                }
            } // find gun
            if (currentNode.getX() == nearestWeapon.getX() && currentNode.getY() == nearestWeapon.getY()) {
                hero.pickupItem();
                System.out.println("Picked up Gun");
                return;
            } // pick up gun

            // Tìm rương gần nhất
            Obstacle nearestChest = findNearestChest(currentNode,gameMap.getListChests());
            int distToChest = distance(currentNode, nearestChest);  // Khoảng cách đến rương
            int distToGun = distance(currentNode, nearestWeapon);  // Khoảng cách đến súng

            // Quyết định dựa trên khoảng cách đến rương và súng
            if (distToChest > distToGun) {
                System.out.println("Move to nearest Gun");
                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestWeapon, false));
            } //move to gun
            else if (distToChest + 20 <= distToGun) {

                // Di chuyển đến vị trí liền kề với rương
                if (distToChest > 1) {
                    System.out.println("Moving to nearest chest...");

                    BlocksNodes.remove( new Node(nearestChest.getX(),nearestChest.getY())); // remove nearest chest from block node to find path to it

                    System.out.println("[Path to chest] "+PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, true));
                    hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, true));  // Di chuyển đến vị trí liền kề rương
                } else {
                    System.out.println("Breaking chest...");

                    // Xác định hướng tấn công rương
                    String attackDirection = determineAttackDirection(currentNode, nearestChest);
                    hero.attack(attackDirection);  // Tấn công rương theo hướng xác định
                }
            } // move to chest and break
            else {
                System.out.println("Move to nearest Gun");
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
            } // fire if we can

            if (fire_or_move) {
                System.out.println("Fired");
            } else {
                System.out.println("Move to player");

                HealingItem nearestHealingItem = findNearestHealingItem(currentNode,gameMap.getListHealingItems());

                if(nearestHealingItem!=null&&hero.getInventory().getListHealingItem().size()<4) {
                    if (currentNode.getX() == nearestHealingItem.getX() && currentNode.getY() == nearestHealingItem.getY()) {
                        hero.pickupItem();
                        System.out.println("Picked up HealingItem");
                        return;
                    }
                    hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestHealingItem, false));
                    System.out.println("Move to healing item: " + nearestHealingItem);
                    return;
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
                Obstacle nearestChest = findNearestChest(currentNode,gameMap.getListChests());
                int distToChest = distance(currentNode, nearestChest);  // Khoảng cách đến rương
                if (distToChest + 20 <= distToGun) {// đang làm dở thay dist_to_gun thành dis_to_player để trên đường đến thằng gần nhất thì quay xe phá rương cho gần 

                    // Di chuyển đến vị trí liền kề với rương
                    if (distToChest > 1) {
                        System.out.println("Moving to nearest chest...");

                        BlocksNodes.remove( new Node(nearestChest.getX(),nearestChest.getY())); // remove nearest chest from block node to find path to it

                        System.out.println("[Path to chest] "+PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, true));
                        hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, true));  // Di chuyển đến vị trí liền kề rương
                    } else {
                        System.out.println("Breaking chest...");

                        // Xác định hướng tấn công rương
                        String attackDirection = determineAttackDirection(currentNode, nearestChest);
                        hero.attack(attackDirection);  // Tấn công rương theo hướng xác định
                    }
                }

                //System.out.println("[Current Hero]:" + currentNode.getX() + " " + currentNode.getY() + "] ");
                System.out.println("[Nearest player:" + minDisToPlayer + "] ");
                System.out.println("Players: " + nearestPlayerNode.getX() + " " + nearestPlayerNode.getY());

                for (Node P : OtherPlayerNodes) {
                    if (P.getX() != nearestPlayerNode.getX() || P.getY() != nearestPlayerNode.getY()) {
                        BlocksNodes.add(P);
                        System.out.println("[Add to Block:" + ": " + P.getX() + " " + P.getY() + "]");
                    }
                }  // Add all other players except the nearest one

                System.out.println("[Path]: " + PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
            }

        } // have gun, find near healing item or near player (not find chest)
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
        int minDistance = Integer.MAX_VALUE;

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
                    Blocks.addAll(gameMap.getListChests());
                    Blocks.addAll(gameMap.getListTraps());

                    List<Enemy> ListEnemies = gameMap.getListEnemies(); // List of Enemies

                    List<Node> BlocksNodes = new ArrayList<>(); // BlocksNodes

                    boolean is_enemy_coming=false;
                    for(Enemy E : ListEnemies){ //cách enemies ít nhất 4 đơn vị
                        Node enemy=new Node(E.getX(),E.getY());
                        for(int i=-2;i<=2;++i)
                        {
                            for(int j=-2;j<=2;++j)
                            {
                                if(Math.abs(i)+Math.abs(j)<=3)
                                {
                                    if(currentNode.x-i==E.x && currentNode.y-j==E.y){
                                        is_enemy_coming=true;
                                    }
                                    BlocksNodes.add(add(new Node(i,j),enemy));
                                }
                            }
                        }
                    } // add blocknodes around enemies and check if they are coming

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



                    try_gun_and_chest( currentNode, hero,OtherPlayerNodes,BlocksNodes, gameMap, is_enemy_coming);


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
