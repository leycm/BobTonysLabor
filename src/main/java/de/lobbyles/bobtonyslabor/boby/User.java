package de.lobbyles.bobtonyslabor.boby;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static de.lobbyles.bobtonyslabor.BobTonysLabor.PLUGIN_FOLDER;


@Getter
@Setter
public class User{
    private final Player player;
    private FileConfiguration playerFile;
    private HashMap<LocalTime, String> ips;
    private TonyMode mode;

    public User(Player player){
        this.player = player;
        this.mode = TonyMode.MEMBER;
        this.ips = new HashMap<>();
        this.playerFile = loadConfig(player.getUniqueId()+"");

        File file = new File(PLUGIN_FOLDER+"/playerdata", player.getUniqueId()+ ".yml");
        if (file.exists()) {
            reload();
        } else
            save();
    }

    public void reload() {
        playerFile = loadConfig(player.getUniqueId().toString());

        if (playerFile == null) return;
        mode.fromString(playerFile.getString("player.mode","MEMBER"));

        ips.clear();
        ConfigurationSection ipSection = playerFile.getConfigurationSection("user.ips");

        if (ipSection != null) {
            for (String key : ipSection.getKeys(false)) {
                String ip = ipSection.getString(key);
                ips.put(LocalTime.parse(key), ip);
            }
        }
    }

    public void save() {
        if (playerFile == null) return;

        playerFile.set("player.name",player.getName());
        playerFile.set("player.uuid",player.getUniqueId()+"");
        playerFile.set("player.playtime",player.getPlayerTime());
        playerFile.set("player.mode",mode.toString());

        for (Map.Entry entry: ips.entrySet()){
            playerFile.set("user.ips"+entry.getKey(),entry.getValue().toString());
        }


        saveConfig(playerFile, player.getUniqueId().toString());
    }


    private FileConfiguration createConfig(String title) {
        File folder = new File(PLUGIN_FOLDER+"/playerdata");
        if (!folder.exists()) {folder.mkdirs();}

        File file = new File(folder, title + ".yml");
        try {
            file.createNewFile();
            return YamlConfiguration.loadConfiguration(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    private FileConfiguration loadConfig(String title) {
        File file = new File(PLUGIN_FOLDER+"/playerdata", title + ".yml");

        if (!file.exists()) {
            return createConfig(title);
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveConfig(FileConfiguration config, String title) {
        File file = new File(PLUGIN_FOLDER + "/playerdata", title + ".yml");
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
