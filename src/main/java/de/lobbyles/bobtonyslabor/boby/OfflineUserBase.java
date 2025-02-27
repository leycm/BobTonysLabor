package de.lobbyles.bobtonyslabor.boby;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static de.lobbyles.bobtonyslabor.BobTonysLabor.PLUGIN_FOLDER;

public final class OfflineUserBase {
    private static LinkedList<OfflineUser> offlineUsers = new LinkedList<>();
    private static final Logger LOGGER = Bukkit.getLogger();

    public static void addOfflineUser(OfflineUser user) {
        offlineUsers.add(user);
    }

    public static void addOfflineUser(OfflinePlayer player) {
        offlineUsers.add(new OfflineUser(player));
    }

    public static void removeOfflineUser(OfflineUser user) {
        offlineUsers.remove(user);
    }

    public static void removeOfflineUser(OfflinePlayer player) {
        offlineUsers.remove(getOfflineUser(player));
    }

    public static OfflineUser getOfflineUser(OfflinePlayer p) {
        for (OfflineUser user : offlineUsers) {
            if (user.getOfflinePlayer().equals(p)) {
                return user;
            }
        }
        return null;
    }

    public static OfflineUser getOfflineUser(UUID uuid) {
        for (OfflineUser user : offlineUsers) {
            if (user.getOfflinePlayer().getUniqueId().equals(uuid)) {
                return user;
            }
        }
        return null;
    }

    public static OfflineUser getOfflineUser(String name) {
        for (OfflineUser user : offlineUsers) {
            if (user.getOfflinePlayer().getName() != null &&
                    user.getOfflinePlayer().getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    public static OfflineUser getOfflineUser(Predicate<OfflineUser> criteria) {
        for (OfflineUser user : offlineUsers) {
            if (criteria.test(user)) {
                return user;
            }
        }
        return null;
    }

    public static LinkedList<OfflineUser> getAllOfflineUsers(Predicate<OfflineUser> criteria) {
        LinkedList<OfflineUser> result = new LinkedList<>();
        for (OfflineUser user : offlineUsers) {
            if (criteria.test(user)) {
                result.add(user);
            }
        }
        return result.size() > 0 ? result : null;
    }

    public static List<OfflineUser> loadAllPlayers() {
        LOGGER.info("Loading all player data files...");
        offlineUsers.clear();

        File playerDataFolder = new File(PLUGIN_FOLDER + "/playerdata");
        if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
            LOGGER.warning("Player data folder does not exist");
            return new ArrayList<>();
        }

        File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null || playerFiles.length == 0) {
            LOGGER.info("No player data files found");
            return new ArrayList<>();
        }

        int loadedCount = 0;
        for (File file : playerFiles) {
            try {
                String fileName = file.getName();
                String uuidStr = fileName.substring(0, fileName.length() - 4);
                UUID uuid = UUID.fromString(uuidStr);

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                OfflineUser offlineUser = new OfflineUser(offlinePlayer);
                offlineUsers.add(offlineUser);
                loadedCount++;
            } catch (Exception e) {
                LOGGER.warning("Failed to load player data from file: " + file.getName() + " - " + e.getMessage());
            }
        }

        LOGGER.info("Loaded " + loadedCount + " player data files");
        return new ArrayList<>(offlineUsers);
    }
    public static OfflineUser convertToOfflineUser(User user) {
        if (user == null) return null;
        OfflineUser offlineUser = new OfflineUser(user.getPlayer());
        return offlineUser;
    }

    public static User convertToOnlineUser(OfflineUser offlineUser) {
        if (offlineUser == null) return null;

        Player onlinePlayer = Bukkit.getPlayer(offlineUser.getOfflinePlayer().getUniqueId());
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return UserBase.getUser(onlinePlayer);
        }
        return null;
    }

    public static void saveAllOfflineUsers() {
        LOGGER.info("Saving all offline user data...");
        int savedCount = 0;

        for (OfflineUser user : offlineUsers) {
            try {
                user.save();
                savedCount++;
            } catch (Exception e) {
                LOGGER.warning("Failed to save offline user data for: " +
                        user.getOfflinePlayer().getName() + " - " + e.getMessage());
            }
        }

        LOGGER.info("Saved " + savedCount + " offline user data files");
    }
}