package tc.oc.bingo.objectives;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("inventory-cluttered")
public class InventoryClutteredObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClose(InventoryCloseEvent event) {
    Player player = (Player) event.getPlayer();
    if (isInventoryFullyCluttered(player)) reward(player);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemPickup(PlayerPickupItemEvent event) {
    Player player = event.getPlayer();
    if (isInventoryFullyCluttered(player)) reward(player);
  }

  private boolean isInventoryFullyCluttered(Player player) {
    MatchPlayer matchPlayer = getPlayer(player.getUniqueId());
    if (matchPlayer == null) return false;
    if (!matchPlayer.isAlive()) return false;

    ItemStack[] inventoryContents = player.getInventory().getContents();
    Set<Material> uncompletedStackMaterial = new HashSet<Material>();
    // Check each slot
    for (ItemStack item : inventoryContents) {
      // Check empty slots
      if (item == null || item.getType() == Material.AIR) return false;
      // If there is already an uncompleted stack of a material
      if (item.getMaxStackSize() > item.getAmount()
          && !uncompletedStackMaterial.add(item.getType())) return false;
    }
    return true;
  }
}
