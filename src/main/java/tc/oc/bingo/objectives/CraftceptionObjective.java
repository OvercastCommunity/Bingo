package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;

@Tracker("craftception")
public class CraftceptionObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCraft(CraftItemEvent event) {
    if (event.getInventory().getType() == InventoryType.WORKBENCH) {
      if (event.getRecipe().getResult().getType() == Material.WORKBENCH) {
        reward(event.getActor());
      }
    }
  }
}
