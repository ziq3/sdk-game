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
import java.io.IOException;
import java.util.*;

public class Main {
    private static final String SERVER_URL = "https://cf-server.jsclub.dev";
    private static final String GAME_ID = "105904";
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

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME); // Our hero
        Emitter.Listener onMapUpdate = new Emitter.Listener() {
            ArrayList<ArrayList<Integer>> g = new ArrayList<>();
            ArrayList<ArrayList<Integer>> trace = new ArrayList<>();
            GameMap gameMap = null;
            Node myPos = null;

            <T extends Node> Boolean equal(T x, T y) {

                return x.getX() == y.getX() && x.getY() == y.getY();
            }

            void bfs() {
                int[] dx = { 0, 0, 1, -1 };
                int[] dy = { 1, -1, 0, 0 };
                int mapSize = gameMap.getMapSize();
                int darkAreaSize = gameMap.getDarkAreaSize();

                List<Node> restrictedNodes = new ArrayList<>();

                List<Obstacle> listConstruct = gameMap.getListTraps();
                listConstruct.addAll(gameMap.getListIndestructibleObstacles());
                listConstruct.addAll(gameMap.getListChests());
                for (Node p : listConstruct) {
                    restrictedNodes.add(new Node(p.getX(), p.getY()));
                }

                List<Player> listPlayer = gameMap.getOtherPlayerInfo();
                for (Node p : listPlayer) {
                    if (equal(p, myPos))
                        continue;
                    for (int i = 0; i < 4; ++i) {
                        Node nearPlayer = new Node(p.getX(), p.getY());
                        // gun.range()-1
                        for (int j = 0; j < 3; ++j) {
                            nearPlayer = add(nearPlayer, DIRECTIONS.get(i));
                            restrictedNodes.add(nearPlayer);
                        }
                    }
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

            void attack(Node target) {
                try {
                    if (myPos.getX() + 1 == target.getX()) {
                        hero.attack("r");
                    }
                    if (myPos.getX() - 1 == target.getX()) {
                        hero.attack("l");
                    }
                    if (myPos.getY() + 1 == target.getY()) {
                        hero.attack("u");
                    }
                    if (myPos.getY() - 1 == target.getY()) {
                        hero.attack("d");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            void getChest(Node target) {
                try {
                    if (Math.abs(myPos.getX() - target.getX()) + Math.abs(myPos.getY() - target.getY()) == 1) {
                        attack(target);
                    } else {
                        hero.move(getPath(NextToChest(target)));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            void getItem(Node target) {
                try {
                    if (equal(myPos, target)) {
                        hero.pickupItem();
                    } else {
                        hero.move(getPath(target));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            int getGainArmor(Armor armor) {
                List<Armor> listArmors = hero.getInventory().getListArmor();
                // System.out.println(listArmors.size());
                for (Armor currentArmor : listArmors) {
                    // System.out.println(currentArmor.getId());
                    // System.out.println(armor.getId());
                    String vest = "VEST";
                    if (currentArmor.getId().equals(armor.getId())
                            || (currentArmor.getId().equals(vest) && armor.getId().equals(vest))) {
                        return 0;
                    }
                }
                return armor.getDamageReduce();
            }

            void calculateOptimizedMove(List<Node> otherPlayers, List<Node> BlocksNodes) {
                try {
                    Player me = gameMap.getCurrentPlayer();
                    Inventory myInventory = hero.getInventory();
                    List<Node> targetNode = new ArrayList<>();
                    for (Node p : otherPlayers) {
                        for (int i = 0; i < 4; ++i) {
                            targetNode.add(add(p, DIRECTIONS2.get(i)));
                        }
                    }
                    Node nearestChest = nearestNode(gameMap.getListChests(), true);
                    Node nearestPlayer = nearestNode(targetNode, false);
                    HealingItem nearestHealth = nearestNode(gameMap.getListHealingItems(), false);
                    Armor nearestArmor = nearestNode(gameMap.getListArmors(), false);

                    List<Integer> pointItems = Arrays.asList(0, 0, 0, 0);
                    int pointChest = Math.max(0, 25 - me.getDamageReduction()) + (100 - me.getHp()) * 2;
                    pointItems.set(0, pointChest * 100 / Math.max(1, distance(NextToChest(nearestChest))));
                    if (nearestHealth != null && myInventory.getListHealingItem().size() != 4) {
                        pointItems.set(1, ((100 - me.getHp()) * 2 + nearestHealth.getHealingHP() / 2) * 100);
                    }
                    if (nearestArmor != null) {
                        pointItems.set(2, getGainArmor(nearestArmor) * 300);
                    }
                    pointItems.set(3, 115 * 100);

                    List<Node> target = new ArrayList<>();
                    target.add(nearestChest);
                    target.add(nearestHealth);
                    target.add(nearestArmor);
                    target.add(nearestPlayer);

                    System.out.println("num player" + otherPlayers.size());
                    System.out.println("dis player" + distance(nearestPlayer));
                    for (int i = 1; i < 4; i++) {
                        pointItems.set(i, pointItems.get(i)
                                / Math.max(distance(target.get(i)), 1));
                    }
                    System.out.println(pointItems.get(0));
                    System.out.println(pointItems.get(1));
                    System.out.println(pointItems.get(2));
                    System.out.println(pointItems.get(3));
                    boolean danger = false;
                    boolean moved = false;
                    for (Node p : BlocksNodes) {
                        if (p.getX() == myPos.getX() && p.getY() == myPos.getY()) {
                            danger = true;
                            break;
                        }
                    }
                    if (!danger) {
                        List<HealingItem> myItems = myInventory.getListHealingItem();
                        for (HealingItem item : myItems) {
                            if (me.getHp() < 100 && distance(nearestPlayer) >= 4 + item.getUsageTime()) {
                                hero.useItem(item.getId());
                                moved = true;
                                break;
                            }
                        }
                    }
                    if (moved) {
                        return;
                    }
                    int maxPoint = Collections.max(pointItems);
                    for (int i = 0; i < 4; ++i) {
                        if (pointItems.get(i) == maxPoint) {
                            System.out.println("Move: " + i + " " + maxPoint);
                            System.out.println(
                                    "Target2: " + target.get(i).getX() + " " + target.get(i).getY());
                            if (i == 0) {
                                getChest(target.get(i));
                            }
                            if (i == 1 || i == 2) {
                                getItem(target.get(i));
                            }
                            if (i == 3) {
                                hero.move(getPath(nearestPlayer));
                            }
                            break;
                        }
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void call(Object... args) {
                try {
                    gameMap = hero.getGameMap(); // map
                    gameMap.updateOnUpdateMap(args[0]);
                    myPos = gameMap.getCurrentPlayer();
                    bfs();
                    Inventory myInventory = hero.getInventory();
                    System.out.println("Vi tri hien tai " + myPos.getX() + " " + myPos.getY());
                    List<Node> otherPlayers = new ArrayList<>(); // OtherPlayerNodes
                    {
                        List<Player> allPlayer = gameMap.getOtherPlayerInfo(); // Other Players
                        for (Player p : allPlayer) {
                            if (equal(p, myPos)) {
                                continue;
                            }
                            if (p.getIsAlive()) {
                                otherPlayers.add(new Node(p.getX(), p.getY()));
                            }
                        }
                    }
                    List<Node> BlocksNodes = new ArrayList<>(gameMap.getListTraps());
                    // BlocksNodes.addAll(gameMap.getListIndestructibleObstacles());
                    List<Enemy> ListEnemies = gameMap.getListEnemies(); // List of Enemies
                    for (Enemy E : ListEnemies) {
                        Node enemy = new Node(E.getX(), E.getY());
                        for (int i = -2; i <= 2; ++i) {
                            for (int j = -2; j <= 2; ++j) {
                                if (Math.abs(i) + Math.abs(j) <= 3) {
                                    BlocksNodes.add(add(new Node(i, j), enemy));
                                }
                            }
                        }
                    }
                    BlocksNodes.addAll(gameMap.getListChests());
                    Weapon MeleeName = myInventory.getMelee(); // Melee
                    boolean pickedUpMelee = !MeleeName.getId().equals("HAND");
                    System.out.println("Have melee?: " + pickedUpMelee);
                    if (!pickedUpMelee) {
                        Node nearestMelee = nearestNode(gameMap.getAllMelee(), false);
                        Node nearestChest = nearestNode(gameMap.getListChests(), true);
                        int distanceMelee = distance(nearestMelee);
                        int distanceChest = distance(nearestChest);
                        // System.out.println(g.get(38).get(73));
                        System.out.println(nearestChest.getX() + " " + nearestChest.getY());
                        // System.out.println(getPath(nearestChest));
                        // System.exit(0);
                        // System.out.println(distanceChest);
                        // System.out.println(nearestMelee.getX() + " " + nearestMelee.getY());
                        // System.out.println(distanceMelee);
                        if (distanceMelee < distanceChest) {
                            getItem(nearestMelee);
                        } else {
                            getChest(nearestChest);
                        }
                    } else {

                        boolean fire_or_move = false;
                        for (Node P : otherPlayers) {
                            if (Math.abs(P.getX() - myPos.getX())
                                    + Math.abs(P.getY() - myPos.getY()) == 1 && !fire_or_move) {
                                attack(P);
                                fire_or_move = true;
                            }
                        }
                        for (Node P : otherPlayers) {
                            if (Math.abs(P.getX() - myPos.getX()) == 1 && Math.abs(P.getY() - myPos.getY()) == 1
                                    && !fire_or_move) {
                                hero.move(PathUtils.getShortestPath(gameMap, BlocksNodes, myPos, P, false));
                                fire_or_move = true;
                            }
                        }
                        if (fire_or_move) {
                            System.out.println("Fire");
                        } else {
                            System.out.println("Move");
                        }
                        if (!fire_or_move) { // move
                            calculateOptimizedMove(otherPlayers, BlocksNodes);
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
