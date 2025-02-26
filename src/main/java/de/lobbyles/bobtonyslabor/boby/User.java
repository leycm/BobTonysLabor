package de.lobbyles.bobtonyslabor.boby;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static de.lobbyles.bobtonyslabor.BobTonysLabor.PLUGIN_FOLDER;
import static de.lobbyles.bobtonyslabor.BobTonysLabor.plugin;

@Getter
@Setter
public class User implements Listener {
    private final Player player;
    private FileConfiguration playerFile;
    private HashMap<String, PlayerLoginInfo> loginHistory; // Changed to store login info objects
    private TonyMode mode;
    private String lastKnownLocation;
    private String lastISP;
    private String operatingSystem;
    private int ping;
    private String clientVersion;
    private LocalDateTime firstJoin;
    private LocalDateTime lastJoin;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Logger LOGGER = Bukkit.getLogger();

    // Inner class to store login information
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

        // For loading from config
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

    public User(Player player) {
        this.player = player;
        this.mode = TonyMode.MEMBER;
        this.loginHistory = new HashMap<>();
        this.playerFile = loadConfig(player.getUniqueId().toString());

        File file = new File(PLUGIN_FOLDER + "/playerdata", player.getUniqueId() + ".yml");
        if (file.exists()) {
            reload();
        } else {
            this.firstJoin = LocalDateTime.now();
            this.lastJoin = LocalDateTime.now();
            save();
        }
    }

    public void reload() {
        playerFile = loadConfig(player.getUniqueId().toString());
        if (playerFile == null) return;

        // Load player mode
        String modeStr = playerFile.getString("player.mode", "MEMBER");
        mode = TonyMode.fromString(modeStr);

        // Load login history
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

        // Load other user data
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
    }

    public void save() {
        if (playerFile == null) return;

        // Save player data
        playerFile.set("player.name", player.getName());
        playerFile.set("player.uuid", player.getUniqueId().toString());
        playerFile.set("player.playtime", player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE));
        playerFile.set("player.mode", mode.toString());

        playerFile.set("user.lastLocation", lastKnownLocation);
        playerFile.set("user.lastISP", lastISP);
        playerFile.set("user.operatingSystem", operatingSystem);
        playerFile.set("user.clientVersion", clientVersion);
        playerFile.set("user.firstJoin", firstJoin.toString());
        playerFile.set("user.lastJoin", lastJoin.toString());

        playerFile.set("user.logins", null);
        for (Map.Entry<String, PlayerLoginInfo> entry : loginHistory.entrySet()) {
            String timeKey = entry.getKey();
            PlayerLoginInfo info = entry.getValue();

            playerFile.set("user.logins." + timeKey + ".ip", info.getIp());
            playerFile.set("user.logins." + timeKey + ".location", info.getLocation());
            playerFile.set("user.logins." + timeKey + ".isp", info.getIsp());
            playerFile.set("user.logins." + timeKey + ".os", info.getOperatingSystem());
            playerFile.set("user.logins." + timeKey + ".client", info.getClientVersion());
            playerFile.set("user.logins." + timeKey + ".timestamp", info.getTimestamp().toString());
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

    public static class UserListener implements Listener {

        public UserListener() {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }

        @EventHandler
        public void onUserJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            User user = new User(player);

            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "Unknown";

            String locationInfo = "Unknown";
            String ispInfo = "Unknown";

            if (isLocalhost(ip)) {
                locationInfo = "Development Environment";
                ispInfo = "Local Network";
            } else {
                try {
                    // Fetch location data
                    String locationResponse = fetchLocationData(ip);
                    JSONParser parser = new JSONParser();
                    JSONObject locationJson = (JSONObject) parser.parse(locationResponse);

                    if ("success".equals(locationJson.get("status"))) {
                        String country = (String) locationJson.get("country");
                        String city = (String) locationJson.get("city");
                        String region = (String) locationJson.get("regionName");
                        locationInfo = city + ", " + region + ", " + country;
                        ispInfo = (String) locationJson.get("isp");
                    } else {
                        locationInfo = "Location lookup failed: " + locationJson.get("message");
                        ispInfo = "ISP lookup failed: " + locationJson.get("message");
                    }
                } catch (Exception e) {
                    locationInfo = "Error: " + e.getMessage();
                    ispInfo = "Error: " + e.getMessage();
                    e.printStackTrace();
                }
            }

            String osInfo = System.getProperty("os.name");
            String clientInfo = player.getClientBrandName() != null ? player.getClientBrandName() : "Unknown";

            String timeKey = LocalTime.now().format(TIME_FORMATTER);

            PlayerLoginInfo loginInfo = new PlayerLoginInfo(ip, locationInfo, ispInfo, osInfo, clientInfo);
            user.getLoginHistory().put(timeKey, loginInfo);

            // Update user properties
            user.setLastKnownLocation(locationInfo);
            user.setLastISP(ispInfo);
            user.setOperatingSystem(osInfo);
            user.setClientVersion(clientInfo);
            user.setPing(player.getPing());
            user.setLastJoin(LocalDateTime.now());

            logPlayerLogin(player, loginInfo);

            UserBase.addUser(user);
            user.save();
        }

        private void logPlayerLogin(Player player, PlayerLoginInfo info) {
            LOGGER.info("=== Player Login: " + player.getName() + " ===");
            LOGGER.info("IP: " + info.getIp());
            LOGGER.info("Location: " + info.getLocation());
            LOGGER.info("ISP: " + info.getIsp());
            LOGGER.info("OS: " + info.getOperatingSystem());
            LOGGER.info("Client: " + info.getClientVersion());
            LOGGER.info("================================");
        }

        private boolean isLocalhost(String ip) {
            return ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("localhost");
        }

        @EventHandler
        public void onUserQuit(PlayerQuitEvent event) {
            User user = UserBase.getUser(event.getPlayer());
            if (user != null) {
                user.save();
                UserBase.removeUser(user);
            }
        }
    }

    private static String fetchLocationData(String ip) {
        if (isLocalhost(ip)) {
            return "{\"status\":\"success\",\"country\":\"Local\",\"countryCode\":\"LH\",\"region\":\"Local\",\"regionName\":\"Development\",\"city\":\"Development\",\"zip\":\"00000\",\"lat\":0,\"lon\":0,\"timezone\":\"Local\",\"isp\":\"Local Network\",\"org\":\"Development\",\"as\":\"\",\"query\":\"" + ip + "\"}";
        }

        try {
            URL url = new URL("http://ip-api.com/json/" + ip + "?fields=status,message,country,countryCode,region,regionName,city,zip,lat,lon,timezone,isp,org,as,query");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                return "{\"status\":\"fail\",\"message\":\"Connection error: " + status + "\",\"query\":\"" + ip + "\"}";
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return "{\"status\":\"fail\",\"message\":\"Error: " + e.getMessage() + "\",\"query\":\"" + ip + "\"}";
        }
    }

    private static boolean isLocalhost(String ip) {
        return ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("localhost");
    }
}