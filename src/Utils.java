import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jsclub.codefest2024.sdk.model.weapon.*;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;

public class Utils {
    static <T> List<List<T>> initializeList(int size, T value) {
        List<List<T>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new ArrayList<>(Collections.nCopies(size, value)));
        }
        return result;
    }

    static <T extends Node> boolean equal(T x, T y) {
        return x.getX() == y.getX() && x.getY() == y.getY();
    }

    static Node add(Node x, Node y) {
        return new Node(x.getX() + y.getX(), x.getY() + y.getY());
    }

    static boolean isValid(Node p, GameMap gameMap) {
        int mapSize = gameMap.getMapSize();
        return p.getX() >= 0 && p.getX() < mapSize && p.getY() >= 0 && p.getY() < mapSize;
    }

    static int distance(Node p1, Node p2, GameMap gameMap) {
        if (p1 == null || p2 == null || !isInsideSafeArea(p1, gameMap) || !isInsideSafeArea(p2, gameMap)) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY());
    }

    static boolean isInsideSafeArea(Node p, GameMap gameMap) {
        return PathUtils.checkInsideSafeArea(p, gameMap.getDarkAreaSize(), gameMap.getMapSize());
    }

    static int getDame(Weapon weapon) {
        if (weapon == null)
            return 0;
        return weapon.getDamage();
    }

    static int stepToKill(Weapon gun, Weapon melee, int health) {
        if (gun == null && melee == null)
            return Integer.MAX_VALUE;
        if (gun == null)
            return (health + melee.getDamage() - 1) / melee.getDamage() * melee.getCooldown();
        if (melee == null)
            return (health + gun.getDamage() - 1) / gun.getDamage() * gun.getCooldown();
        int diffCooldown = melee.getCooldown() - gun.getCooldown();
        health -= melee.getDamage();
        if (health <= 0)
            return 1;
        health -= gun.getDamage();
        if (health <= 0)
            return 2;
        health -= gun.getDamage();
        if (health <= 0)
            return 2 + gun.getCooldown();
        health -= melee.getDamage();
        return 1 + gun.getCooldown() + diffCooldown;
    }
}