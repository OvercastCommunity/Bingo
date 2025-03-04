package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;

@Tracker("player-craft")
public class PlayerCraftObjective extends ObjectiveTracker {

  private final Supplier<Material> MATERIAL_REQUIRED =
      useConfig("material-name", Material.PISTON_BASE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCraftEvent(CraftItemEvent event) {
    if (event.getRecipe().getResult().getType().equals(MATERIAL_REQUIRED.get())) {
      reward(event.getActor().getPlayer());
    }
  }
}
