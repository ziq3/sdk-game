import io.socket.emitter.Emitter;
import jsclub.codefest2024.sdk.*;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.factory.WeaponFactory;
import jsclub.codefest2024.sdk.model.*;
import jsclub.codefest2024.sdk.model.equipments.*;
import jsclub.codefest2024.sdk.model.obstacles.Obstacle;
import jsclub.codefest2024.sdk.model.players.Player;
import jsclub.codefest2024.sdk.model.weapon.Weapon;
import java.io.IOException;
import java.util.*;

public class Main {
    private static final String SERVER_URL = "https://cf-server.jsclub.dev";
    private static final String GAME_ID = "191492";
    private static final String PLAYER_NAME = "test-01";
    private static final String PLAYER_KEY = "x";
    private static final List<Node> DIRECTIONS = Arrays.asList(new Node(0, 1), new Node(0, -1), new Node(1, 0),
            new Node(-1, 0));
    private static final List<Node> DIRECTIONS_REVERSE = Arrays.asList(new Node(0, -1), new Node(0, 1), new Node(-1, 0),
            new Node(1, 0));
    private static final List<String> DIRECTIONS_STR = Arrays.asList("u", "d", "r", "l");
    private static final List<Node> DIRECTIONS2 = Arrays.asList(new Node(1, 1), new Node(1, -1), new Node(-1, -1),
            new Node(-1, 1));
    private static final List<Node> DIFF_NODE_THROW = Arrays.asList(new Node(0, 6), new Node(0, -6), new Node(6, 0),
            new Node(-6, 0));

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, PLAYER_KEY);
        Emitter.Listener onMapUpdate = new Emitter.Listener() {
            GameMap gameMap;
            Player me;
            Inventory myInventory;
            List<List<Integer>> g, trace;
            List<Node> restrictedNodes, restrictedNodesWithoutPlayers;
            List<Player> otherPlayers;
            int meleeCooldown = 0, gunCooldown = 0;
            Weapon gun, melee = WeaponFactory.getWeaponById("HAND");
            int meleeDame, gunDame;
            int time = -1, previousDarkSide = 0;
            boolean haveGun = false, haveMelee = false, haveThrow = false;
            List<HealingItem> listHealing = new ArrayList<>();
            final EnemyMap enemyMap = new EnemyMap();

            void getItem(Node target) {
                if (Utils.equal(me, target)) {
                    pickupItem();
                } else {
                    move(getPath(target));
                }
            }

            void pickupItem() {
                System.out.println("Pickup item");
                try {
                    hero.pickupItem();
                } catch (Exception ignored) {
                }
            }

            void attack(String x) {
                System.out.println("Attack");
                meleeCooldown = melee.getCooldown();
                try {
                    hero.attack(x);
                } catch (Exception ignored) {

                }
            }

            void shoot(String x) {
                if (me.getBulletNum() == 1) {
                    haveGun = false;
                }
                System.out.println("Shoot");
                gunCooldown = gun.getCooldown();
                try {
                    hero.shoot(x);
                } catch (Exception ignored) {
                }
            }

            void move(String x) {
                System.out.println("Move");
                try {
                    hero.move(x);
                } catch (Exception ignored) {
                }
            }

            void useItem(String x) {
                System.out.println("Use item " + x);
                try {
                    hero.useItem(x);
                } catch (Exception ignored) {
                }
            }

            void throwAttack(String x) {
                System.out.println("Throw attack");
                haveThrow = false;
                try {
                    hero.throwItem(x);
                } catch (Exception ignored) {
                }
            }

            void revokeItem(String x) {
                System.out.println("Revoke item " + x);
                try {
                    hero.revokeItem(x);
                } catch (Exception ignored) {
                }
            }

            void init() {
                me = gameMap.getCurrentPlayer();
                myInventory = hero.getInventory();
                gunCooldown -= 1;
                meleeCooldown -= 1;
                meleeDame = 0;
                gunDame = 0;
                restrictedNodesWithoutPlayers = new ArrayList<>();
                restrictedNodes = new ArrayList<>();
                otherPlayers = new ArrayList<>();
                for (Player p : gameMap.getOtherPlayerInfo()) {
                    if (p.getIsAlive()) {
                        otherPlayers.add(p);
                    }
                }
                if (me.getHp() == 0) {
                    haveGun = false;
                    haveMelee = false;
                    haveThrow = false;
                    listHealing.clear();
                }
                if (haveMelee) {
                    meleeDame = melee.getDamage();
                }
                if (haveGun) {
                    gunDame = gun.getDamage();
                }
                time += 1;
                if (time <= 16) {
                    enemyMap.calcEnemy(gameMap, time);
                }
                for (int i = 0; i < gameMap.getMapSize(); ++i) {
                    for (int j = 0; j < gameMap.getMapSize(); ++j) {
                        if (enemyMap.isBlock(time + 1, new Node(i, j), gameMap)) {
                            restrictedNodesWithoutPlayers.add(new Node(i, j));
                        }
                    }
                }
                List<Obstacle> listConstruct = gameMap.getListTraps();
                listConstruct.addAll(gameMap.getListIndestructibleObstacles());
                listConstruct.addAll(gameMap.getListChests());
                for (Node p : listConstruct) {
                    restrictedNodes.add(new Node(p.getX(), p.getY()));
                }

                restrictedNodesWithoutPlayers.addAll(restrictedNodes);
                for (Node p : otherPlayers) {
                    for (int i = 0; i < 4; ++i) {
                        Node nearPlayer = new Node(p.getX(), p.getY());
                        // gun.range()-1
                        for (int j = 0; j < 3; ++j) {
                            nearPlayer = Utils.add(nearPlayer, DIRECTIONS.get(i));
                            restrictedNodes.add(nearPlayer);
                        }
                    }
                }

            }

            void bfs() {
                int mapSize = gameMap.getMapSize();

                g = Utils.initializeList(mapSize, 99999999);
                List<List<Boolean>> isRestrictedNodes = Utils.initializeList(mapSize, false);
                trace = Utils.initializeList(mapSize, -1);
                for (Node point : restrictedNodes) {
                    if (Utils.isValid(point, gameMap)) {
                        isRestrictedNodes.get(point.x).set(point.y, true);
                    }
                }
                Queue<Node> queue = new LinkedList<>();
                queue.add(me);
                g.get(me.getX()).set(me.getY(), 0);
                while (!queue.isEmpty()) {
                    Node u = queue.poll();
                    for (int dir = 0; dir < 4; ++dir) {
                        Node v = Utils.add(u, DIRECTIONS.get(dir));
                        if (!Utils.isValid(v, gameMap))
                            continue;
                        int cost = g.get(u.x).get(u.y) + 1;
                        if (Utils.isInsideSafeArea(me, gameMap)) {
                            if (enemyMap.isBlock(time + cost, v, gameMap)
                                    || !Utils.isInsideSafeArea(v, gameMap))
                                continue;
                        }
                        if (isRestrictedNodes.get(v.x).get(v.y)) {
                            continue;
                        }
                        if (g.get(v.x).get(v.y) > cost) {
                            g.get(v.x).set(v.y, cost);
                            trace.get(v.x).set(v.y, dir);
                            queue.add(v);
                        }
                    }
                }
            }

            String getPath(Node target) {
                while (true) {
                    int dir = trace.get(target.x).get(target.y);
                    String stringDir = DIRECTIONS_STR.get(dir);
                    target = Utils.add(target, DIRECTIONS_REVERSE.get(dir));
                    if (Utils.equal(target, me)) {
                        return stringDir;
                    }
                }
            }

            int distance(Node p) {
                if (p == null || !Utils.isValid(p, gameMap)) {
                    return 222222222;
                }
                return g.get(p.x).get(p.y);
            }

            <T extends Node> T nearestNode(List<T> nodes) {
                if (nodes.isEmpty()) {
                    return null;
                }
                T nearestNode = nodes.getFirst();
                for (T node : nodes) {
                    if (distance(node) < distance(nearestNode)) {
                        nearestNode = node;
                    }
                }
                return nearestNode;
            }

            void mAttack(Node target) {
                if (me.getX() + 1 == target.getX()) {
                    attack("r");
                }
                if (me.getX() - 1 == target.getX()) {
                    attack("l");
                }
                if (me.getY() + 1 == target.getY()) {
                    attack("u");
                }
                if (me.getY() - 1 == target.getY()) {
                    attack("d");
                }
            }

            void getChest(Node nextToChest) {
                if (Utils.equal(nextToChest, me)) {
                    for (Node p : gameMap.getListChests()) {
                        if (Utils.distance(p, me, gameMap) == 1) {
                            mAttack(p);
                            return;
                        }
                    }
                } else {
                    move(getPath(nextToChest));
                }
            }

            int getPointArmor(Armor armor) {
                if (armor == null)
                    return 0;
                int gainArmor = armor.getDamageReduce();
                List<Armor> listArmors = hero.getInventory().getListArmor();
                if (!armor.getId().equals("VEST")) {
                    for (Armor currentArmor : listArmors) {
                        if (!currentArmor.getId().equals("VEST")) {
                            gainArmor -= currentArmor.getDamageReduce();
                        }
                    }
                } else {
                    if (me.getDamageReduction() >= 20) {
                        return 0;
                    }
                }
                return gainArmor * 400 / (distance(armor) + 1);
            }

            int getPointHealth(int health) {
                if (listHealing.size() == 4)
                    return 0;
                double urgencyFactor = 1 + (100.0 - me.getHp()) / 10;
                return (int) (health * 200 * urgencyFactor);
            }

            int getPointHealth(HealingItem health) {
                if (health == null)
                    return 0;
                return getPointHealth(health.getHealingHP()) / (distance(health) + health.getUsageTime() + 1);
            }

            int getPointPlayer(Player player, Node nextToPlayer) {
                if (player == null || distance(player) == 0)
                    return 0;
                int myHp = me.getHp() * (100 + me.getDamageReduction()) / 100;
                int targetHp = player.getHp() * (100 + player.getDamageReduction()) / 100;
                int myDame = meleeDame + gunDame;
                int targetDame = 40;
                if (player.getBulletNum() != 0) {
                    targetDame = 60;
                }
                double factor = myHp * 1.0 / targetHp * myDame * 1.0 / targetDame;
                factor = factor * factor;
                return (int) (100 * 100 * factor / (distance(nextToPlayer) + 10));
            }

            int getPointWeapon(Weapon weapon) {
                if (weapon == null)
                    return 0;
                int pointWeapon = 0;
                if (weapon.getType() == ElementType.THROWABLE) {
                    if (!haveThrow) {
                        pointWeapon = weapon.getDamage() - 15;
                    }
                }
                if (weapon.getType() == ElementType.MELEE) {
                    pointWeapon = (weapon.getDamage() - meleeDame) * 3;
                }
                if (weapon.getType() == ElementType.GUN) {
                    pointWeapon = (weapon.getDamage() - gunDame) * 3;
                }
                return pointWeapon * 100 / (distance(weapon) + 1);
            }

            int getPointChest(Node chest) {
                if (chest == null)
                    return 0;
                int pointChest = 0;
                if (me.getDamageReduction() < 20) {
                    pointChest += 20 * 400 * (1 - Math.pow(1 - 0.02, 4));
                }
                if (me.getDamageReduction() == 20 || me.getDamageReduction() == 0) {
                    pointChest += 5 * 400 * (1 - Math.pow(1 - 0.03, 4)) + 10 * 400 * (1 - Math.pow(1 - 0.05, 4));
                }
                pointChest += getPointHealth(20);
                if (meleeDame <= 45) {
                    pointChest += (55 - meleeDame) * 300 * (1 - Math.pow(1 - 0.05, 4));
                }
                if (meleeDame == 0) {
                    pointChest += 45 * 300 * (1 - Math.pow(1 - 0.16, 4));
                }
                if (!haveThrow) {
                    pointChest += 10 * 100 * (1 - Math.pow(1 - 0.40, 4));
                }
                return pointChest / (distance(chest) + 4);
            }

            void getWeapon(Weapon weapon, int type) {
                if (Utils.equal(weapon, me)) {
                    if (type == 4) {
                        if (haveMelee) {
                            System.out.println("bo melee");
                            revokeItem(melee.getId());
                            haveMelee = false;
                            return;
                        } else {
                            haveMelee = true;
                            melee = weapon;
                        }
                    }
                    if (type == 5) {
                        if (haveGun) {
                            System.out.println("bo gun");
                            revokeItem(gun.getId());
                            haveGun = false;
                            return;
                        } else {
                            gun = weapon;
                            haveGun = true;
                        }
                    }
                    if (type == 6) {
                        haveThrow = true;
                    }
                }
                getItem(weapon);
            }

            void getArmor(Armor armor) {
                if (Utils.equal(armor, me)) {
                    List<Armor> listArmors = myInventory.getListArmor();
                    if (armor.getId().equals("POT")
                            && (listArmors.size() == 2
                                    || (listArmors.size() == 1 && listArmors.getFirst().getId().equals("HELMET")))) {
                        revokeItem("HELMET");
                        return;
                    }
                }
                getItem(armor);
            }

            boolean contains(List<Node> list, Node node) {
                for (Node p : list) {
                    if (Utils.equal(p, node))
                        return true;
                }
                return false;
            }

            void calculateOptimizedMove() {
                List<Node> nextToChest = new ArrayList<>();
                for (Obstacle p : gameMap.getListChests()) {
                    if (!Utils.isInsideSafeArea(p, gameMap))
                        continue;
                    for (int i = 0; i < 4; ++i) {
                        nextToChest.add(Utils.add(p, DIRECTIONS.get(i)));
                    }
                }
                List<Node> nextToPlayer = new ArrayList<>();
                Node nearestNextToChest = nearestNode(nextToChest);
                Player nearestPlayer = otherPlayers.getFirst();
                Node nearestNextToPlayer = nearestNode(nextToPlayer);
                for (Player p : otherPlayers) {
                    if (!Utils.isInsideSafeArea(p, gameMap))
                        continue;
                    for (int i = 0; i < 4; ++i) {
                        Node addNode = Utils.add(p, DIRECTIONS2.get(i));
                        if (distance(addNode) < distance(nearestNextToPlayer)) {
                            nearestNextToPlayer = addNode;
                            nearestPlayer = p;
                        }
                    }
                }
                HealingItem nearestHealth = nearestNode(gameMap.getListHealingItems());
                Armor nearestArmor = nearestNode(gameMap.getListArmors());
                Weapon nearestMelee = nearestNode(gameMap.getAllMelee());
                Weapon nearestGun = nearestNode(gameMap.getAllGun());
                Weapon nearestThrow = nearestNode(gameMap.getAllThrowable());
                List<Integer> pointItems = new ArrayList<>();
                pointItems.add(getPointChest(nearestNextToChest));
                pointItems.add(getPointPlayer(nearestPlayer, nearestNextToPlayer));
                pointItems.add(getPointHealth(nearestHealth));
                pointItems.add(getPointArmor(nearestArmor));
                pointItems.add(getPointWeapon(nearestMelee));
                pointItems.add(getPointWeapon(nearestGun));
                pointItems.add(getPointWeapon(nearestThrow));
                List<Node> target = new ArrayList<>();
                target.add(nearestNextToChest);
                target.add(nearestNextToPlayer);
                target.add(nearestHealth);
                target.add(nearestArmor);
                target.add(nearestMelee);
                target.add(nearestGun);
                target.add(nearestThrow);
                for (int i = 0; i < 7; ++i) {
                    System.out.println(pointItems.get(i));
                }
                int maxPoint = Collections.max(pointItems);
                for (int i = 0; i < 7; ++i) {
                    if (pointItems.get(i) == maxPoint) {
                        System.out.println("Move: " + i + " " + maxPoint);
                        System.out.println(
                                "Target2: " + target.get(i).getX() + " " + target.get(i).getY());
                        if (i == 0) {
                            getChest(target.get(i));
                        }
                        if (i == 1) {
                            move(getPath(target.get(i)));
                        }
                        if (i == 2) {
                            if (Utils.equal(target.get(i), me)) {
                                listHealing.add(nearestHealth);
                            }
                            getItem(target.get(i));
                        }
                        if (i == 3) {
                            getArmor(nearestArmor);
                        }
                        if (i == 4) {
                            getWeapon(nearestMelee, i);
                        }
                        if (i == 5) {
                            getWeapon(nearestGun, i);
                        }
                        if (i == 6) {
                            getWeapon(nearestThrow, i);
                        }
                        return;
                    }
                }
            }

            boolean tryHealth() {
                Node nearestPlayerReal = otherPlayers.getFirst();
                for (Node p : otherPlayers) {
                    if (Utils.distance(p, me, gameMap) < Utils.distance(nearestPlayerReal, me, gameMap)) {
                        nearestPlayerReal = p;
                    }
                }
                int diffX = Math.abs(nearestPlayerReal.getX() - me.getX());
                int diffY = Math.abs(nearestPlayerReal.getY() - me.getY());
                int timeToReach = Math.min(diffX, diffY) + Math.max(0, Math.max(diffX, diffY) - 4);
                int maxTimeUsage = Math.max(timeToReach, Math.min(meleeCooldown, gunCooldown));
                if (timeToReach > 1) {
                    int maxTimeSafe = 0;
                    for (int i = 1; i <= 4; ++i) {
                        if (enemyMap.isBlock(time + i, me, gameMap))
                            break;
                        maxTimeSafe = i;
                    }
                    maxTimeUsage = Math.min(maxTimeSafe, timeToReach);
                }
                if (!listHealing.isEmpty()) {
                    listHealing.sort((a, b) -> b.getHealingHP() - a.getHealingHP());
                }
                for (HealingItem item : listHealing) {
                    if ((me.getHp() + item.getHealingHP() <= 105 || me.getHp() <= 55)
                            && maxTimeUsage >= item.getUsageTime()) {
                        useItem(item.getId());
                        listHealing.remove(item);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void call(Object... args) {
                gameMap = hero.getGameMap();
                gameMap.updateOnUpdateMap(args[0]);
                init();
                bfs();
                System.out.println("Vi tri hien tai " + me.getX() + " " + me.getY());
                System.out.println("Co sung " + haveGun);
                System.out.println("Co dao " + haveMelee);
                System.out.println("Co nem " + haveThrow);
                System.out.println("gunCooldown " + gunCooldown);
                System.out.println("meleeCooldown " + meleeCooldown);
                System.out.println("bullet " + me.getBulletNum());
                int sizeSafeArea = gameMap.getDarkAreaSize();
                if (previousDarkSide != sizeSafeArea) {
                    sizeSafeArea += 1;
                }
                if (!PathUtils.checkInsideSafeArea(me, sizeSafeArea, gameMap.getMapSize())) {
                    Node nearest = new Node(-1, -1);
                    for (int i = 0; i < gameMap.getMapSize(); ++i) {
                        for (int j = 0; j < gameMap.getMapSize(); ++j) {
                            Node addNode = new Node(i, j);
                            if (distance(addNode) < distance(nearest)
                                    && PathUtils.checkInsideSafeArea(addNode, sizeSafeArea,
                                            gameMap.getMapSize())) {
                                nearest = addNode;
                            }
                        }
                    }
                    move(getPath(nearest));
                    return;
                }
                previousDarkSide = gameMap.getDarkAreaSize();
                if (haveMelee && meleeCooldown <= 0) {
                    for (Node p : otherPlayers) {
                        if (Utils.distance(me, p, gameMap) == 1) {
                            mAttack(p);
                            return;
                        }
                    }
                }
                if (haveGun && gunCooldown <= 0) {
                    for (Node p : otherPlayers) {
                        if (Math.abs(p.x - me.x) == 0 && Math.abs(p.y - me.y) <= gun.getRange()) {
                            if (p.y < me.getY()) {
                                shoot("d");
                            } else {
                                shoot("u");
                            }
                            return;
                        }
                        if (Math.abs(p.y - me.y) == 0 && Math.abs(p.x - me.x) <= gun.getRange()) {
                            if (p.x < me.getX()) {
                                shoot("l");
                            } else {
                                shoot("r");
                            }
                            return;
                        }
                    }
                }
                if (tryHealth()) {
                    return;
                }
                if (haveThrow) {
                    List<Node> targetThrow = new ArrayList<>();
                    for (int i = 0; i < 4; ++i) {
                        targetThrow.add(Utils.add(DIFF_NODE_THROW.get(i), me));
                    }
                    for (Node p : otherPlayers) {
                        for (int i = 0; i < 4; ++i) {
                            if (Utils.distance(targetThrow.get(i), p, gameMap) <= 1) {
                                throwAttack(DIRECTIONS_STR.get(i));
                                return;
                            }
                        }
                    }
                }
                if ((haveGun && gunCooldown <= 1)
                        || (haveMelee && meleeCooldown <= 1)) {
                    for (Node p : otherPlayers) {
                        if (Math.abs(p.x - me.x) == 1 && Math.abs(p.y - me.y) == 1) {
                            if (me.y + 1 == p.y
                                    && !contains(restrictedNodesWithoutPlayers, new Node(me.x, me.y + 1))) {
                                move("u");
                                return;
                            }
                            if (me.y - 1 == p.y
                                    && !contains(restrictedNodesWithoutPlayers, new Node(me.x, me.y - 1))) {
                                move("d");
                                return;
                            }
                            move(PathUtils.getShortestPath(gameMap, restrictedNodesWithoutPlayers, me, p, false));
                            return;
                        }
                    }
                }
                calculateOptimizedMove();
            }
        };

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}
