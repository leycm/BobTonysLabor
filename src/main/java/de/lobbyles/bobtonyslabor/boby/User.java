package de.lobbyles.bobtonyslabor.boby;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static de.lobbyles.bobtonyslabor.BobTonysLabor.*;

@Getter
@Setter
public class User implements Listener {
    private final Player player;
    private NameTag nameTag;
    private FileConfiguration playerFile;
    private HashMap<String, PlayerLoginInfo> loginHistory;
    private TonyMode mode;
    private String lastKnownLocation;
    private String lastISP;
    private String operatingSystem;
    private int ping;
    private String clientVersion;
    private LocalDateTime firstJoin;
    private LocalDateTime lastJoin;
    private String nickname;
    private List<String> nameTagPatterns;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Logger LOGGER = Bukkit.getLogger();

    public User(Player player) {
        this.player = player;
        this.mode = TonyMode.MEMBER;
        this.loginHistory = new HashMap<>();
        this.playerFile = loadConfig(player.getUniqueId().toString());
        this.nameTagPatterns = new ArrayList<>();

        File file = new File(PLUGIN_FOLDER + "/playerdata", player.getUniqueId() + ".yml");
        if (file.exists()) {
            reload();
        } else {
            this.firstJoin = LocalDateTime.now();
            this.lastJoin = LocalDateTime.now();
            this.nickname = player.getName();
            this.nameTagPatterns.add("%playername%");
            save();
        }

        this.nameTag = new NameTag(nameTagPatterns, this);
        spawnNameTags();
        reloadTabName();
    }

    @Getter
    @Setter
    public static class PlayerLoginInfo {
        private String ip;
        private String location;
        private String isp;
        private String operatingSystem;
        private String clientVersion;
        private LocalDateTime timestamp;

        public PlayerLoginInfo(String ip, String location, String isp, String os, String clientVersion) {
            this.ip = ip;
            this.location = location;
            this.isp = isp;
            this.operatingSystem = os;
            this.clientVersion = clientVersion;
            this.timestamp = LocalDateTime.now();
        }

        public PlayerLoginInfo(String ip, String location, String isp, String os, String clientVersion, LocalDateTime timestamp) {
            this.ip = ip;
            this.location = location;
            this.isp = isp;
            this.operatingSystem = os;
            this.clientVersion = clientVersion;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "IP: " + ip +
                    ", Location: " + location +
                    ", ISP: " + isp +
                    ", OS: " + operatingSystem +
                    ", Client: " + clientVersion;
        }
    }

    public void killNameTags(){nameTag.killNameTag();}
    public void spawnNameTags(){nameTag = new NameTag(nameTagPatterns, this); nameTag.spawnNameTag(player);}
    public void reloadNameTags(){nameTag.reloadNameTag(player);}
    public void setNameTags(List<String> lines) {killNameTags();(nameTag = new NameTag(lines,this)).reloadNameTag(player);}

    public void reload() {
        playerFile = loadConfig(player.getUniqueId().toString());
        if (playerFile == null) return;

        String modeStr = playerFile.getString("player.mode", "MEMBER");
        mode = TonyMode.fromString(modeStr);

        nickname = playerFile.getString("player.nickname", player.getName());

        nameTagPatterns = playerFile.getStringList("player.nameTagPatterns");

        if (nameTagPatterns.isEmpty()) {
            nameTagPatterns.add("%playername%");
        }

        nameTag = new NameTag(nameTagPatterns, this);

        setNameTags(nameTagPatterns);

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

                    PlayerLoginInfo info = new PlayerLoginInfo(ip, location, isp, os, client, timestamp);
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
            LOGGER.warning("Error parsing dates for player " + player.getName() + ": " + e.getMessage());
            firstJoin = LocalDateTime.now();
            lastJoin = LocalDateTime.now();
        }

        setNameTags(nameTagPatterns);
    }

    public void save() {
        if (playerFile == null) return;

        playerFile.set("player.name", player.getName());
        playerFile.set("player.uuid", player.getUniqueId().toString());
        playerFile.set("player.playtime", player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE));
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
        Map<PlayerLoginInfo, String> anchorMap = new HashMap<>();

        for (Map.Entry<String, PlayerLoginInfo> entry : loginHistory.entrySet()) {
            String timeKey = entry.getKey();
            PlayerLoginInfo info = entry.getValue();

            if (anchorMap.containsKey(info)) {
                String anchorKey = anchorMap.get(info);
                playerFile.set("user.logins." + timeKey + ".<<", "*" + anchorKey);
                playerFile.set("user.logins." + timeKey + ".timestamp", info.getTimestamp().toString());
            } else {
                String anchorKey = "login_" + timeKey.replace(":", "_").replace("-", "_");
                anchorMap.put(info, anchorKey);

                playerFile.set("user.logins." + timeKey + ".&" + anchorKey, null);
                playerFile.set("user.logins." + timeKey + ".ip", info.getIp());
                playerFile.set("user.logins." + timeKey + ".location", info.getLocation());
                playerFile.set("user.logins." + timeKey + ".isp", info.getIsp());
                playerFile.set("user.logins." + timeKey + ".os", info.getOperatingSystem());
                playerFile.set("user.logins." + timeKey + ".client", info.getClientVersion());
                playerFile.set("user.logins." + timeKey + ".timestamp", info.getTimestamp().toString());
            }
        }

        saveConfig(playerFile, player.getUniqueId().toString());
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

    public void setNickname(String nickname) {
        player.setDisplayName(player.getDisplayName().replace(player.getName(),nickname).replace(this.nickname,nickname));
        player.setPlayerListName(player.getDisplayName());
        this.nickname = nickname;
        save();
    }

    public void setNameTagPatterns(List<String> patterns) {
        this.nameTagPatterns = patterns;
        setNameTags(nameTagPatterns);
        save();
    }

    public void reloadTabName(){
        player.setPlayerListName(nickname);
    }

    public String getPlayerPrefix() {
        net.luckperms.api.model.user.User lkuser = luckperms.getUserManager().getUser(player.getUniqueId());
        if (lkuser != null) {
            String prefix = lkuser.getCachedData().getMetaData().getPrefix();
            return prefix != null ? prefix : "Kein Prefix";
        }
        return "Kein User";
    }

    public void kick(String reason, String kickedBy) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String time = now.format(formatter);

        String[] reasonLines = splitReason(reason, 32);

        Component kickMessage = Component.text()
                .append(Component.text("BobLabor\n", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.text("--------------------------------\n", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                .append(Component.text("Du wurdest gekickt!\n", NamedTextColor.DARK_GRAY))
                .append(Component.text(reasonLines[0] + "\n", NamedTextColor.WHITE)) // Erste Zeile des Grundes
                .append(reasonLines.length > 1 ? Component.text(reasonLines[1] + "\n", NamedTextColor.WHITE) : Component.empty()) // Zweite Zeile (falls vorhanden)
                .append(Component.text("--------------------------------\n", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                .append(Component.text(kickedBy + " - " + time + "\n", NamedTextColor.DARK_AQUA))
                .build();

        player.kick(kickMessage);
    }

    private String[] splitReason(String reason, int maxLineLength) {
        if (reason.length() <= maxLineLength) {return new String[]{reason};}
        int splitIndex = reason.lastIndexOf(' ', maxLineLength);
        if (splitIndex <= 0) {splitIndex = maxLineLength;}
        String line1 = reason.substring(0, splitIndex).trim();
        String line2 = reason.substring(splitIndex).trim();
        return new String[]{line1, line2};
    }
    
}
