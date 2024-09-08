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
import jsclub.codefest2024.sdk.model.Inventory;
import jsclub.codefest2024.sdk.model.ElementType;
import java.io.IOException;
import java.util.*;

public class Main {
    private static final String SERVER_URL = "https://cf-server.jsclub.dev";
    private static final String GAME_ID = "129935";
    private static final String PLAYER_NAME = "Nguoi Lua Gian Doi";

    private static Node add(Node x, Node y) {

        return new Node(x.x + y.x, x.y + y.y);
    }

    private static final List<Node> DIRECTIONS = Arrays.asList(new Node(0, 1), new Node(0, -1), new Node(1, 0),
            new Node(-1, 0));
    private static final List<Node> DIRECTIONS_REVERSE = Arrays.asList(new Node(0, -1), new Node(0, 1), new Node(-1, 0),
            new Node(1, 0));
    private static final List<String> DIRECTIONS_STR = Arrays.asList("u", "d", "r", "l");
    private static final List<Node> DIRECTIONS2 = Arrays.asList(new Node(1, 1), new Node(1, -1), new Node(-1, -1),
            new Node(-1, 1));

    private static final List<Node> diffNodeThrow = Arrays.asList(new Node(0, 6), new Node(0, -6), new Node(6, 0),
            new Node(-6, 0));

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME); // Our hero
        Emitter.Listener onMapUpdate = new Emitter.Listener() {
            ArrayList<ArrayList<Integer>> g = new ArrayList<>();
            ArrayList<ArrayList<Integer>> trace = new ArrayList<>();
            GameMap gameMap = null;
            Node myPos = null;
            List<Node> restrictedNodes = new ArrayList<>();
            List<Node> restrictedNodesWithoutPlayers = new ArrayList<>();
            Inventory myInventory = null;
            List<Node> otherPlayers = new ArrayList<>();
            Player me;
            int meleeCooldown = 0;
            int gunCooldown = 0;
            Weapon gun;
            boolean haveGun;
            Weapon melee;
            boolean haveMelee;
            Weapon throwWeapon;
            boolean haveThrow;

            <T extends Node> Boolean equal(T x, T y) {

                return x.getX() == y.getX() && x.getY() == y.getY();
            }

            void init() {
                me = gameMap.getCurrentPlayer();
                myInventory = hero.getInventory();
                gun = myInventory.getGun();
                haveGun = gun != null;
                melee = myInventory.getMelee();
                haveMelee = !melee.getId().equals("HAND");
                throwWeapon = myInventory.getThrowable();
                haveThrow = throwWeapon != null;
                myPos = gameMap.getCurrentPlayer();
                restrictedNodes = new ArrayList<>();
                List<Obstacle> listConstruct = gameMap.getListTraps();
                listConstruct.addAll(gameMap.getListIndestructibleObstacles());
                listConstruct.addAll(gameMap.getListChests());
                for (Node p : listConstruct) {
                    restrictedNodes.add(new Node(p.getX(), p.getY()));
                }

                List<Enemy> ListEnemies = gameMap.getListEnemies(); // List of Enemies
                for (Enemy E : ListEnemies) {
                    Node enemy = new Node(E.getX(), E.getY());
                    for (int i = -2; i <= 2; ++i) {
                        for (int j = -2; j <= 2; ++j) {
                            if (Math.abs(i) + Math.abs(j) <= 3) {
                                restrictedNodes.add(add(new Node(i, j), enemy));
                            }
                        }
                    }
                }
                restrictedNodesWithoutPlayers = new ArrayList<>(restrictedNodes);
                List<Player> allPlayer = gameMap.getOtherPlayerInfo(); // Other Players
                for (Player p : allPlayer) {
                    if (equal(p, myPos)) {
                        continue;
                    }
                    if (p.getIsAlive()) {
                        otherPlayers.add(new Node(p.getX(), p.getY()));
                    }
                }
                for (Node p : otherPlayers) {
                    for (int i = 0; i < 4; ++i) {
                        Node nearPlayer = new Node(p.getX(), p.getY());
                        // gun.range()-1
                        for (int j = 0; j < 3; ++j) {
                            nearPlayer = add(nearPlayer, DIRECTIONS.get(i));
                            restrictedNodes.add(nearPlayer);
                        }
                    }
                }

            }

            void bfs() {
                int[] dx = { 0, 0, 1, -1 };
                int[] dy = { 1, -1, 0, 0 };
                int mapSize = gameMap.getMapSize();
                int darkAreaSize = gameMap.getDarkAreaSize();

                g = new ArrayList<>(mapSize);
                trace = new ArrayList<>(mapSize);
                ArrayList<ArrayList<Boolean>> isRestrictedNodes = new ArrayList<>(mapSize);

                for (int i = 0; i < mapSize; i++) {
                    isRestrictedNodes.add(new ArrayList<>(mapSize));
                    g.add(new ArrayList<>(mapSize));
                    trace.add(new ArrayList<>(mapSize));
                    for (int j = 0; j < mapSize; j++) {
                        isRestrictedNodes.get(i).add(false);
                        g.get(i).add(99999999);
                        trace.get(i).add(-1);
                    }
                }
                for (Node point : restrictedNodes) {
                    if (point.x >= 0 && point.x < mapSize && point.y >= 0 && point.y < mapSize) {
                        isRestrictedNodes.get(point.x).set(point.y, true);
                    }
                }

                Queue<Node> queue = new LinkedList<>();
                queue.add(myPos);
                g.get(myPos.getX()).set(myPos.getY(), 0);
                while (!queue.isEmpty()) {
                    Node u = queue.poll();
                    for (int dir = 0; dir < 4; ++dir) {
                        int x = u.x + dx[dir];
                        int y = u.y + dy[dir];
                        if (!PathUtils.checkInsideSafeArea(new Node(x, y), darkAreaSize, mapSize)) {
                            continue;
                        }
                        if (isRestrictedNodes.get(x).get(y)) {
                            continue;
                        }
                        int cost = g.get(u.x).get(u.y) + 1;
                        // System.out.println(cost);
                        if (g.get(x).get(y) > cost) {
                            g.get(x).set(y, cost);
                            trace.get(x).set(y, dir);
                            queue.add(new Node(x, y));
                        }
                    }
                }
            }

            String getPath(Node target) {
                while (true) {
                    int dir = trace.get(target.x).get(target.y);
                    String stringDir = DIRECTIONS_STR.get(dir);
                    target = add(target, DIRECTIONS_REVERSE.get(dir));
                    if (equal(target, myPos)) {
                        return stringDir;
                    }
                }
            }

            int distance(Node p) {
                if (p == null || !PathUtils.checkInsideSafeArea(p, gameMap.getDarkAreaSize(), gameMap.getMapSize())) {
                    return 222222222;
                }
                return g.get(p.x).get(p.y);
            }

            int distance2(Node p1, Node p2) {
                if (p1 == null || p2 == null
                        || !PathUtils.checkInsideSafeArea(p1, gameMap.getDarkAreaSize(), gameMap.getMapSize())
                        || !PathUtils.checkInsideSafeArea(p2, gameMap.getDarkAreaSize(), gameMap.getMapSize())) {
                    return 222222222;
                }
                return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
            }

            <T extends Node> T nearestNode(List<T> nodes, Boolean isChest) {
                if (nodes.isEmpty()) {
                    return null;
                }
                T nearestNode = nodes.getFirst();
                for (T node : nodes) {

                    if (isChest) {
                        if (distance(NextToChest(node)) < distance(NextToChest(nearestNode))) {
                            nearestNode = node;
                        }
                    } else {
                        if (distance(node) < distance(nearestNode)) {
                            nearestNode = node;
                        }
                    }
                }
                return nearestNode;
            }

            Node NextToChest(Node target) {
                Node ans = null;
                for (int i = 0; i < 4; ++i) {
                    Node addNode = add(target, DIRECTIONS.get(i));
                    if (distance(addNode) < distance(ans)) {
                        ans = addNode;
                    }
                }
                return ans;
            }

            void mAttack(Node target) {
                if (myPos.getX() + 1 == target.getX()) {
                    attack("r");
                }
                if (myPos.getX() - 1 == target.getX()) {
                    attack("l");
                }
                if (myPos.getY() + 1 == target.getY()) {
                    attack("u");
                }
                if (myPos.getY() - 1 == target.getY()) {
                    attack("d");
                }
            }

            void getChest(Node target) {
                if (Math.abs(myPos.getX() - target.getX()) + Math.abs(myPos.getY() - target.getY()) == 1) {
                    mAttack(target);
                } else {
                    move(getPath(NextToChest(target)));
                }
            }

            void getItem(Node target) {
                if (equal(myPos, target)) {
                    pickupItem();
                } else {
                    move(getPath(target));
                }
            }

            void pickupItem() {
                try {
                    hero.pickupItem();
                } catch (Exception ignored) {
                }
            }

            void attack(String x) {
                try {
                    hero.attack(x);
                } catch (Exception ignored) {

                }
            }

            void shoot(String x) {
                try {
                    hero.shoot(x);
                } catch (Exception ignored) {
                }
            }

            void move(String x) {
                try {
                    hero.move(x);
                } catch (Exception ignored) {
                }
            }

            void useItem(String x) {
                try {
                    hero.useItem(x);
                } catch (Exception ignored) {
                }
            }

            void throwAttack(String x) {
                try {
                    hero.throwItem(x);
                } catch (Exception ignored) {
                }
            }

            void revokeItem(String x) {
                try {
                    hero.revokeItem(x);
                } catch (Exception ignored) {
                }
            }

            int getPointArmor(Armor armor) {
                if (armor == null)
                    return 0;
                List<Armor> listArmors = hero.getInventory().getListArmor();
                for (Armor currentArmor : listArmors) {
                    String vest = "VEST";
                    if (currentArmor.getId().equals(armor.getId())
                            || (currentArmor.getId().equals(vest) && armor.getId().equals(vest))) {
                        return 0;
                    }
                }
                return armor.getDamageReduce() * 300 / Math.max(distance(armor), 1);
            }

            int getPointHealth(HealingItem health) {
                if (health == null)
                    return 0;
                return ((100 - me.getHp()) * 2 + health.getHealingHP() / 2) * 100 / Math.max(distance(health), 1);
            }

            int getPointPlayer(Node player) {
                int minDistance = 99999999;
                for (Node p : DIRECTIONS2) {
                    Node addNode = add(player, p);
                    if (distance(addNode) < minDistance) {
                        minDistance = distance(addNode);
                    }
                }
                return 100 * 100 / Math.max(minDistance, 1);
            }

            void getWeapon(Node weapon, int type) {
                if (equal(weapon, myPos)) {
                    if (type == 4) {
                        if (haveMelee) {
                            System.out.println("bo melee");
                            revokeItem(melee.getId());
                            return;
                        }
                    }
                    if (type == 5) {
                        if (haveGun) {
                            System.out.println("bo gun");
                            revokeItem(gun.getId());
                            return;
                        }
                    }
                }
                getItem(weapon);
            }

            int getPointWeapon(Weapon weapon) {
                if (weapon == null)
                    return 0;
                int pointWeapon = 0;
                int meleeDame = 0;
                int gunDame = 0;

                if (haveMelee) {
                    meleeDame = melee.getDamage();
                }
                if (haveGun) {
                    gunDame = gun.getDamage();
                }
                if (weapon.getType() == ElementType.THROWABLE) {
                    if (!haveThrow) {
                        pointWeapon = weapon.getDamage();
                    }
                }
                if (weapon.getType() == ElementType.MELEE) {
                    pointWeapon = weapon.getDamage() - meleeDame;
                }
                if (weapon.getType() == ElementType.GUN) {
                    pointWeapon = weapon.getDamage() - gunDame;
                }
                return pointWeapon * 100 / Math.max(distance(weapon), 1);
            }

            int getPointChest(Node chest) {
                if (chest == null)
                    return 0;
                int pointChest = 0;
                if (me.getDamageReduction() < 20) {
                    pointChest += 20 * 3 * 4 * 2;
                }
                if (me.getDamageReduction() == 20 || me.getDamageReduction() == 0) {
                    pointChest += 5 * 3 * 4 * 5 + 10 * 3 * 4 * 3;
                }
                pointChest += (100 - me.getHp()) * 200;
                int meleeDame = 0;
                if (haveMelee) {
                    meleeDame = melee.getDamage();
                }
                if (meleeDame == 45) {
                    pointChest += 55 * 2 * 4 * 4;
                }
                if (meleeDame == 0) {
                    pointChest += 45 * 2 * 4 * 16;
                }
                if (!haveThrow) {
                    pointChest += 25 * 1 * 4 * 40;
                }
                return pointChest / Math.max(distance(NextToChest(chest)), 1);
            }

            void calculateOptimizedMove() {
                ;
                List<Node> targetNode = new ArrayList<>();
                for (Node p : otherPlayers) {
                    for (int i = 0; i < 4; ++i) {
                        targetNode.add(add(p, DIRECTIONS2.get(i)));
                    }
                }
                Obstacle nearestChest = nearestNode(gameMap.getListChests(), true);
                Node nearestPlayer = nearestNode(targetNode, false);
                HealingItem nearestHealth = nearestNode(gameMap.getListHealingItems(), false);
                Armor nearestArmor = nearestNode(gameMap.getListArmors(), false);
                Weapon nearestMelee = nearestNode(gameMap.getAllMelee(), false);
                Weapon nearestGun = nearestNode(gameMap.getAllGun(), false);
                Weapon nearestThrow = nearestNode(gameMap.getAllThrowable(), false);
                List<Integer> pointItems = new ArrayList<>();
                pointItems.add(getPointChest(nearestChest));
                pointItems.add(getPointPlayer(nearestPlayer));
                pointItems.add(getPointHealth(nearestHealth));
                pointItems.add(getPointArmor(nearestArmor));
                pointItems.add(getPointWeapon(nearestMelee));
                pointItems.add(getPointWeapon(nearestGun));
                pointItems.add(getPointWeapon(nearestThrow));
                List<Node> target = new ArrayList<>();
                target.add(nearestChest);
                target.add(nearestPlayer);
                target.add(nearestHealth);
                target.add(nearestArmor);
                target.add(nearestMelee);
                target.add(nearestGun);
                target.add(nearestThrow);
                for (int i = 0; i < 7; ++i) {
                    System.out.println(pointItems.get(i));
                }
                boolean danger = false;
                for (Node p : restrictedNodes) {
                    if (equal(p, myPos)) {
                        danger = true;
                        break;
                    }
                }
                if (!danger) {
                    List<HealingItem> myItems = myInventory.getListHealingItem();
                    for (HealingItem item : myItems) {
                        if (me.getHp() < 100 && distance2(nearestPlayer, myPos) >= 4 + item.getUsageTime()) {
                            useItem(item.getId());
                            return;
                        }
                    }
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
                            move(getPath(nearestPlayer));
                        }
                        if (i >= 2 && i <= 3) {
                            getItem(target.get(i));
                        }
                        if (i >= 4) {
                            getWeapon(target.get(i), i);
                        }
                        return;
                    }
                }
            }

            @Override
            public void call(Object... args) {
                gameMap = hero.getGameMap(); // map
                gameMap.updateOnUpdateMap(args[0]);
                init();
                bfs();

                meleeCooldown -= 1;
                gunCooldown -= 1;
                System.out.println("Vi tri hien tai " + myPos.getX() + " " + myPos.getY());
                System.out.println("Co sung " + haveGun);
                System.out.println("Co dao " + haveMelee);
                System.out.println("Co nem " + haveThrow);
                System.out.println("gunCooldown " + gunCooldown);
                System.out.println("meleeCooldown " + meleeCooldown);
                if (meleeCooldown <= 0 && haveMelee) {
                    for (Node p : otherPlayers) {
                        if (distance2(myPos, p) == 1) {
                            mAttack(p);
                            meleeCooldown = melee.getCooldown();
                            return;
                        }
                    }
                }
                if (gunCooldown <= 0 && haveGun) {
                    for (Node p : otherPlayers) {
                        if (Math.abs(p.x - myPos.x) == 0 && Math.abs(p.y - myPos.y) <= gun.getRange()) {
                            if (p.y < myPos.getY()) {
                                shoot("d");
                            } else {
                                shoot("u");
                            }
                        }
                        if (Math.abs(p.y - myPos.y) == 0 && Math.abs(p.x - myPos.x) <= gun.getRange()) {
                            if (p.x < myPos.getX()) {
                                shoot("l");
                            } else {
                                shoot("r");
                            }
                        }
                        gunCooldown = gun.getCooldown();
                        return;
                    }
                }
                if (haveThrow) {
                    List<Node> targetThrow = new ArrayList<>();
                    for (int i = 0; i < 4; ++i) {
                        targetThrow.add(add(diffNodeThrow.get(i), myPos));
                    }
                    for (Node p : otherPlayers) {
                        for (int i = 0; i < 4; ++i) {
                            if (distance2(targetThrow.get(i), p) <= 1) {
                                throwAttack(DIRECTIONS_STR.get(i));
                                return;
                            }
                        }
                    }
                }
                if ((gunCooldown <= 1 && haveGun) || (meleeCooldown <= 1 && haveMelee)) {
                    for (Node p : otherPlayers) {
                        if (Math.abs(p.x - myPos.x) == 1 && Math.abs(p.y - myPos.y) == 1) {
                            move(PathUtils.getShortestPath(gameMap, restrictedNodesWithoutPlayers, myPos, p, false));
                            return;
                        }
                    }
                }
                calculateOptimizedMove();
                System.out.println("===================");
            }
        };

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}
