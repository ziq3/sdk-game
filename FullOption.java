package gamecodefest2024;
import io.socket.emitter.Emitter;
import jsclub.codefest2024.sdk.*;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;
import jsclub.codefest2024.sdk.model.enemies.Enemy;
import jsclub.codefest2024.sdk.model.equipments.Armor;
import jsclub.codefest2024.sdk.model.equipments.HealingItem;
import jsclub.codefest2024.sdk.model.obstacles.Obstacle;
import jsclub.codefest2024.sdk.model.players.Player;
import jsclub.codefest2024.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.*;

public class Main {
    private static final String SERVER_URL = "https://cf-server.jsclub.dev";
    private static final String GAME_ID = "198892";
    private static final String PLAYER_NAME = "ThuongChoTamThanCoHan,NgamNguiLangNhinConDoSangNgang";


    private static final List<Node> DIRECTION = Arrays.asList(new Node(0, 1), new Node(0, -1), new Node(1, 0),
            new Node(-1, 0));
    private static final int CHESTGUN=20;
    private static final int CHESTPLAYER=25;
    private static final int ENEMY=2;
    private static final int MAX=123456;

    private static int lengthPath(Node x, Node y,List<Node> BlockNodes,GameMap gameMap) {

        String path= PathUtils.getShortestPath(gameMap,BlockNodes,x,y,false);
        if(path==null) {
            int minDistance=MAX;
            for(Node dir : DIRECTION) {
                if(!BlockNodes.contains(add(dir,y))) {
                    path=PathUtils.getShortestPath(gameMap,BlockNodes,x,add(dir,y),false);
                    if(path!=null) {
                        minDistance=Math.min(minDistance,path.length());
                    }
                }
            }
            if(minDistance==MAX) {
                return distance(x,y);
            }
            else {
                return minDistance;
            }
        }
        else {
            return path.length();
        }
    }//số bước để tới đích

    private static int distance(Node x, Node y) {
        return Math.abs(x.x - y.x) + Math.abs(x.y - y.y);} // khoảng cách mahatan

    private static int dis_weapon(Node x, Weapon y) {
        return (Math.abs(x.x - y.x) + Math.abs(x.y - y.y))*360/y.getDamage();
    } // ưu tiên 1 chút với súng có dame to hơn

    private static Node add(Node x, Node y) {

        return new Node(x.x + y.x, x.y + y.y);
    } // (node x + node y)

    public static boolean checkInsideSafeArea(Node x, int darkAreaSize, int mapSize) {
        //darkAreaSize++;
        return x.x >= darkAreaSize && x.y >= darkAreaSize && x.x < mapSize - darkAreaSize && x.y < mapSize - darkAreaSize;
    }

    private static Obstacle findNearestChest(Node currentNode, List<Obstacle> ListChest,List<Node> BlockNodes,GameMap gameMap) {
        Obstacle nearestChest = null;
        int minDistance = MAX;

        for (Obstacle chest : ListChest) {
            int distanceToChest = lengthPath(currentNode, chest,BlockNodes,gameMap);
            if (distanceToChest < minDistance) {
                minDistance = distanceToChest;
                nearestChest = chest;
            }
        }

        return nearestChest;
    }// find nearest chest

    private static int usingHealingItem(Hero hero, GameMap gameMap) throws IOException {
        Player Me=gameMap.getCurrentPlayer();
        if(hero.getInventory().getListHealingItem().isEmpty()) return 0;
        if(!checkInsideSafeArea(new Node(Me.getX(),Me.getY()), gameMap.getDarkAreaSize(), gameMap.getMapSize())) return 0;
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
            return 1;
        }
        return 0;

    }// hồi máu hợp lí

    // main tactic
    private static int try_gun_and_chest( Node currentNode, Hero hero,List<Node> OtherPlayerNodes, List<Node> BlocksNodes,
                                           GameMap gameMap, boolean is_enemy_coming) throws IOException {

        if(is_enemy_coming) return 0;
        Node nearestPlayerNode = OtherPlayerNodes.getFirst();
        int minDisToPlayer = MAX;

        for (Node P : OtherPlayerNodes) {
            int disToPlayer = lengthPath(P, currentNode,BlocksNodes,gameMap);
            if (disToPlayer < minDisToPlayer) {
                minDisToPlayer = disToPlayer;
                nearestPlayerNode = P;
            }
        } // find nearest player
        if(checkInsideSafeArea(currentNode, gameMap.getDarkAreaSize(), gameMap.getMapSize()) &&
            minDisToPlayer>10 && usingHealingItem(hero,gameMap)==1) {
            System.out.println("Healing!!!!!");
            return 1;
        }
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

        Weapon curMelee = hero.getInventory().getMelee(); // Melee
        boolean pickedBestMelee = curMelee.getId().equals("LIGHT_SABER");
        System.out.println("Current melee?: " + curMelee.getId());

        int  numberHealItem=hero.getInventory().getListHealingItem().size();
        boolean isFullHeal=(numberHealItem==4);
        System.out.println("Have 4 heal item?: " + isFullHeal);

        List<Armor> MyarmorList=hero.getInventory().getListArmor();
        int numberArmor=MyarmorList.size();
        boolean isFullArmor=(numberArmor==2);
        System.out.println("Have 2 armor?: " + isFullArmor);

        List<Weapon> meleeList = gameMap.getAllMelee();


        // try to pick heal,armor and melee if (distance<=3)
        Weapon nearestMelee = findNearestMelee(currentNode,BlocksNodes,gameMap,meleeList);
        if(nearestMelee!=null && curMelee.getDamage()<nearestMelee.getDamage()) {
            if (currentNode.getX() == nearestMelee.getX() && currentNode.getY() == nearestMelee.getY()) {
                if(curMelee.getId().equals("HAND")) {
                    System.out.println("Picked up Melee");
                    hero.pickupItem();

                }
                else{
                    hero.revokeItem(curMelee.getId());
                }
                return 1;
            }
            System.out.println("Move to melee : " + nearestMelee);
            BlocksNodes.addAll(CHESTLISTNODE);
            hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestMelee, false));
            return 1;
        }  //move to melee item and pick up if near

        HealingItem nearestHealingItem = findNearestHealingItem(currentNode,BlocksNodes,gameMap,gameMap.getListHealingItems());
        if(nearestHealingItem!=null&&!isFullHeal) {
            if (currentNode.getX() == nearestHealingItem.getX() && currentNode.getY() == nearestHealingItem.getY()) {

                System.out.println("Picked up HealingItem");
                hero.pickupItem();
                return 1;
            }
            System.out.println("Move to healing item: " + nearestHealingItem);
            BlocksNodes.addAll(CHESTLISTNODE);
            hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestHealingItem, false));
            return 1;
        }  //move to healing item and pick up if near

        Armor nearestArmor = findNearestArmor(currentNode,BlocksNodes,gameMap,gameMap.getListArmors());
        if(nearestArmor!=null && !isFullArmor ) {

            if (currentNode.getX() == nearestArmor.getX() && currentNode.getY() == nearestArmor.getY()) {

                System.out.println("Picked up Armor");
                hero.pickupItem();
            }
            else {
                System.out.println("Move to armor item: " + nearestArmor);
                BlocksNodes.addAll(CHESTLISTNODE);
                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestArmor, false));
            }
            return 1;
        }  //move to armor item and pick up if near

        boolean isFullSet=(isFullArmor&&isFullHeal&&pickedBestMelee);

        if( minDisToPlayer<=3 && distance(currentNode,nearestPlayerNode)<=3){
            if( (Me.getHp()>=50 && pickedBestMelee) ||
                ( Me.getHp()>=60 && !curMelee.getId().equals("HAND") ) && minDisToPlayer<=2 && distance(currentNode,nearestPlayerNode)<=2 ){
                if(distance(currentNode,nearestPlayerNode)==1){
                    System.out.println("Attack player");
                    String attackDirection = determineAttackDirection(currentNode, nearestPlayerNode);
                    hero.attack(attackDirection);  // Tấn công player theo hướng xác định
                }
                else{
                    System.out.println("Go to the next of player");
                    hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
                }
                return 1;
            }
        }// chém khi có vũ khí xịn và gần địch

        // pick gun or find other players
        if (!pickedUpGun) {

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
                return 1;
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
                nearestChest=findNearestChest(currentNode,InsideChestList,BlocksNodes,gameMap);
            }
            int distToChest = MAX;
            if(nearestChest!=null) {
                distToChest=lengthPath(currentNode, nearestChest,BlocksNodes,gameMap);  // Khoảng cách đến rương
            }

            int distToGun;
            if(nearestWeapon==null) distToGun=MAX;
            else distToGun= lengthPath(currentNode, nearestWeapon,BlocksNodes,gameMap);  // Khoảng cách đến súng

            // Quyết định dựa trên khoảng cách đến rương và súng
            if (nearestChest!=null && ( (Me.getHp()<=60) ||(!isFullSet&&(distToChest + CHESTGUN <= distToGun))) ) {
                boolean is_remove=false;
                int cnt=0;
                for(Node p:BlocksNodes) {
                    if(p.x==nearestChest.getX() && p.y==nearestChest.getY()) {
                        BlocksNodes.remove(cnt);
                        is_remove=true;
                        break;
                    }
                    cnt++;
                }
                System.out.println("Chest removed?:"+is_remove );

                // Di chuyển đến vị trí liền kề với rương
                if (distance(currentNode,nearestChest) > 1) {
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

            for (Node P : OtherPlayerNodes) {

                if (lengthPath(P, currentNode,BlocksNodes,gameMap) <= curGun.getRange() && !trapInMid(currentNode, P,gameMap.getListTraps())) {
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
            }// fire if we can

            if (fire_or_move) {
                System.out.println("Fired");
            } else {

                // Move to the nearest player or chest if not shooting
                System.out.println("Move to player");

                List<Obstacle> ChestList=gameMap.getListChests();
                List<Obstacle> InsideChestList = new ArrayList<>();
                for(Obstacle Chest : ChestList) {
                    if(checkInsideSafeArea(Chest, gameMap.getDarkAreaSize(), gameMap.getMapSize())){
                        InsideChestList.add(Chest);
                    }
                }
                Obstacle nearestChest = null;
                if(!InsideChestList.isEmpty()){
                    nearestChest=findNearestChest(currentNode,InsideChestList,BlocksNodes,gameMap);
                }
                int distToChest = MAX;
                if(nearestChest!=null){
                    distToChest=lengthPath(currentNode, nearestChest,BlocksNodes,gameMap);  // Khoảng cách đến rương
                }

                System.out.println("[Chest: "+nearestChest+"]");

                int distToPlayer = lengthPath(currentNode, nearestPlayerNode,BlocksNodes,gameMap);
                if (nearestChest!=null &&( (Me.getHp()<=60) ||(!isFullSet && (distToChest + CHESTPLAYER <= distToPlayer)) )) {

                    int cnt = 0;
                    boolean is_remove = false;
                    for (Node p : BlocksNodes) {
                        if (p.x == nearestChest.getX() && p.y == nearestChest.getY()) {
                            BlocksNodes.remove(cnt);
                            System.out.println("Removed Chest: " + p);
                            is_remove = true;
                            break;
                        }
                        cnt++;
                    }
                    System.out.println("Chest removed?:" + is_remove);         // Di chuyển đến vị trí liền kề với rương
                    if (distance(currentNode, nearestChest) > 1) {
                        System.out.println("Moving to nearest chest...");
                        for (Node p : CHESTLISTNODE) {
                            if (p.getX() != nearestChest.getX() && p.getY() != nearestChest.getY()) {
                                BlocksNodes.add(new Node(p.getX(), p.getY()));
                            }
                        }
                        BlocksNodes.remove(new Node(nearestChest.getX(), nearestChest.getY())); // remove nearest chest from block node to find path to it

                        System.out.println("[Path to chest] " + PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, false));
                        hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, false));  // Di chuyển đến vị trí liền kề rương
                    } else {
                        System.out.println("Breaking chest...");

                        // Xác định hướng tấn công rương
                        String attackDirection = determineAttackDirection(currentNode, nearestChest);
                        hero.attack(attackDirection);  // Tấn công rương theo hướng xác định
                    }
                    return 1;
                }

                BlocksNodes.remove(nearestPlayerNode);// xoá thằng này khỏi block để tìm đường đi
                BlocksNodes.addAll(CHESTLISTNODE);
                System.out.println("[Path]: " + PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, false));
            }

        } // have gun, find near healing item or near player (not find chest)
        return 1;
    }

    private static boolean trapInMid(Node x, Node y, List<Obstacle> listTraps) {
        for(Obstacle mid : listTraps){
            if(distance(x,mid)+distance(mid,y)<distance(x, y)){
                return true;
            }
        }
        return false;
    }// check any trap in shoot line

    private static Weapon findNearestMelee(Node currentNode, List<Node> BlocksNodes, GameMap gameMap, List<Weapon> meleeList) {
        Weapon nearestMelee = null;
        int minDistance = MAX;
        for (Weapon item : meleeList) {
            int distanceToItem = lengthPath(currentNode, item, BlocksNodes, gameMap);
            if (distanceToItem <= 3 && distanceToItem < minDistance) {
                minDistance = distanceToItem;
                nearestMelee = item;
            }
        }
        return nearestMelee;
    }

    private static HealingItem findNearestHealingItem(Node currentNode,List<Node> BlocksNodes,GameMap gameMap, List<HealingItem> healthItems) {
        HealingItem nearestHealingItem = null;
        int minDistance = MAX;
        for (HealingItem item : healthItems) {
            int distanceToItem = lengthPath(currentNode, item,BlocksNodes,gameMap);
            if (distanceToItem <= 3 && distanceToItem < minDistance) {
                minDistance = distanceToItem;
                nearestHealingItem = item;
            }
        }
        return nearestHealingItem;
    }

    private static Armor findNearestArmor(Node currentNode, List<Node> BlocksNodes, GameMap gameMap, List<Armor> armorList) {
        Armor nearestArmor = null;
        int minDistance = MAX;
        for (Armor item : armorList) {
            int distanceToItem = lengthPath(currentNode, item, BlocksNodes, gameMap);
            if (distanceToItem <= 3 && distanceToItem < minDistance) {
                minDistance = distanceToItem;
                nearestArmor = item;
            }
        }
        return nearestArmor;
    }

    private static String determineAttackDirection(Node currentNode, Node target) {
        if (target.getY() < currentNode.getY()) {
            return "d";  // Target nằm dưới
        } else if (target.getY() > currentNode.getY()) {
            return "u";  // Target nằm trên
        } else if (target.getX() < currentNode.getX()) {
            return "l";  // Target nằm bên trái
        } else {
            return "r";  // Target nằm bên phải
        }
    } // hướng để phá rương


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
                        if(p.getIsAlive()&&checkInsideSafeArea(new Node(p.getX(), p.getY()), gameMap.getDarkAreaSize(), gameMap.getMapSize())){
                            OtherPlayerNodes.add(new Node(p.getX(), p.getY()));
                        }
                    } //  create OtherPlayerNodes by alive players

                    List<Obstacle> Blocks = gameMap.getListIndestructibleObstacles(); // Blocks(Wall+Chest+Trap)
//                    Blocks.addAll(gameMap.getListChests());
                    Blocks.addAll(gameMap.getListTraps());

                    List<Enemy> ListEnemies = gameMap.getListEnemies(); // List of Enemies

                    List<Node> BlocksNodes = new ArrayList<>(); // BlocksNodes

                    boolean is_enemy_coming=false;
                    for(Enemy E : ListEnemies){ 
                        Node enemy=new Node(E.getX(),E.getY());
                        for(int i=-ENEMY;i<=ENEMY;++i)
                        {
                            for(int j=-ENEMY;j<=ENEMY;++j)
                                if(Math.abs(i)+Math.abs(j)<=ENEMY+1)
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


                    if(is_enemy_coming){
                        List<Obstacle> CHESTLIST=gameMap.getListChests();
                        for(Obstacle O : CHESTLIST){
                            BlocksNodes.add(new Node(O.getX(), O.getY()));
                        }
                        for(Node P:OtherPlayerNodes){
                            if(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, P, false)!=null){
                                System.out.println("ENEMY IS COMING!!!!");
                                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, P, false));
                                break;
                            }
                        }
                    }
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
//Thiếu xử lí chạy vòng ra ngoài bo để chạy vào lại map sáng, cần thêm cơ chế hồi máu kể cả đang đừng ngoài bo
