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
import java.util.*;
import java.util.stream.Collectors;

public class UserCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ERROR_PREFIX = "§c";
    private static final String INFO_PREFIX = "§e";
    private static final String HEADER_PREFIX = "§6";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "online":
            case "all":
                handleUserCommand(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleUserCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ERROR_PREFIX + "Benutzung: /user " + args[0] + " <spielername> <data/info/profile>");
            return;
        }

        String playerName = args[1];
        String subCommand = args[2].toLowerCase();
        boolean isOnlineOnly = args[0].equalsIgnoreCase("online");

        UserWrapper userWrapper = findUser(playerName, isOnlineOnly);

        if (userWrapper == null) {
            sender.sendMessage(ERROR_PREFIX + "Spieler " + playerName + (isOnlineOnly ? " ist nicht online." : " nicht gefunden."));
            return;
        }

        switch (subCommand) {
            case "info":
                sendUserInfo(sender, userWrapper);
                break;
            case "data":
                handleUserData(sender, args, userWrapper);
                break;
            case "profile":
                handleUserProfile(sender, args, userWrapper);
                break;
            default:
                sender.sendMessage(ERROR_PREFIX + "Ungültiger Parameter. Benutze 'info', 'data' oder 'profile'.");
                break;
        }
    }

    private UserWrapper findUser(String playerName, boolean onlineOnly) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            User user = UserBase.getUser(onlinePlayer);
            if (user != null) {
                return new UserWrapper(user, null);
            }
        }

        if (onlineOnly) {
            return null;
        }

        OfflinePlayer offlinePlayer = findOfflinePlayer(playerName);
        if (offlinePlayer != null) {
            OfflineUser offlineUser = OfflineUserBase.getOfflineUser(offlinePlayer);
            if (offlineUser == null) {
                offlineUser = new OfflineUser(offlinePlayer);
                OfflineUserBase.addOfflineUser(offlineUser);
            }
            return new UserWrapper(null, offlineUser);
        }

        return null;
    }

    private void handleUserData(CommandSender sender, String[] args, UserWrapper userWrapper) {
        if (args.length < 4) {
            sender.sendMessage(ERROR_PREFIX + "Benutzung: /user " + args[0] + " <spielername> data <logins/...>");
            return;
        }

        String dataType = args[3].toLowerCase();
        if ("logins".equals(dataType)) {
            handleLoginData(sender, args, userWrapper);
        } else {
            sender.sendMessage(ERROR_PREFIX + "Unbekannter Datentyp: " + dataType);
        }
    }

    private void handleUserProfile(CommandSender sender, String[] args, UserWrapper userWrapper) {
        if (args.length < 4) {
            sender.sendMessage(ERROR_PREFIX + "Benutzung: /user " + args[0] + " <spielername> profile <get/set>");
            return;
        }

        String action = args[3].toLowerCase();
        switch (action) {
            case "get":
                if (args.length < 5) {
                    sender.sendMessage(ERROR_PREFIX + "Benutzung: /user " + args[0] + " <spielername> profile get <name/playtime/mode/line>");
                    return;
                }
                getProfileSetting(sender, args[4], userWrapper);
                break;
            case "set":
                if (args.length < 6) {
                    sender.sendMessage(ERROR_PREFIX + "Benutzung: /user " + args[0] + " <spielername> profile set <name/playtime/mode/line> <wert>");
                    return;
                }
                setProfileSetting(sender, args[4], args[5], userWrapper, args);
                break;
            default:
                sender.sendMessage(ERROR_PREFIX + "Ungültiger Parameter. Benutze 'get' oder 'set'.");
                break;
        }
    }

    private void getProfileSetting(CommandSender sender, String setting, UserWrapper userWrapper) {
        String playerName = userWrapper.getName();
        String settingValue = "Nicht verfügbar";

        switch (setting.toLowerCase()) {
            case "nickname":
                settingValue = userWrapper.getNickname();
                break;
            case "playtime":
                settingValue = formatPlaytime(userWrapper.getPlaytime());
                break;
            case "mode":
                settingValue = userWrapper.getMode().toString();
                break;
            case "line":
                if (userWrapper.isOnline() && userWrapper.getOnlineUser().getNameTag() != null) {
                    HashMap<Integer, HashMap<String, Object>> lines = userWrapper.getOnlineUser().getNameTag().getLines();
                    StringBuilder sb = new StringBuilder();

                    sb.append("Aktuelle Zeilen:\n");
                    for (int i = 0; i < lines.size(); i++) {
                        HashMap<String, Object> line = lines.get(i);
                        sb.append(i + 1).append(": ").append(line.get("text")).append("\n");
                    }
                    settingValue = sb.toString();
                } else {
                    settingValue = "Keine Zeilen vorhanden.";
                }
                break;
        }

        sender.sendMessage(HEADER_PREFIX + "Profileinstellung für " + playerName + ":");
        sender.sendMessage(INFO_PREFIX + setting + ": §f" + settingValue);
    }

    private void setProfileSetting(CommandSender sender, String setting, String value, UserWrapper userWrapper, String[] args) {
        String playerName = userWrapper.getName();
        boolean success = false;

        try {
            switch (setting.toLowerCase()) {
                case "name":
                    if (userWrapper.isOnline()) {
                        userWrapper.getOnlineUser().getPlayer().setDisplayName(value);
                        success = true;
                    } else {
                        sender.sendMessage(ERROR_PREFIX + "Diese Einstellung kann nicht für Offline-Spieler geändert werden.");
                        return;
                    }
                    break;
                case "nickname":
                    if (userWrapper.isOnline()) {
                        userWrapper.getOnlineUser().setNickname(value);
                    } else {
                        userWrapper.getOfflineUser().setNickname(value);
                    }
                    success = true;
                    break;
                case "playtime":
                    if (userWrapper.isOnline()) {
                        int minutes = Integer.parseInt(value);
                        int ticks = minutes * 20 * 60;
                        userWrapper.getOnlineUser().getPlayer().setStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE, ticks);
                        success = true;
                    } else {
                        sender.sendMessage(ERROR_PREFIX + "Diese Einstellung kann nicht für Offline-Spieler geändert werden.");
                        return;
                    }
                    break;
                case "mode":
                    TonyMode mode = TonyMode.fromString(value);
                    if (userWrapper.isOnline()) {
                        userWrapper.getOnlineUser().setMode(mode);
                    } else {
                        userWrapper.getOfflineUser().setMode(mode);
                    }
                    success = true;
                    break;
                case "line":
                    if (userWrapper.isOnline()) {
                        if (args.length < 7) {
                            sender.sendMessage(ERROR_PREFIX + "Benutzung: /user " + args[0] + " <spielername> profile set line <position/under/over> <text>");
                            return;
                        }

                        String positionArg = args[5].toLowerCase();
                        String lineText = String.join(" ", Arrays.copyOfRange(args, 5, args.length));

                        HashMap<Integer, HashMap<String, Object>> lines = userWrapper.getOnlineUser().getNameTag().getLines();
                        List<String> linesList = new ArrayList<>();

                        for (int i = 0; i < lines.size(); i++) {
                            HashMap<String, Object> line = lines.get(i);
                            linesList.add((String) line.get("text"));
                        }

                        switch (positionArg) {
                            case "under":
                                if (linesList.isEmpty()) {
                                    linesList.add(lineText);
                                } else {
                                    linesList.add(lineText);
                                }
                                break;
                            case "over":
                                if (linesList.isEmpty()) {
                                    linesList.add(lineText);
                                } else {
                                    linesList.add(0, lineText);
                                }
                                break;
                            default:
                                try {
                                    int lineNumber = Integer.parseInt(positionArg) - 1;
                                    if (lineNumber < 0 || lineNumber >= linesList.size()) {
                                        sender.sendMessage(ERROR_PREFIX + "Ungültige Position: " + (lineNumber + 1));
                                        return;
                                    }
                                    linesList.set(lineNumber, lineText);
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(ERROR_PREFIX + "Ungültige Position: " + positionArg);
                                    return;
                                }
                                break;
                        }
                        userWrapper.getOnlineUser().setNameTags(linesList);
                        success = true;
                    } else {
                        sender.sendMessage(ERROR_PREFIX + "Diese Einstellung kann nicht für Offline-Spieler geändert werden.");
                        return;
                    }
                    break;
            }

            if (success) {
                userWrapper.save();
                sender.sendMessage("§aProfileinstellung " + setting + " für " + playerName + " wurde auf '" + value + "' gesetzt.");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ERROR_PREFIX + "Ungültiger Zahlenwert: " + value);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ERROR_PREFIX + "Ungültiger Wert: " + value);
        } catch (Exception e) {
            sender.sendMessage(ERROR_PREFIX + "Fehler beim Setzen der Einstellung: " + e.getMessage());
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

    private void handleLoginData(CommandSender sender, String[] args, UserWrapper userWrapper) {
        if (args.length < 5) {
            sender.sendMessage(ERROR_PREFIX + "Benutzung: /user " + args[0] + " <spielername> data logins <last/first/all/timeKey>");
            return;
        }

        String loginOption = args[4].toLowerCase();
        Map<String, User.PlayerLoginInfo> loginHistory = userWrapper.getLoginHistory();
        String userName = userWrapper.getName();

        if (loginHistory.isEmpty()) {
            sender.sendMessage(INFO_PREFIX + "Keine Login-Informationen für " + userName + " gefunden.");
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
                    sender.sendMessage(ERROR_PREFIX + "Login-Zeitstempel " + loginOption + " nicht gefunden.");
                }
                break;
        }
    }

    private void showLastLogin(CommandSender sender, Map<String, User.PlayerLoginInfo> loginHistory, String userName) {
        Map.Entry<String, User.PlayerLoginInfo> latest = findLoginByTime(loginHistory, true);
        if (latest != null) {
            showLoginDetail(sender, latest.getKey(), latest.getValue(), userName);
        }
    }

    private void showFirstLogin(CommandSender sender, Map<String, User.PlayerLoginInfo> loginHistory, String userName) {
        Map.Entry<String, User.PlayerLoginInfo> earliest = findLoginByTime(loginHistory, false);
        if (earliest != null) {
            showLoginDetail(sender, earliest.getKey(), earliest.getValue(), userName);
        }
    }

    private Map.Entry<String, User.PlayerLoginInfo> findLoginByTime(Map<String, User.PlayerLoginInfo> loginHistory, boolean latest) {
        LocalDateTime targetTime = latest ? LocalDateTime.MIN : LocalDateTime.MAX;
        String targetKey = null;
        User.PlayerLoginInfo targetInfo = null;

        for (Map.Entry<String, User.PlayerLoginInfo> entry : loginHistory.entrySet()) {
            User.PlayerLoginInfo info = entry.getValue();
            if ((latest && info.getTimestamp().isAfter(targetTime)) ||
                    (!latest && info.getTimestamp().isBefore(targetTime))) {
                targetTime = info.getTimestamp();
                targetKey = entry.getKey();
                targetInfo = info;
            }
        }

        return targetKey != null ? Map.entry(targetKey, targetInfo) : null;
    }

    private void showAllLogins(CommandSender sender, Map<String, User.PlayerLoginInfo> loginHistory, String userName) {
        sender.sendMessage(HEADER_PREFIX + "=== Login-Historie für " + userName + " ===");

        loginHistory.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().getTimestamp().compareTo(e1.getValue().getTimestamp()))
                .forEach(entry -> sender.sendMessage(
                        INFO_PREFIX + entry.getKey() + "§8 - " +
                                "§f" + entry.getValue().getTimestamp().format(DATE_FORMAT) +
                                "§8 - §f" + entry.getValue().getIp()));
    }

    private void showLoginDetail(CommandSender sender, String timeKey, User.PlayerLoginInfo info, String userName) {
        sender.sendMessage(HEADER_PREFIX + "=== Login-Details für " + userName + " (" + timeKey + ") ===");
        sender.sendMessage(INFO_PREFIX + "Zeit: §f" + info.getTimestamp().format(DATE_FORMAT));
        sender.sendMessage(INFO_PREFIX + "IP: §f" + info.getIp());
        sender.sendMessage(INFO_PREFIX + "Standort: §f" + info.getLocation());
        sender.sendMessage(INFO_PREFIX + "ISP: §f" + info.getIsp());
        sender.sendMessage(INFO_PREFIX + "Betriebssystem: §f" + info.getOperatingSystem());
        sender.sendMessage(INFO_PREFIX + "Client: §f" + info.getClientVersion());
    }

    private List<String> getLineCompletions(UserWrapper userWrapper) {
        List<String> completions = new ArrayList<>();
        if (userWrapper.isOnline() && userWrapper.getOnlineUser().getNameTag() != null) {
            HashMap<Integer, HashMap<String, Object>> lines = userWrapper.getOnlineUser().getNameTag().getLines();
            for (int i = 0; i < lines.size(); i++) {
                HashMap<String, Object> line = lines.get(i);
                completions.add((i + 1) + " " + line.get("text"));
            }
        }
        return completions;
    }

    private void sendUserInfo(CommandSender sender, UserWrapper userWrapper) {
        sender.sendMessage(HEADER_PREFIX + "=== " +
                (userWrapper.isOnline() ? "" : "Offline-") + "Benutzerinfo für " + userWrapper.getName() + " ===");

        sender.sendMessage(INFO_PREFIX + "UUID: §f" + userWrapper.getUUID());
        sender.sendMessage(INFO_PREFIX + "Modus: §f" + userWrapper.getMode());
        sender.sendMessage(INFO_PREFIX + "Erster Join: §f" +
                (userWrapper.getFirstJoin() != null ? userWrapper.getFirstJoin().format(DATE_FORMAT) : "Unbekannt"));
        sender.sendMessage(INFO_PREFIX + "Letzter Join: §f" +
                (userWrapper.getLastJoin() != null ? userWrapper.getLastJoin().format(DATE_FORMAT) : "Unbekannt"));
        sender.sendMessage(INFO_PREFIX + "Letzter Standort: §f" + userWrapper.getLastKnownLocation());
        sender.sendMessage(INFO_PREFIX + "ISP: §f" + userWrapper.getLastISP());
        sender.sendMessage(INFO_PREFIX + "Betriebssystem: §f" + userWrapper.getOperatingSystem());
        sender.sendMessage(INFO_PREFIX + "Client: §f" + userWrapper.getClientVersion());

        if (userWrapper.isOnline()) {
            sender.sendMessage(INFO_PREFIX + "Ping: §f" + userWrapper.getOnlineUser().getPing() + "ms");
        }

        if (userWrapper.isOnline()) {
            sender.sendMessage("§aSpieler ist derzeit online.");
        } else {
            sender.sendMessage("§cSpieler ist derzeit offline.");
        }

        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(userWrapper.getUUID())) {
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
        sender.sendMessage(HEADER_PREFIX + "=== User Command Hilfe ===");
        sender.sendMessage(INFO_PREFIX + "/user online <spielername> info §8- Zeigt Informationen über einen Online-Spieler");
        sender.sendMessage(INFO_PREFIX + "/user online <spielername> data logins [last/first/all/timeKey] §8- Zeigt Login-Daten eines Online-Spielers");
        sender.sendMessage(INFO_PREFIX + "/user online <spielername> profile [get/set] [name/playtime/mode/line] [wert] §8- Verwaltet Profileinstellungen");
        sender.sendMessage(INFO_PREFIX + "/user all <spielername> info §8- Zeigt Informationen über einen Spieler (online oder offline)");
        sender.sendMessage(INFO_PREFIX + "/user all <spielername> data logins [last/first/all/timeKey] §8- Zeigt Login-Daten eines Spielers");
        sender.sendMessage(INFO_PREFIX + "/user all <spielername> profile [get/set] [name/playtime/mode/line] [wert] §8- Verwaltet Profileinstellungen");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return getPartialMatches(args[0], Arrays.asList("online", "all"));
        } else if (args.length == 2) {
            return getPlayerNameCompletions(args);
        } else if (args.length == 3) {
            return getPartialMatches(args[2], Arrays.asList("info", "data", "profile"));
        } else if (args.length == 4) {
            return getSubcommandCompletions(args);
        } else if (args.length == 5) {
            return getDetailedOptionCompletions(args);
        } else if (args.length == 6) {
            return getSettingValueCompletions(args);
        }

        return new ArrayList<>();
    }

    private List<String> getPlayerNameCompletions(String[] args) {
        List<String> playerNames = new ArrayList<>();

        if (args[0].equalsIgnoreCase("online")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
        } else if (args[0].equalsIgnoreCase("all")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }

            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (player.getName() != null) {
                    playerNames.add(player.getName());
                }
            }
        }

        return getPartialMatches(args[1], playerNames);
    }

    private List<String> getSubcommandCompletions(String[] args) {
        if (args[2].equalsIgnoreCase("data")) {
            return getPartialMatches(args[3], Collections.singletonList("logins"));
        } else if (args[2].equalsIgnoreCase("profile")) {
            return getPartialMatches(args[3], Arrays.asList("get", "set"));
        }
        return new ArrayList<>();
    }

    private List<String> getDetailedOptionCompletions(String[] args) {
        if (args[2].equalsIgnoreCase("data") && args[3].equalsIgnoreCase("logins")) {
            List<String> options = new ArrayList<>(Arrays.asList("last", "first", "all"));

            UserWrapper userWrapper = findUser(args[1], false);
            if (userWrapper != null) {
                options.addAll(userWrapper.getLoginHistory().keySet());
            }

            return getPartialMatches(args[4], options);
        } else if (args[2].equalsIgnoreCase("profile")) {
            if (args[3].equalsIgnoreCase("get") || args[3].equalsIgnoreCase("set")) {
                return getPartialMatches(args[4], Arrays.asList("nickname", "playtime", "mode", "line"));
            }
        }
        return new ArrayList<>();
    }

    private List<String> getSettingValueCompletions(String[] args) {
        if (args[2].equalsIgnoreCase("profile") && args[3].equalsIgnoreCase("set")) {
            UserWrapper userWrapper = findUser(args[1], false);
            if (userWrapper != null) {
                switch (args[4].toLowerCase()) {
                    case "name":
                        return Collections.singletonList(userWrapper.getName());
                    case "playtime":
                        return Collections.singletonList(String.valueOf(userWrapper.getPlaytime()));
                    case "mode":
                        return Collections.singletonList(userWrapper.getMode().toString());
                    case "line":
                        if (userWrapper.isOnline()) {
                            List<String> completions = new ArrayList<>();
                            completions.add("under");
                            completions.add("over");
                            HashMap<Integer, HashMap<String, Object>> lines = userWrapper.getOnlineUser().getNameTag().getLines();
                            for (int i = 0; i < lines.size(); i++) {
                                HashMap<String, Object> line = lines.get(i);
                                completions.add((i + 1) + " " + line.get("text"));
                            }

                            return getPartialMatches(args[5], completions);
                        }
                        break;
                }
            }

            if (args[4].equalsIgnoreCase("mode")) {
                return Arrays.stream(TonyMode.values())
                        .map(TonyMode::toString)
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    private List<String> getPartialMatches(String token, List<String> options) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(token.toLowerCase()))
                .collect(Collectors.toList());
    }

    private static class UserWrapper {
        private final User onlineUser;
        private final OfflineUser offlineUser;

        public UserWrapper(User onlineUser, OfflineUser offlineUser) {
            this.onlineUser = onlineUser;
            this.offlineUser = offlineUser;
        }

        public boolean isOnline() {
            return onlineUser != null;
        }

        public User getOnlineUser() {
            return onlineUser;
        }

        public OfflineUser getOfflineUser() {
            return offlineUser;
        }

        public String getNickname() {
            if (isOnline()) {
                return onlineUser.getNickname();
            } else {
                return offlineUser.getNickname();
            }
        }

        public String getName() {
            if (isOnline()) {
                return onlineUser.getPlayer().getName();
            } else {
                return offlineUser.getOfflinePlayer().getName() != null ?
                        offlineUser.getOfflinePlayer().getName() :
                        offlineUser.getOfflinePlayer().getUniqueId().toString();
            }
        }

        public UUID getUUID() {
            if (isOnline()) {
                return onlineUser.getPlayer().getUniqueId();
            } else {
                return offlineUser.getOfflinePlayer().getUniqueId();
            }
        }

        public TonyMode getMode() {
            if (isOnline()) {
                return onlineUser.getMode();
            } else {
                return offlineUser.getMode();
            }
        }

        public LocalDateTime getFirstJoin() {
            if (isOnline()) {
                return onlineUser.getFirstJoin();
            } else {
                return offlineUser.getFirstJoin();
            }
        }

        public LocalDateTime getLastJoin() {
            if (isOnline()) {
                return onlineUser.getLastJoin();
            } else {
                return offlineUser.getLastJoin();
            }
        }

        public String getLastKnownLocation() {
            if (isOnline()) {
                return onlineUser.getLastKnownLocation();
            } else {
                return offlineUser.getLastKnownLocation();
            }
        }

        public String getLastISP() {
            if (isOnline()) {
                return onlineUser.getLastISP();
            } else {
                return offlineUser.getLastISP();
            }
        }

        public String getOperatingSystem() {
            if (isOnline()) {
                return onlineUser.getOperatingSystem();
            } else {
                return offlineUser.getOperatingSystem();
            }
        }

        public String getClientVersion() {
            if (isOnline()) {
                return onlineUser.getClientVersion();
            } else {
                return offlineUser.getClientVersion();
            }
        }

        public Map<String, User.PlayerLoginInfo> getLoginHistory() {
            if (isOnline()) {
                return onlineUser.getLoginHistory();
            } else {
                return offlineUser.getLoginHistory();
            }
        }

        public void save() {
            if (isOnline()) {
                onlineUser.save();
            } else {
                offlineUser.save();
            }
        }

        public int getPlaytime() {
            if (isOnline()) {
                return onlineUser.getPlayer().getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20 / 60;
            } else if (offlineUser.getOfflinePlayer().hasPlayedBefore()) {
                return offlineUser.getOfflinePlayer().getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20 / 60;
            }
            return 0;
        }
    }
}