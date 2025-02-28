package de.lobbyles.bobtonyslabor.essetials;

import de.lobbyles.bobtonyslabor.boby.User;
import de.lobbyles.bobtonyslabor.boby.UserBase;
import de.lobbyles.bobtonyslabor.boby.TonyMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class Mode implements CommandExecutor, Listener {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von einem Spieler verwendet werden.");
            return true;
        }

        Player player = (Player) sender;
        openModeSelectionInventory(player);
        return true;
    }

    private void openModeSelectionInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, "§m§o§d§e");

        for (TonyMode mode : TonyMode.values()) {
            ItemStack item = new ItemStack(Material.GHAST_TEAR);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(mode.name());
                meta.setCustomModelData(mode.lvl()+1);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().equals("§m§o§d§e")) return;

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String modeName = clickedItem.getItemMeta().getDisplayName();
        TonyMode newMode = TonyMode.fromString(modeName);
        if (newMode == null) return;

        User user = UserBase.getUser(player);
        user.setMode(newMode);
        user.save();
        player.sendMessage("Du hast den Modus zu " + newMode.name() + " gewechselt.");
        player.closeInventory();
    }
}
