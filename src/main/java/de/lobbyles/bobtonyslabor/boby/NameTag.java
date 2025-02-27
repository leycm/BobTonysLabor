package de.lobbyles.bobtonyslabor.boby;


import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;

import java.util.HashMap;
import java.util.List;


import static de.lobbyles.bobtonyslabor.BobTonysLabor.*;
@Getter
public class NameTag {
    private HashMap<Integer, HashMap<String, Object>> lines = new HashMap<>();

    public NameTag(List<String> lines){

        for (int i = 0; i < aircrafter; i++) {
            lines.add("^^ AAirCrafter on Top ^^");
        }

        for (int i = 0; i < lines.size(); i++) {
            HashMap<String, Object> line = new HashMap<>();
            line.put("tag",null);
            line.put("placeholder",null);
            line.put("text", lines.get(i));
            this.lines.put(i,line);
        }
    }


    public void spawnNameTag(Player player){
        console.sendMessage("Spawn NameTags "+player.getName());
        Location location = player.getLocation();
        World world = location.getWorld();

        for (int i = 0; i < lines.size(); i++) {
            HashMap<String, Object> line = lines.get(i);
            String text = (String) line.get("text");
            console.sendMessage("Spawn NameTags "+i);

            ArmorStand tag = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
            tag.setCustomName(text);
            tag.setCustomNameVisible(text != null);
            tag.setInvisible(false);
            tag.setSilent(true);
            tag.setAI(false);
            tag.setInvulnerable(true);
            tag.setCollidable(false);
            tag.setGravity(false);
            tag.setBasePlate(false);
            tag.setVisible(false);
            tag.setMarker(true);
            tag.setSmall(true);
            tag.setFireTicks(0);
            tag.setCollidable(false);
            tag.setVisualFire(false);

            Tadpole placeholder = (Tadpole) world.spawnEntity(location, EntityType.TADPOLE);
            placeholder.setAI(false);
            placeholder.setInvisible(true);
            placeholder.setSilent(true);
            placeholder.setGravity(false);
            placeholder.setLootTable(null);
            placeholder.setInvulnerable(true);
            placeholder.setCanPickupItems(false);
            placeholder.setAgeLock(true);
            placeholder.setFireTicks(0);
            placeholder.setVisualFire(false);
            placeholder.setCollidable(false);
            placeholder.addScoreboardTag("placeholder");

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
