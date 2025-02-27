package de.lobbyles.bobtonyslabor.boby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static de.lobbyles.bobtonyslabor.BobTonysLabor.*;

public class UserListener implements Listener {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Logger LOGGER = Bukkit.getLogger();

    @EventHandler
    public void onUserJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        User user = new User(player);
        team.addEntry(player.getName());
        logger.info("JoinEvent");

        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof LivingEntity && entity.getScoreboardTags().contains("placeholder")) {
                player.hideEntity(plugin, entity);
            }
        }

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

        String osInfo = "Windows11, Windows10, Linux oder MacOS";
        String clientInfo = player.getClientBrandName() != null ? player.getClientBrandName() : "Unknown";

        String timeKey = LocalTime.now().format(TIME_FORMATTER);

        User.PlayerLoginInfo loginInfo = new User.PlayerLoginInfo(ip, locationInfo, ispInfo, osInfo, clientInfo);
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

    private void logPlayerLogin(Player player, User.PlayerLoginInfo info) {
        logger.info("=== Player Login: " + player.getName() + " ===");
        logger.info("IP: " + info.getIp());
        logger.info("Location: " + info.getLocation());
        logger.info("ISP: " + info.getIsp());
        logger.info("OS: " + info.getOperatingSystem());
        logger.info("Client: " + info.getClientVersion());
        logger.info("================================");
    }

    @EventHandler
    public void onUserQuit(PlayerQuitEvent event) {
        User user = UserBase.getUser(event.getPlayer());
        if (user != null) {
            user.save();
            UserBase.removeUser(user);
            user.killNameTags();
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