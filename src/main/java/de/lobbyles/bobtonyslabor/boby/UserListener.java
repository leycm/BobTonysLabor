package de.lobbyles.bobtonyslabor.boby;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class UserListener implements Listener {

    @EventHandler
    public void onUserJoin(PlayerJoinEvent event){
        User user = new User(event.getPlayer());
        UserBase.addUser(user);
    }

    @EventHandler
    public void onUserQuit(PlayerQuitEvent event){
        User user = UserBase.getUser(event.getPlayer());
        user.save();
        UserBase.removeUser(user);
    }
}
