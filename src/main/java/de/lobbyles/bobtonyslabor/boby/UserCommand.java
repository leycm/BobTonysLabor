package de.lobbyles.bobtonyslabor.boby;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

public class UserCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "online":
                handleOnlineUser(sender, args);
                break;
            case "all":
                handleAllUser(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleOnlineUser(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cBenutzung: /user online <spielername> <data/info/profile>");
            return;
        }

        String playerName = args[1];
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("§cSpieler " + playerName + " ist nicht online.");
            return;
        }

        User user = UserBase.getUser(player);
        if (user == null) {
            sender.sendMessage("§cBenutzer nicht gefunden.");
            return;
        }

        String subCommand = args[2].toLowerCase();
        switch (subCommand) {
            case "info":
                sendUserInfo(sender, user);
                break;
            case "data":
                handleUserData(sender, args, user, null);
                break;
            case "profile":
                handleUserProfile(sender, args, user, null);
                break;
            default:
                sender.sendMessage("§cUngültiger Parameter. Benutze 'info', 'data' oder 'profile'.");
                break;
        }
    }

    private void handleAllUser(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cBenutzung: /user all <spielername> <data/info/profile>");
            return;
        }

        String playerName = args[1];
        Player onlinePlayer = Bukkit.getPlayer(playerName);

        if (onlinePlayer != null) {
            User user = UserBase.getUser(onlinePlayer);
            if (user != null) {
                String subCommand = args[2].toLowerCase();
                switch (subCommand) {
                    case "info":
                        sendUserInfo(sender, user);
                        break;
                    case "data":
                        handleUserData(sender, args, user, null);
                        break;
                    case "profile":
                        handleUserProfile(sender, args, user, null);
                        break;
                    default:
                        sender.sendMessage("§cUngültiger Parameter. Benutze 'info', 'data' oder 'profile'.");
                        break;
                }
                return;
            }
        }

        OfflinePlayer offlinePlayer = findOfflinePlayer(playerName);
        if (offlinePlayer == null) {
            sender.sendMessage("§cSpieler " + playerName + " nicht gefunden.");
            return;
        }

        OfflineUser offlineUser = OfflineUserBase.getOfflineUser(offlinePlayer);
        if (offlineUser == null) {
            offlineUser = new OfflineUser(offlinePlayer);
            OfflineUserBase.addOfflineUser(offlineUser);
        }

        String subCommand = args[2].toLowerCase();
        switch (subCommand) {
            case "info":
                sendOfflineUserInfo(sender, offlineUser);
                break;
            case "data":
                handleUserData(sender, args, null, offlineUser);
                break;
            case "profile":
                handleUserProfile(sender, args, null, offlineUser);
                break;
            default:
                sender.sendMessage("§cUngültiger Parameter. Benutze 'info', 'data' oder 'profile'.");
                break;
        }
    }

    private void handleUserData(CommandSender sender, String[] args, User onlineUser, OfflineUser offlineUser) {
        if (args.length < 4) {
            sender.sendMessage("§cBenutzung: /user <online/all> <spielername> data <logins/...>");
            return;
        }

        String dataType = args[3].toLowerCase();
        switch (dataType) {
            case "logins":
                handleLoginData(sender, args, onlineUser, offlineUser);
                break;
            default:
                sender.sendMessage("§cUnbekannter Datentyp: " + dataType);
                break;
        }
    }

    private void handleUserProfile(CommandSender sender, String[] args, User onlineUser, OfflineUser offlineUser) {
        if (args.length < 4) {
            sender.sendMessage("§cBenutzung: /user <online/all> <spielername> profile <get/set>");
            return;
        }

        String action = args[3].toLowerCase();
        switch (action) {
            case "get":
                if (args.length < 5) {
                    sender.sendMessage("§cBenutzung: /user <online/all> <spielername> profile get <name/playtime/mode/line>");
                    return;
                }
                getProfileSetting(sender, args[4], onlineUser, offlineUser);
                break;
            case "set":
                if (args.length < 6) {
                    sender.sendMessage("§cBenutzung: /user <online/all> <spielername> profile set <name/playtime/mode/line> <wert>");
                    return;
                }
                setProfileSetting(sender, args[4], args[5], onlineUser, offlineUser);
                break;
            default:
                sender.sendMessage("§cUngültiger Parameter. Benutze 'get' oder 'set'.");
                break;
        }
    }

    private void getProfileSetting(CommandSender sender, String setting, User onlineUser, OfflineUser offlineUser) {
        String playerName;
        String settingValue = "Nicht verfügbar";

        if (onlineUser != null) {
            playerName = onlineUser.getPlayer().getName();
            switch (setting.toLowerCase()) {
                case "name":
                    settingValue = onlineUser.getPlayer().getName();
                    break;
                case "playtime":
                    int minutes = onlineUser.getPlayer().getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20 / 60;
                    settingValue = formatPlaytime(minutes);
                    break;
                case "mode":
                    settingValue = onlineUser.getMode().toString();
                    break;
                case "line":
                    if (onlineUser.getNameTag() != null && !onlineUser.getNameTag().getLines().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        HashMap<Integer, HashMap<String, Object>> lines = onlineUser.getNameTag().getLines();
                        for (int i = 0; i < lines.size(); i++) {
                            HashMap<String, Object> line = lines.get(i);
                            sb.append(i + 1).append(": ").append(line.get("text")).append(", ");
                        }
                        settingValue = sb.toString().replaceAll(", $", "");
                    }
                    break;
                default:
                    sender.sendMessage("§cUnbekannte Profileinstellung: " + setting);
                    return;
            }
        } else if (offlineUser != null) {
            playerName = offlineUser.getOfflinePlayer().getName() != null ?
                    offlineUser.getOfflinePlayer().getName() : offlineUser.getOfflinePlayer().getUniqueId().toString();
            switch (setting.toLowerCase()) {
                case "name":
                    settingValue = playerName;
                    break;
                case "playtime":
                    if (offlineUser.getOfflinePlayer().hasPlayedBefore()) {
                        int minutes = offlineUser.getOfflinePlayer().getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20 / 60;
                        settingValue = formatPlaytime(minutes);
                    }
                    break;
                case "mode":
                    settingValue = offlineUser.getMode().toString();
                    break;
                default:
                    sender.sendMessage("§cUnbekannte Profileinstellung: " + setting);
                    return;
            }
        } else {
            sender.sendMessage("§cKein Benutzer gefunden.");
            return;
        }

        sender.sendMessage("§6Profileinstellung für " + playerName + ":");
        sender.sendMessage("§e" + setting + ": §f" + settingValue);
    }

    private void setProfileSetting(CommandSender sender, String setting, String value, User onlineUser, OfflineUser offlineUser) {
        String playerName;
        boolean success = false;

        if (onlineUser != null) {
            playerName = onlineUser.getPlayer().getName();
            switch (setting.toLowerCase()) {
                case "name":
                    onlineUser.getPlayer().setDisplayName(value);
                    success = true;
                    break;
                case "playtime":
                    try {
                        int minutes = parseInt(value);
                        int ticks = minutes * 20 * 60;
                        onlineUser.getPlayer().setStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE, ticks);
                        success = true;
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cUngültiger Wert für Spielzeit: " + value);
                        return;
                    }
                    break;
                case "mode":
                    try {
                        TonyMode mode = TonyMode.fromString(value);
                        onlineUser.setMode(mode);
                        success = true;
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§cUngültiger Modus: " + value);
                        return;
                    }
                    break;
                case "line":
                    try {
                        String[] parts = value.split(" ", 2);
                        int lineNumber = parseInt(parts[0]) - 1;
                        String lineText = parts.length > 1 ? parts[1] : "";

                        HashMap<Integer, HashMap<String, Object>> lines = onlineUser.getNameTag().getLines();
                        List<String> linesList = new ArrayList<>();

                        // Convert current lines to list format for setNameTags method
                        for (int i = 0; i < Math.max(lines.size(), lineNumber + 1); i++) {
                            if (i == lineNumber) {
                                linesList.add(lineText);
                            } else if (i < lines.size()) {
                                HashMap<String, Object> line = lines.get(i);
                                linesList.add((String) line.get("text"));
                            } else {
                                linesList.add("");
                            }
                        }

                        onlineUser.setNameTags(linesList);
                        success = true;
                    } catch (Exception e) {
                        sender.sendMessage("§cUngültiges Format für Line. Benutze: <linenumber> <text>");
                        return;
                    }
                    break;
                default:
                    sender.sendMessage("§cUnbekannte Profileinstellung: " + setting);
                    return;
            }
            if (success) {
                onlineUser.save();
            }
        } else if (offlineUser != null) {
            playerName = offlineUser.getOfflinePlayer().getName() != null ?
                    offlineUser.getOfflinePlayer().getName() : offlineUser.getOfflinePlayer().getUniqueId().toString();
            switch (setting.toLowerCase()) {
                case "mode":
                    try {
                        TonyMode mode = TonyMode.fromString(value);
                        offlineUser.setMode(mode);
                        success = true;
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§cUngültiger Modus: " + value);
                        return;
                    }
                    break;
                default:
                    sender.sendMessage("§cDiese Einstellung kann nicht für Offline-Spieler geändert werden.");
                    return;
            }
            if (success) {
                offlineUser.save();
            }
        } else {
            sender.sendMessage("§cKein Benutzer gefunden.");
            return;
        }

        if (success) {
            sender.sendMessage("§aProfileinstellung " + setting + " für " + playerName + " wurde auf '" + value + "' gesetzt.");
        }
    }

    private String formatPlaytime(int minutes) {
        int hours = minutes / 60;
        int days = hours / 24;
        hours = hours % 24;
        minutes = minutes % 60;

        if (days > 0) {
            return days + " Tag(e), " + hours + " Stunde(n), " + minutes + " Minute(n)";
        } else if (hours > 0) {
            return hours + " Stunde(n), " + minutes + " Minute(n)";
        } else {
            return minutes + " Minute(n)";
        }
    }

    private void handleLoginData(CommandSender sender, String[] args, User onlineUser, OfflineUser offlineUser) {
        if (args.length < 5) {
            sender.sendMessage("§cBenutzung: /user <online/all> <spielername> data logins <last/first/all/timeKey>");
            return;
        }

        String loginOption = args[4].toLowerCase();
        Map<String, User.PlayerLoginInfo> loginHistory;
        String userName;

        if (onlineUser != null) {
            loginHistory = onlineUser.getLoginHistory();
            userName = onlineUser.getPlayer().getName();
        } else if (offlineUser != null) {
            loginHistory = offlineUser.getLoginHistory();
            userName = offlineUser.getOfflinePlayer().getName() != null ?
                    offlineUser.getOfflinePlayer().getName() : offlineUser.getOfflinePlayer().getUniqueId().toString();
        } else {
            sender.sendMessage("§cKein Benutzer gefunden.");
            return;
        }

        switch (loginOption) {
            case "last":
                showLastLogin(sender, loginHistory, userName);
                break;
            case "first":
                showFirstLogin(sender, loginHistory, userName);
                break;
            case "all":
                showAllLogins(sender, loginHistory, userName);
                break;
            default:
                // Assuming it's a timeKey
                if (loginHistory.containsKey(loginOption)) {
                    showLoginDetail(sender, loginOption, loginHistory.get(loginOption), userName);
                } else {
                    sender.sendMessage("§cLogin-Zeitstempel " + loginOption + " nicht gefunden.");
                }
                break;
        }
    }

    private void showLastLogin(CommandSender sender, Map<String, User.PlayerLoginInfo> loginHistory, String userName) {
        if (loginHistory.isEmpty()) {
            sender.sendMessage("§eKeine Login-Informationen für " + userName + " gefunden.");
            return;
        }

        LocalDateTime latestTime = LocalDateTime.MIN;
        String latestKey = null;
        User.PlayerLoginInfo latestInfo = null;

        for (Map.Entry<String, User.PlayerLoginInfo> entry : loginHistory.entrySet()) {
            User.PlayerLoginInfo info = entry.getValue();
            if (info.getTimestamp().isAfter(latestTime)) {
                latestTime = info.getTimestamp();
                latestKey = entry.getKey();
                latestInfo = info;
            }
        }

        if (latestInfo != null) {
            showLoginDetail(sender, latestKey, latestInfo, userName);
        }
    }

    private void showFirstLogin(CommandSender sender, Map<String, User.PlayerLoginInfo> loginHistory, String userName) {
        if (loginHistory.isEmpty()) {
            sender.sendMessage("§eKeine Login-Informationen für " + userName + " gefunden.");
            return;
        }

        LocalDateTime earliestTime = LocalDateTime.MAX;
        String earliestKey = null;
        User.PlayerLoginInfo earliestInfo = null;

        for (Map.Entry<String, User.PlayerLoginInfo> entry : loginHistory.entrySet()) {
            User.PlayerLoginInfo info = entry.getValue();
            if (info.getTimestamp().isBefore(earliestTime)) {
                earliestTime = info.getTimestamp();
                earliestKey = entry.getKey();
                earliestInfo = info;
            }
        }

        if (earliestInfo != null) {
            showLoginDetail(sender, earliestKey, earliestInfo, userName);
        }
    }

    private void showAllLogins(CommandSender sender, Map<String, User.PlayerLoginInfo> loginHistory, String userName) {
        if (loginHistory.isEmpty()) {
            sender.sendMessage("§eKeine Login-Informationen für " + userName + " gefunden.");
            return;
        }

        sender.sendMessage("§6=== Login-Historie für " + userName + " ===");

        List<Map.Entry<String, User.PlayerLoginInfo>> sortedLogins = new ArrayList<>(loginHistory.entrySet());
        sortedLogins.sort((e1, e2) -> e2.getValue().getTimestamp().compareTo(e1.getValue().getTimestamp()));

        for (Map.Entry<String, User.PlayerLoginInfo> entry : sortedLogins) {
            String timeKey = entry.getKey();
            User.PlayerLoginInfo info = entry.getValue();
            sender.sendMessage("§e" + timeKey + "§8 - " +
                    "§f" + info.getTimestamp().format(DATE_FORMAT) +
                    "§8 - §f" + info.getIp());
        }
    }

    private void showLoginDetail(CommandSender sender, String timeKey, User.PlayerLoginInfo info, String userName) {
        sender.sendMessage("§6=== Login-Details für " + userName + " (" + timeKey + ") ===");
        sender.sendMessage("§eZeit: §f" + info.getTimestamp().format(DATE_FORMAT));
        sender.sendMessage("§eIP: §f" + info.getIp());
        sender.sendMessage("§eStandort: §f" + info.getLocation());
        sender.sendMessage("§eISP: §f" + info.getIsp());
        sender.sendMessage("§eBetriebssystem: §f" + info.getOperatingSystem());
        sender.sendMessage("§eClient: §f" + info.getClientVersion());
    }

    private void sendUserInfo(CommandSender sender, User user) {
        sender.sendMessage("§6=== Benutzerinfo für " + user.getPlayer().getName() + " ===");
        sender.sendMessage("§eUUID: §f" + user.getPlayer().getUniqueId());
        sender.sendMessage("§eModus: §f" + user.getMode());
        sender.sendMessage("§eErster Join: §f" +
                (user.getFirstJoin() != null ? user.getFirstJoin().format(DATE_FORMAT) : "Unbekannt"));
        sender.sendMessage("§eLetzter Join: §f" +
                (user.getLastJoin() != null ? user.getLastJoin().format(DATE_FORMAT) : "Unbekannt"));
        sender.sendMessage("§eLetzter Standort: §f" + user.getLastKnownLocation());
        sender.sendMessage("§eISP: §f" + user.getLastISP());
        sender.sendMessage("§eBetriebssystem: §f" + user.getOperatingSystem());
        sender.sendMessage("§eClient: §f" + user.getClientVersion());
        sender.sendMessage("§ePing: §f" + user.getPing() + "ms");

        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(user.getPlayer().getUniqueId())) {
            sender.sendMessage("§aDies ist dein aktueller User!");
        }
    }

    private void sendOfflineUserInfo(CommandSender sender, OfflineUser user) {
        String playerName = user.getOfflinePlayer().getName() != null ?
                user.getOfflinePlayer().getName() : user.getOfflinePlayer().getUniqueId().toString();

        sender.sendMessage("§6=== Offline-Benutzerinfo für " + playerName + " ===");
        sender.sendMessage("§eUUID: §f" + user.getOfflinePlayer().getUniqueId());
        sender.sendMessage("§eModus: §f" + user.getMode());
        sender.sendMessage("§eErster Join: §f" +
                (user.getFirstJoin() != null ? user.getFirstJoin().format(DATE_FORMAT) : "Unbekannt"));
        sender.sendMessage("§eLetzter Join: §f" +
                (user.getLastJoin() != null ? user.getLastJoin().format(DATE_FORMAT) : "Unbekannt"));
        sender.sendMessage("§eLetzter Standort: §f" + user.getLastKnownLocation());
        sender.sendMessage("§eISP: §f" + user.getLastISP());
        sender.sendMessage("§eBetriebssystem: §f" + user.getOperatingSystem());
        sender.sendMessage("§eClient: §f" + user.getClientVersion());

        if (user.isOnline()) {
            sender.sendMessage("§aSpieler ist derzeit online.");
        } else {
            sender.sendMessage("§cSpieler ist derzeit offline.");
        }

        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(user.getOfflinePlayer().getUniqueId())) {
            sender.sendMessage("§aDies ist dein aktueller User!");
        }
    }

    private OfflinePlayer findOfflinePlayer(String nameOrUuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(nameOrUuid);
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            return offlinePlayer;
        }

        try {
            UUID uuid = UUID.fromString(nameOrUuid);
            offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                return offlinePlayer;
            }
        } catch (IllegalArgumentException ignored) {}

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null && player.getName().equalsIgnoreCase(nameOrUuid)) {
                return player;
            }
        }

        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== User Command Hilfe ===");
        sender.sendMessage("§e/user online <spielername> info §8- Zeigt Informationen über einen Online-Spieler");
        sender.sendMessage("§e/user online <spielername> data logins [last/first/all/timeKey] §8- Zeigt Login-Daten eines Online-Spielers");
        sender.sendMessage("§e/user online <spielername> profile [get/set] [name/playtime/mode/line] §8- Verwaltet Profileinstellungen");
        sender.sendMessage("§e/user all <spielername> info §8- Zeigt Informationen über einen Spieler (online oder offline)");
        sender.sendMessage("§e/user all <spielername> data logins [last/first/all/timeKey] §8- Zeigt Login-Daten eines Spielers");
        sender.sendMessage("§e/user all <spielername> profile [get/set] [name/playtime/mode/line] §8- Verwaltet Profileinstellungen");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return getPartialMatches(args[0], Arrays.asList("online", "all"));
        } else if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            if (args[0].equalsIgnoreCase("online")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
            } else if (args[0].equalsIgnoreCase("all")) {
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName() != null) {
                        playerNames.add(player.getName());
                    }
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
            }
            return getPartialMatches(args[1], playerNames);
        } else if (args.length == 3) {
            return getPartialMatches(args[2], Arrays.asList("info", "data", "profile"));
        } else if (args.length == 4) {
            if (args[2].equalsIgnoreCase("data")) {
                return getPartialMatches(args[3], Arrays.asList("logins"));
            } else if (args[2].equalsIgnoreCase("profile")) {
                return getPartialMatches(args[3], Arrays.asList("get", "set"));
            }
        } else if (args.length == 5) {
            if (args[2].equalsIgnoreCase("data") && args[3].equalsIgnoreCase("logins")) {
                List<String> options = new ArrayList<>(Arrays.asList("last", "first", "all"));

                if (args[0].equalsIgnoreCase("online") || args[0].equalsIgnoreCase("all")) {
                    Player player = Bukkit.getPlayer(args[1]);
                    if (player != null) {
                        User user = UserBase.getUser(player);
                        if (user != null) {
                            options.addAll(user.getLoginHistory().keySet());
                        }
                    }
                }

                if (args[0].equalsIgnoreCase("all")) {
                    OfflinePlayer offlinePlayer = findOfflinePlayer(args[1]);
                    if (offlinePlayer != null) {
                        OfflineUser offlineUser = OfflineUserBase.getOfflineUser(offlinePlayer);
                        if (offlineUser == null) {
                            offlineUser = new OfflineUser(offlinePlayer);
                        }
                        options.addAll(offlineUser.getLoginHistory().keySet());
                    }
                }

                return getPartialMatches(args[4], options);
            } else if (args[2].equalsIgnoreCase("profile")) {
                if (args[3].equalsIgnoreCase("get") || args[3].equalsIgnoreCase("set")) {
                    return getPartialMatches(args[4], Arrays.asList("name", "playtime", "mode", "line"));
                }
            }
        } else if (args.length == 6) {
            if (args[2].equalsIgnoreCase("profile") && args[3].equalsIgnoreCase("set")) {
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer != null) {
                    User user = UserBase.getUser(targetPlayer);
                    if (user != null) {
                        switch (args[4].toLowerCase()) {
                            case "name":
                                return Arrays.asList(targetPlayer.getDisplayName());
                            case "playtime":
                                int minutes = targetPlayer.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20 / 60;
                                return Arrays.asList(String.valueOf(minutes));
                            case "mode":
                                return Arrays.asList(user.getMode().toString());
                            case "line":
                                List<String> lineOptions = new ArrayList<>();
                                HashMap<Integer, HashMap<String, Object>> lines = user.getNameTag().getLines();
                                for (int i = 0; i < lines.size(); i++) {
                                    HashMap<String, Object> line = lines.get(i);
                                    lineOptions.add((i+1) + " " + line.get("text"));
                                }
                                lineOptions.add((lines.size()+1) + " ");
                                return getPartialMatches(args[5], lineOptions);
                        }
                    }
                } else {
                    OfflinePlayer offlinePlayer = findOfflinePlayer(args[1]);
                    if (offlinePlayer != null) {
                        OfflineUser offlineUser = OfflineUserBase.getOfflineUser(offlinePlayer);
                        if (offlineUser != null) {
                            if (args[4].equalsIgnoreCase("mode")) {
                                return Arrays.asList(offlineUser.getMode().toString());
                            }
                        }
                    }
                }

                // Fallback options if specific user values aren't available
                if (args[4].equalsIgnoreCase("mode")) {
                    return Arrays.stream(TonyMode.values())
                            .map(TonyMode::toString)
                            .collect(Collectors.toList());
                }
            }
        }

        return completions;
    }

    private List<String> getPartialMatches(String token, List<String> options) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(token.toLowerCase()))
                .collect(Collectors.toList());
    }
}