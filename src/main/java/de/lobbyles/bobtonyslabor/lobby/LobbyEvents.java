package de.lobbyles.bobtonyslabor.lobby;

import de.lobbyles.bobtonyslabor.boby.User;
import de.lobbyles.bobtonyslabor.boby.UserBase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;

public class LobbyEvents implements Listener {

    @EventHandler
    public void onUserMove(PlayerMoveEvent event){
        Player p = event.getPlayer();
        User u = UserBase.getUser(p);

        if(u.getMode().lvl() < 2){event.setCancelled(true);}

        if(u.getMode().lvl() == 0){
            Inventory trap = Bukkit.createInventory(null,9*1,"TrapHaHa");
            p.openInventory(trap);
        }
    }

    @EventHandler
    public void onUserInteract(PlayerInteractEvent event){
        Player p = event.getPlayer();
        User u = UserBase.getUser(p);

        if(u.getMode().lvl() < 3){event.setCancelled(true);}
    }

    @EventHandler
    public void onUserInvInteract(InventoryClickEvent event){
        Player p = (Player) event.getWhoClicked();
        User u = UserBase.getUser(p);

        if(u.getMode().lvl() < 3){event.setCancelled(true);}
    }

    @EventHandler
    public void onUserInventoryClose(InventoryCloseEvent event){
        Player p = (Player) event.getPlayer();
        User u = UserBase.getUser(p);

        if(u.getMode().lvl() == 0){
            Inventory trap = Bukkit.createInventory(null,9*1,"TrapHaHa");
            p.openInventory(trap);
        }
    }

    @EventHandler
    public void onUserJoin(PlayerJoinEvent event){
        Player p = (Player) event.getPlayer();

        if(!(p.hasPermission("bobtony.admin"))){
            World world = Bukkit.getWorld("world");
            Location spawn = new Location(world, 0.5, 64, 0.5, 90.0f, 0.0f);
            p.teleport(spawn);
        }
    }
}
