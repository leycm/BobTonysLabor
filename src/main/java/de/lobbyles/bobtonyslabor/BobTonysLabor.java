package de.lobbyles.bobtonyslabor;

import de.lobbyles.bobtonyslabor.boby.UserCommand;
import de.lobbyles.bobtonyslabor.boby.UserListener;
import de.lobbyles.bobtonyslabor.essetials.Mode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class BobTonysLabor extends JavaPlugin {
    public static JavaPlugin javaPlugin;
    public static Plugin plugin;
    public static BobTonysLabor instance;

    public static Logger logger;
    public static CommandSender console;
    
    public static Team team;

    public static final String PREFIX = "§3§lBob§b§lTony §8>§7";
    public static final String CONSOLE_PREFIX = "\u001B[0m[BobTony][BobTonyLabor] \u001B[0m";
    public static final String PLUGIN_FOLDER = "../bobtony";

    String c0 = "\u001B[0m";
    String c3 = "\u001B[36m";
    String cb = "\u001B[96m";
    String c8 = "\u001B[90m";
    String c7 = "\u001B[37m";
    String c6 = "\u001B[33m";

    @Override
    public void onEnable() {
        plugin = BobTonysLabor.getPlugin(BobTonysLabor.class);
        javaPlugin = JavaPlugin.getPlugin(BobTonysLabor.class);
        instance = this;

        logger = plugin.getLogger();
        console = Bukkit.getConsoleSender();

        createVanillaStyleTeam();

        List<String> enable = new ArrayList<>();
        enable.add(registerListener());
        enable.add(registerCommands());
        consolFeedback(enable);
    }

    @Override
    public void onDisable() {

        List<String> disableInfo = new ArrayList<>();

        consolFeedback(disableInfo);
    }

    public void onReload() {

        List<String> disableInfo = new ArrayList<>();

        consolFeedback(disableInfo);
    }

    public String registerListener(){
        PluginManager pluginManager = Bukkit.getPluginManager();
        try{
            pluginManager.registerEvents(new UserListener(),this);
            pluginManager.registerEvents(new Mode(),this);
            return "\u001B[90mLoading eventlistener...\u001B[0m";
        } catch (Exception e){
            return c6 + "Fail to load eventlistener\n" + e;
        }
    }

    public String loadConfig(){
        try{
            return "\u001B[90mLoading configs...\u001B[0m";
        } catch (Exception e){
            return c6 + "Fail to load configs\n" + e;
        }
    }

    public String registerCommands(){
        try{
            getCommand("user").setExecutor(new UserCommand());
            getCommand("mode").setExecutor(new Mode());
            return "\u001B[90mLoading commands...\u001B[0m";
        } catch (Exception e){
            return c6 + "Fail to load commands\n" + e;
        }
    }



    private void consolFeedback(List<String> list) {

        if(!Bukkit.getPluginManager().isPluginEnabled("BobTony")){
            console.sendMessage(c3 + "   __  "+cb+"  ___  " + c7 + "                         " + c0);
            console.sendMessage(c3 + "  |__| "+cb+"   |   " + c3 + "§3§lBob§b§lTony " +c0+ "v-1.0.1         " + c0);
            console.sendMessage(c3 + "  |__| "+cb+"   |   " + c8 + "Running on Bukkit - "+cb+"Paper" + c0);
            console.sendMessage(c3 + "       "+cb+"       " + c7 + "                         " + c0);
            for (String s  : list) {
                console.sendMessage( CONSOLE_PREFIX+s);
            }
            console.sendMessage(CONSOLE_PREFIX +"Finish Loading [BobTony]\u001B[0m");
        }
    }

    private void createVanillaStyleTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getMainScoreboard(); // Vanilla-Scoreboard verwenden

        team = board.getTeam("playerbase");
        if (team == null) {
            team = board.registerNewTeam("playerbase");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setAllowFriendlyFire(false);
        }
    }

}
