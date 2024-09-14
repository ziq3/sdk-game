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
    private static final String GAME_ID = "110899";
    private static final String PLAYER_NAME = "BurnedBing";
    private static final String PLAYER_KEY = "9576b1db-2ac8-4534-80da-21feec8bba8d";
    private static final List<Node> DIRECTION = Arrays.asList(new Node(0, 1), new Node(0, -1), new Node(1, 0),
            new Node(-1, 0));

    private static final int CHESTGUN=10;
    private static final int CHESTPLAYER=15;
    private static final int BETTERGUNPLAYER=20;
    private static final int ENEMY=2;
    private static final int MAX=123456789;
    private static int countToReHeal=0;
    private static int countToAttack=0;
    private static int countToFire=0;
    private static int lengthPath(Node x, Node y,List<Node> BlockNodes,GameMap gameMap) {

        String path= PathUtils.getShortestPath(gameMap,BlockNodes,x,y,true);
        if(path==null) {
            int minDistance=MAX;
            for(Node dir : DIRECTION) {
                if(!BlockNodes.contains(add(dir,y))) {
                    path=PathUtils.getShortestPath(gameMap,BlockNodes,x,add(dir,y),true);
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
        return (Math.abs(x.x - y.x) + Math.abs(x.y - y.y))*180000 / (y.getDamage()*y.getDamage());
    } // ưu tiên với súng có dame to hơn

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

        for (Obstacle chest : ListChest) if(checkInsideSafeArea(chest, gameMap.getDarkAreaSize(), gameMap.getMapSize())) {
            int distanceToChest = lengthPath(currentNode, chest,BlockNodes,gameMap);
            if (distanceToChest < minDistance) {
                minDistance = distanceToChest;
                nearestChest = chest;
            }
        }

        return nearestChest;
    }// find nearest chest

    private static HealingItem usingHealingItem(Hero hero, GameMap gameMap) throws IOException {
        Player Me=gameMap.getCurrentPlayer();
        if(hero.getInventory().getListHealingItem().isEmpty()) return null;
        //if(!checkInsideSafeArea(new Node(Me.getX(),Me.getY()), gameMap.getDarkAreaSize(), gameMap.getMapSize())) return null;
        if(Me.getHp()>90) return null;
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
        if(bestHeal!=null) {
                countToReHeal = bestHeal.getUsageTime()+1;

        }
        return bestHeal;

    }// hồi máu hợp lí

    private static boolean isAround(Node currentNode, Node nearestPlayerNode) {

        return (distance(currentNode,nearestPlayerNode)==1);
    }

    private static boolean trapInMid(Node x, Node y, List<Obstacle> listTraps,List<Obstacle> listChest) {
        for(Obstacle mid : listTraps){
            if(distance(x,mid)+distance(mid,y)<distance(x, y)){
                return true;
            }
        }
        for(Obstacle mid : listChest){
            if(distance(x,mid)+distance(mid,y)<distance(x, y)){
                return true;
            }
        }
        return false;
    }// check any trap or chest in shoot line

    private static Weapon findNearestMelee(Node currentNode, List<Node> BlocksNodes, GameMap gameMap, List<Weapon> meleeList) {
        Weapon nearestMelee = null;
        int minDistance = MAX;
        for (Weapon item : meleeList) if(checkInsideSafeArea(item, gameMap.getDarkAreaSize(), gameMap.getMapSize())
                && !BlocksNodes.contains(currentNode)) {
            int distanceToItem = lengthPath(currentNode, item, BlocksNodes, gameMap);
            if (distanceToItem <= 10 && distanceToItem < minDistance) {
                minDistance = distanceToItem;
                nearestMelee = item;
            }
        }
        return nearestMelee;
    }

    private static HealingItem findNearestHealingItem(Node currentNode,List<Node> BlocksNodes,GameMap gameMap, List<HealingItem> healthItems) {
        HealingItem nearestHealingItem = null;
        int minDistance = MAX;
        for (HealingItem item : healthItems) if(checkInsideSafeArea(item, gameMap.getDarkAreaSize(), gameMap.getMapSize())
                && !BlocksNodes.contains(currentNode)){
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
        for (Armor item : armorList) if(checkInsideSafeArea(item, gameMap.getDarkAreaSize(), gameMap.getMapSize())
                && !BlocksNodes.contains(currentNode)){
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
    } // hướng để attack

    // main tactic
    private static int try_all( Node currentNode, Hero hero,List<Node> OtherPlayerNodes, List<Node> BlocksNodes,
                                GameMap gameMap ) throws IOException {

        Node nearestPlayerNode =null;
        if(!OtherPlayerNodes.isEmpty()) nearestPlayerNode=OtherPlayerNodes.getFirst();
        int minDisToPlayer = MAX;

        List<Weapon> allGun= gameMap.getAllGun();
        List<Weapon> gunList = new ArrayList<>();
        for(Weapon w:allGun) if(checkInsideSafeArea(w, gameMap.getDarkAreaSize(), gameMap.getMapSize())){
            gunList.add(w);
        }

        for (Node P : OtherPlayerNodes) {
            int disToPlayer = lengthPath(P, currentNode,BlocksNodes,gameMap);
            if (disToPlayer < minDisToPlayer) {
                minDisToPlayer = disToPlayer;
                nearestPlayerNode = P;
            }
        } // find nearest player
        if( (nearestPlayerNode!=null||minDisToPlayer>8) && countToReHeal==0) {
            HealingItem curHeal= usingHealingItem(hero,gameMap);
            if(curHeal!=null) {
                hero.useItem(curHeal.getId());
                System.out.println("Healing!!!!!");
                return 1;
            }
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
        Weapon curGun = hero.getInventory().getGun(); // Gun
        boolean pickedUpGun = (curGun != null);
        if(!pickedUpGun) System.out.println("Have gun?: " + pickedUpGun);
        else System.out.println("Currnet gun?: "+curGun.getId()+", Dame: "+curGun.getDamage());

        Weapon curMelee = hero.getInventory().getMelee(); // Melee
        boolean pickedBestMelee = curMelee.getId().equals("LIGHT_SABER");
        System.out.println("Current melee?: " + curMelee.getId()+", Dame: "+curMelee.getDamage());

        int  numberHealItem=hero.getInventory().getListHealingItem().size();
        boolean isFullHeal=(numberHealItem==4);
        System.out.println("Number of heal items?: " + numberHealItem);

        List<Armor> MyarmorList=hero.getInventory().getListArmor();
        boolean isFullArmor,haveVest=false,haveHelmet=false;
        for(Armor A: MyarmorList){
            if(A.getId().equals("VEST")){
                haveVest=true;
            }
            if(A.getId().equals("HELMET")||A.getId().equals("POT")){
                haveHelmet=true;
            }
        }
        isFullArmor=(haveVest && haveHelmet);
        System.out.println("Have Vest?: " + haveVest);
        System.out.println("Have Helmet?: " + haveHelmet);
        System.out.println("Have full armor?: " + isFullArmor);

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
            hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestHealingItem, true));
            return 1;
        }  //move to healing item and pick up if near

        Armor nearestArmor = findNearestArmor(currentNode,BlocksNodes,gameMap,gameMap.getListArmors());
        if(nearestArmor!=null && !isFullArmor ) {
            if (
                    (nearestArmor.getId().equals("VEST") && !haveVest) ||
                            (nearestArmor.getId().equals("HELMET") && !haveHelmet)||
                            (nearestArmor.getId().equals("POT")&& !haveHelmet)
            )
            {
                if (currentNode.getX() == nearestArmor.getX() && currentNode.getY() == nearestArmor.getY()) {
                    System.out.println("Picked up Armor: " +nearestArmor.getId());
                    hero.pickupItem();
                }
                else {
                    System.out.println("Move to armor item: " + nearestArmor);
                    BlocksNodes.addAll(CHESTLISTNODE);
                    hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestArmor, true));
                }
                return 1;
            }
        }  //move to armor item and pick up if near

        boolean isFullSet=(isFullArmor&&isFullHeal&&pickedBestMelee);

//        if(pickedUpGun && (haveHelmet||haveVest) ){
//            hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, new Node(1,1), true));
//            return 0;
//        }

        if(nearestPlayerNode!=null&&countToAttack==0&& minDisToPlayer<=3 && distance(currentNode,nearestPlayerNode)<=3){
            if( (Me.getHp()>55 && pickedBestMelee) ||
                    (Me.getHp()>65 && !curMelee.getId().equals("HAND"))
            ){
                if(isAround(currentNode,nearestPlayerNode)){
                    System.out.println("Attack player");
                    String attackDirection = determineAttackDirection(currentNode, nearestPlayerNode);
                    countToAttack=curMelee.getCooldown();
                    hero.attack(attackDirection);  // Tấn công player theo hướng xác định
                }
                else{
                    System.out.println("Go to the next of player");
                    BlocksNodes.addAll(CHESTLISTNODE);
                    BlocksNodes.remove(nearestPlayerNode);
                    hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, true));
                }
                return 1;
            }
        }// chém khi có vũ khí xịn và gần địch


        if (!pickedUpGun) {

            Weapon nearestGun = null;

            for (Weapon weapon : gunList)
                if(PathUtils.getShortestPath(gameMap,BlocksNodes,currentNode,weapon,true)!=null){
                    if(nearestGun==null) {
                        nearestGun=weapon;
                    }
                    else if (dis_weapon(currentNode, weapon) < dis_weapon(currentNode, nearestGun)) {
                        nearestGun = weapon;
                    }
                } // find mearest gun

            if (nearestGun!=null&& currentNode.getX() == nearestGun.getX() && currentNode.getY() == nearestGun.getY()) {

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
            if(nearestGun==null) distToGun=MAX;
            else distToGun= lengthPath(currentNode, nearestGun,BlocksNodes,gameMap);  // Khoảng cách đến súng

            // Quyết định dựa trên khoảng cách đến rương và súng
            if (nearestChest!=null && ( (Me.getHp()<=60) ||(!isFullSet&&(distToChest + CHESTGUN <= distToGun))) ) {
                if (distance(currentNode,nearestChest) == 1)
                {
                    System.out.println("Breaking chest...");

                    // Xác định hướng tấn công rương
                    String attackDirection = determineAttackDirection(currentNode, nearestChest);
                    hero.attack(attackDirection);  // Tấn công rương theo hướng xác định
                }//break it
                else{
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
                    System.out.println("Moving to nearest chest...");
                    for(Node p:CHESTLISTNODE) {
                        if (p.getX() != nearestChest.getX() && p.getY() != nearestChest.getY()) {
                            BlocksNodes.add(new Node(p.getX(), p.getY()));
                        }
                    }// thêm các rương là vật cản
                    String Path=PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, true);
                    System.out.println("[Path to chest] "+Path);
                    hero.move(Path);  // Di chuyển đến vị trí liền kề rương
                }// move to neartest chest


            } // move to chest and break
            else if(nearestGun!=null) {
                System.out.println("Move to nearest Gun");
                BlocksNodes.addAll(CHESTLISTNODE);
                BlocksNodes.remove(nearestGun);
                String Path=PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestGun, true);
                System.out.println("[Path to gun] "+Path);
                hero.move(Path);  // Di chuyển đến gun
            }//move to gun
            else{
                System.out.println("?????");
                return 0;
            }
            return 1;
        } // no gun, find near healing item or chest or gun (not find player)
        else {
            // Đã có súng
            boolean fire_or_move = false;

            if(countToFire==0) for (Node P : OtherPlayerNodes) {

                if (lengthPath(P, currentNode,BlocksNodes,gameMap) <= curGun.getRange() && !trapInMid(currentNode, P,gameMap.getListTraps(),CHESTLIST)) {
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
            }// fire if we can and no trap in shoot line

            if (fire_or_move) {
                System.out.println("Fired");
                if(!curMelee.getId().equals("HAND")) countToFire=curGun.getCooldown();// nếu có melee thì set countToFire để lại gần địch
                return 1;
            } // Print that we have shot
            else {

                // Move to the nearest player or chest or pick better gun

                Weapon nearestGun = null;
                int minDistToGun=MAX;

                for (Weapon weapon : gunList){
                    String Path=PathUtils.getShortestPath(gameMap,BlocksNodes,currentNode,weapon,true);
                    if(Path!=null){
                        if(nearestGun==null) {
                            nearestGun=weapon;
                            minDistToGun=Path.length();
                        }
                        else if (Path.length() < minDistToGun) {
                            nearestGun = weapon;
                            minDistToGun=Path.length();
                        }
                    }
                }// find mearest gun

                boolean betterGun= nearestGun != null && nearestGun.getDamage() > curGun.getDamage();

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

                int distToPlayer = MAX;
                if(nearestPlayerNode!=null) distToPlayer=lengthPath(currentNode, nearestPlayerNode,BlocksNodes,gameMap);

                if (nearestChest!=null &&( (Me.getHp()<=60)
                        || (!isFullSet && (distToChest + CHESTPLAYER <= distToPlayer)) )) {

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
                        }//add hết chest trừ nearest chest

                        String Path=PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestChest, true);
                        System.out.println("[Path to chest] "+Path);
                        hero.move(Path);  // Di chuyển đến vị trí liền kề rương
                    } else {
                        System.out.println("Breaking chest...");

                        // Xác định hướng tấn công rương
                        String attackDirection = determineAttackDirection(currentNode, nearestChest);
                        hero.attack(attackDirection);  // Tấn công rương theo hướng xác định
                    }
                    return 1;
                }// move to chest and break

                if(betterGun && minDistToGun + BETTERGUNPLAYER <=distToPlayer){
                    if(currentNode.getX()==nearestGun.getX() && currentNode.getY()==nearestGun.getY()){
                        System.out.println("INVOKE CURRENT GUN");
                        hero.revokeItem(curGun.getId());
                    }//vứt curGun, đưa về trạng thái !pickedUpGun -> ko cần xử lí nhặt súng ở đây nữa
                    else{
                        System.out.println("Move to better gun");
                        BlocksNodes.addAll(CHESTLISTNODE);
                        BlocksNodes.remove(nearestGun);
                        hero.move(PathUtils.getShortestPath(gameMap,BlocksNodes,currentNode,nearestGun,true));
                    }
                    return 1;
                }// move to better gun

                if(nearestPlayerNode!=null) {
                    BlocksNodes.remove(nearestPlayerNode);// xoá thằng này khỏi block để tìm đường đi
                    BlocksNodes.addAll(CHESTLISTNODE);
                    String Path=PathUtils.getShortestPath(gameMap, BlocksNodes, currentNode, nearestPlayerNode, true);
                    System.out.println("[Path to player] "+Path);
                    hero.move(Path);  // Di chuyển đến player
                    return 1;
                }// move to nearesr player
            }

        } // have gun, move to chest,better gun or player
        return 0;
    }



    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME,PLAYER_KEY);

        Emitter.Listener onMapUpdate = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    System.out.println("Start a try");
                    GameMap gameMap = hero.getGameMap(); // map
                    gameMap.updateOnUpdateMap(args[0]);

                    if (countToReHeal > 0) countToReHeal--;
                    if (countToAttack > 0) countToAttack--;
                    if (countToFire > 0) countToFire--;
                    Player Me = gameMap.getCurrentPlayer(); // Me
                    Node currentNode = new Node(Me.getX(), Me.getY());
                    if (!Me.getIsAlive()) {
                        System.out.println("You are dead");
                    }
                    else {
                        List<Player> OtherPlayerList = gameMap.getOtherPlayerInfo(); // Other Players List
                        List<Node> OtherPlayerNodes = new ArrayList<>(); // OtherPlayerNodes

                        for (Player p : OtherPlayerList) {
                            if (p.getIsAlive() && checkInsideSafeArea(new Node(p.getX(), p.getY()), gameMap.getDarkAreaSize(), gameMap.getMapSize())) {
                                OtherPlayerNodes.add(new Node(p.getX(), p.getY()));
                            }
                        } //  create OtherPlayerNodes by alive players

                        List<Obstacle> Blocks = gameMap.getListIndestructibleObstacles(); // Blocks(Wall+Trap)
                        Blocks.addAll(gameMap.getListTraps());

                        List<Enemy> ListEnemies = gameMap.getListEnemies(); // List of Enemies

                        List<Node> BlocksNodes = new ArrayList<>(); // BlocksNodes

                        for (Obstacle O : Blocks) {

                            BlocksNodes.add(new Node(O.getX(), O.getY()));
                        } // creat Blocknodes (wall + trap + other players)

                        BlocksNodes.addAll(OtherPlayerNodes);


                        for (Enemy E : ListEnemies) {
                            Node enemy = new Node(E.getX(), E.getY());
                            for (int i = -ENEMY; i <= ENEMY; ++i) {
                                for (int j = -ENEMY; j <= ENEMY; ++j)
                                    if (Math.abs(i) + Math.abs(j) <= ENEMY) {

                                        BlocksNodes.add(add(new Node(i, j), enemy));
                                    }
                            }
                        } // add block around enemies and check if they are coming


                        System.out.println("Return: " + try_all(currentNode, hero, OtherPlayerNodes, BlocksNodes, gameMap));
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
//Chưa loại các gun đang dưới chân player khác(ko nhớ fix chưa)
