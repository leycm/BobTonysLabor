package de.lobbyles.bobtonyslabor.boby;


import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static de.lobbyles.bobtonyslabor.BobTonysLabor.*;
import static java.lang.String.valueOf;

@Getter
public class NameTag {
    private HashMap<Integer, HashMap<String, Object>> lines = new HashMap<>();

    public NameTag(List<String> lines, User user){
        List<String> rplines = replace(new ArrayList<>(lines), user);
        for (int i = 0; i < rplines.size(); i++) {
            HashMap<String, Object> line = new HashMap<>();
            line.put("tag",null);
            line.put("placeholder",null);
            line.put("text", rplines.get(i));
            this.lines.put(i,line);
        }
    }

    private List<String> replace(List<String> lines, User user) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            line = line.replace("%playername%", user.getNickname() != null ? user.getNickname() : "");
            line = line.replace("%rank%", user.getPlayerPrefix() != null ? user.getPlayerPrefix() : "");
            line = line.replace("%bobmode%", user.getMode() != null ? user.getMode().toString() : "");

            if (user.getPlayer() != null) {
                line = line.replace("%uuid%", user.getPlayer().getUniqueId().toString());
                line = line.replace("%realname%", user.getPlayer().getName());
                line = line.replace("%gamemode%", user.getPlayer().getGameMode().toString());

                if (user.getPlayer().getAddress() != null && user.getPlayer().getAddress().getAddress() != null) {
                    line = line.replace("%ip%", user.getPlayer().getAddress().getAddress().getHostAddress());
                } else {
                    line = line.replace("%ip%", "");
                }
            } else {
                line = line.replace("%uuid%", "");
                line = line.replace("%realname%", "");
                line = line.replace("%gamemode%", "");
                line = line.replace("%ip%", "");
            }

            line = line.replace("%isp%", user.getLastISP() != null ? user.getLastISP() : "");
            line = line.replace("%loc%", user.getLastKnownLocation() != null ? user.getLastKnownLocation() : "");
            line = line.replace("%clientbrand%", user.getClientVersion() != null ? user.getClientVersion() : "");
            line = line.replace("%ping%", String.valueOf(user.getPing()));
            lines.set(i, line);
        }
        return lines;
    }

    public void spawnNameTag(Player player){
        Location location = player.getLocation();
        World world = location.getWorld();

        for (int i = 0; i < lines.size(); i++) {
            HashMap<String, Object> line = lines.get(i);
            String text = (String) line.get("text");

            ArmorStand tag = (ArmorStand) world.spawnEntity(location.add(0, 1.5, 0), EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                ArmorStand armorStand = (ArmorStand) entity;
                armorStand.setCustomName(text);
                armorStand.setCustomNameVisible(text != null);
                armorStand.setInvisible(true);
                armorStand.setSilent(true);
                armorStand.setAI(false);
                armorStand.setInvulnerable(true);
                armorStand.setCollidable(false);
                armorStand.setGravity(false);
                armorStand.setBasePlate(false);
                armorStand.setMarker(true);
                armorStand.setSmall(true);
                armorStand.setFireTicks(0);
                armorStand.setVisualFire(false);
            });

            Tadpole placeholder = (Tadpole) world.spawnEntity(location, EntityType.TADPOLE, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                Tadpole tadpole = (Tadpole) entity;
                tadpole.setAI(false);
                tadpole.setInvisible(true);
                tadpole.setSilent(true);
                tadpole.setGravity(false);
                tadpole.setLootTable(null);
                tadpole.setInvulnerable(true);
                tadpole.setCanPickupItems(false);
                tadpole.setAgeLock(true);
                tadpole.setFireTicks(0);
                tadpole.setVisualFire(false);
                tadpole.setCollidable(false);
                tadpole.addScoreboardTag("placeholder");
            });

            line.put("tag",tag);
            line.put("placeholder",placeholder);
            lines.put(i,line);

            tag.addPassenger(placeholder);
            if(i==0){player.addPassenger(tag);}
            else {((LivingEntity) lines.get(i - 1).get("placeholder")).addPassenger(tag);}

            player.hideEntity(plugin, placeholder);
            player.hideEntity(plugin, tag);
        }
    }

    public void killNameTag() {
        for (HashMap<String, Object> line : lines.values()) {
            LivingEntity tag = (LivingEntity) line.get("tag");
            LivingEntity placeholder = (LivingEntity) line.get("placeholder");

            if (tag != null) tag.remove();
            if (placeholder != null) placeholder.remove();
        }
        lines.clear();
    }

    public void reloadNameTag(Player player) {
        killNameTag();
        spawnNameTag(player);
    }

}
