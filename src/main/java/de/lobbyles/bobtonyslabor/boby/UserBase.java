package de.lobbyles.bobtonyslabor.boby;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Predicate;

@Getter
public final class UserBase {
    private static LinkedList<User> users = new LinkedList<>();

    public static void addUser(User user) {
        users.add(user);
    }

    public static void addUser(Player player) {
        users.add(new User(player));
    }

    public static void removeUser(User user) {
        users.remove(user);
    }

    public static void removeUser(Player player) {
        users.remove(getUser(player));
    }

    public static User getUser(Player p) {
        for (User user : users) {
            if (user.getPlayer().equals(p)) {
                return user;
            }
        }
        return null;
    }

    public static User getUser(UUID uuid) {
        for (User user : users) {
            if (user.getPlayer().getUniqueId().equals(uuid)) {
                return user;
            }
        }
        return null;
    }

    public static User getUser(String name) {
        for (User user : users) {
            if (user.getPlayer().getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    public User getUser(Predicate<LinkedList<User>> criteria) {
        for (User user : users) {
            if (criteria.test(users)) {
                return user;
            }
        }
        return null;
    }

    public LinkedList<User> getAllUser(Predicate<LinkedList<User>> criteria) {
        LinkedList<User> result = new LinkedList<>();
        for (User user : users) {
            if (criteria.test(users)) {
                result.add(user);
            }
        }
        return result.size() > 0 ? result : null;
    }

}