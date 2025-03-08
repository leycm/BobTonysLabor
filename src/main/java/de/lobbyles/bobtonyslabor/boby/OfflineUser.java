package de.lobbyles.bobtonyslabor.boby;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static de.lobbyles.bobtonyslabor.BobTonysLabor.PLUGIN_FOLDER;

@Getter
@Setter
public class OfflineUser {
    private final OfflinePlayer offlinePlayer;
    private FileConfiguration playerFile;
    private HashMap<String, User.PlayerLoginInfo> loginHistory;
    private TonyMode mode;
    private String lastKnownLocation;
    private String lastISP;
    private String operatingSystem;
    private String clientVersion;
    private LocalDateTime firstJoin;
    private LocalDateTime lastJoin;
    private String nickname;
    private @NotNull List<String> nameTagPatterns;

    private static final Logger LOGGER = Bukkit.getLogger();

    public OfflineUser(OfflinePlayer offlinePlayer) {
        this.offlinePlayer = offlinePlayer;
        this.mode = TonyMode.MEMBER;
        this.loginHistory = new HashMap<>();
        this.nameTagPatterns = new ArrayList<>();
        this.playerFile = loadConfig(offlinePlayer.getUniqueId().toString());

        File file = new File(PLUGIN_FOLDER + "/playerdata", offlinePlayer.getUniqueId() + ".yml");
        if (file.exists()) {
            reload();
        } else {
            this.firstJoin = LocalDateTime.now();
            this.lastJoin = LocalDateTime.now();
            this.nickname = offlinePlayer.getName();
            this.nameTagPatterns.add("%playername%");
            save();
        }
    }

    public void reload() {
        playerFile = loadConfig(offlinePlayer.getUniqueId().toString());
        if (playerFile == null) return;

        String modeStr = playerFile.getString("player.mode", "MEMBER");
        mode = TonyMode.fromString(modeStr);

        nickname = playerFile.getString("player.nickname", offlinePlayer.getName());

        nameTagPatterns = playerFile.getStringList("player.nameTagPatterns");
        if (nameTagPatterns.isEmpty()) {
            nameTagPatterns.add("%playername%");
        }

        loginHistory.clear();
        ConfigurationSection loginSection = playerFile.getConfigurationSection("user.logins");
        if (loginSection != null) {
            for (String timeKey : loginSection.getKeys(false)) {
                ConfigurationSection infoSection = loginSection.getConfigurationSection(timeKey);
                if (infoSection != null) {
                    String ip = infoSection.getString("ip", "Unknown");
                    String location = infoSection.getString("location", "Unknown");
                    String isp = infoSection.getString("isp", "Unknown");
                    String os = infoSection.getString("os", "Unknown");
                    String client = infoSection.getString("client", "Unknown");

                    LocalDateTime timestamp;
                    try {
                        timestamp = LocalDateTime.parse(infoSection.getString("timestamp", LocalDateTime.now().toString()));
                    } catch (Exception e) {
                        timestamp = LocalDateTime.now();
                    }

                    User.PlayerLoginInfo info = new User.PlayerLoginInfo(ip, location, isp, os, client, timestamp);
                    loginHistory.put(timeKey, info);
                }
            }
        }

        lastKnownLocation = playerFile.getString("user.lastLocation", "Unknown");
        lastISP = playerFile.getString("user.lastISP", "Unknown");
        operatingSystem = playerFile.getString("user.operatingSystem", "Unknown");
        clientVersion = playerFile.getString("user.clientVersion", "Unknown");

        String firstJoinStr = playerFile.getString("user.firstJoin");
        String lastJoinStr = playerFile.getString("user.lastJoin");

        try {
            if (firstJoinStr != null) {
                firstJoin = LocalDateTime.parse(firstJoinStr);
            } else {
                firstJoin = LocalDateTime.now();
            }

            if (lastJoinStr != null) {
                lastJoin = LocalDateTime.parse(lastJoinStr);
            } else {
                lastJoin = LocalDateTime.now();
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing dates for player " +
                    (offlinePlayer.getName() != null ? offlinePlayer.getName() : offlinePlayer.getUniqueId()) +
                    ": " + e.getMessage());
            firstJoin = LocalDateTime.now();
            lastJoin = LocalDateTime.now();
        }
    }

    public void save() {
        if (playerFile == null) return;

        playerFile.set("player.name", offlinePlayer.getName());
        playerFile.set("player.uuid", offlinePlayer.getUniqueId().toString());
        if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            playerFile.set("player.playtime", offlinePlayer.getPlayer().getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE));
        }
        playerFile.set("player.mode", mode.toString());
        playerFile.set("player.nickname", nickname);
        playerFile.set("player.nameTagPatterns", nameTagPatterns);

        playerFile.set("user.lastLocation", lastKnownLocation);
        playerFile.set("user.lastISP", lastISP);
        playerFile.set("user.operatingSystem", operatingSystem);
        playerFile.set("user.clientVersion", clientVersion);
        playerFile.set("user.firstJoin", firstJoin.toString());
        playerFile.set("user.lastJoin", lastJoin.toString());

        playerFile.set("user.logins", null);
        for (Map.Entry<String, User.PlayerLoginInfo> entry : loginHistory.entrySet()) {
            String timeKey = entry.getKey();
            User.PlayerLoginInfo info = entry.getValue();

            playerFile.set("user.logins." + timeKey + ".ip", info.getIp());
            playerFile.set("user.logins." + timeKey + ".location", info.getLocation());
            playerFile.set("user.logins." + timeKey + ".isp", info.getIsp());
            playerFile.set("user.logins." + timeKey + ".os", info.getOperatingSystem());
            playerFile.set("user.logins." + timeKey + ".client", info.getClientVersion());
            playerFile.set("user.logins." + timeKey + ".timestamp", info.getTimestamp().toString());
        }

        saveConfig(playerFile, offlinePlayer.getUniqueId().toString());
    }

    private FileConfiguration createConfig(String title) {
        File folder = new File(PLUGIN_FOLDER + "/playerdata");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, title + ".yml");
        try {
            if (file.createNewFile()) {
                return YamlConfiguration.loadConfiguration(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private FileConfiguration loadConfig(String title) {
        File file = new File(PLUGIN_FOLDER + "/playerdata", title + ".yml");
        return file.exists() ? YamlConfiguration.loadConfiguration(file) : createConfig(title);
    }

    private void saveConfig(FileConfiguration config, String title) {
        File file = new File(PLUGIN_FOLDER + "/playerdata", title + ".yml");
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User.PlayerLoginInfo getLastLoginInfo() {
        if (loginHistory.isEmpty()) {
            return null;
        }

        User.PlayerLoginInfo latestInfo = null;
        LocalDateTime latestTime = LocalDateTime.MIN;

        for (User.PlayerLoginInfo info : loginHistory.values()) {
            if (info.getTimestamp().isAfter(latestTime)) {
                latestTime = info.getTimestamp();
                latestInfo = info;
            }
        }

        return latestInfo;
    }

    public boolean isOnline() {
        return offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null;
    }

    public User toOnlineUser() {
        if (isOnline()) {
            return UserBase.getUser(offlinePlayer.getPlayer());
        }
        return null;
    }
}