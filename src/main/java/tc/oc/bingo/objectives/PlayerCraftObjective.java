package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;

@Tracker("player-craft")
public class PlayerCraftObjective extends ObjectiveTracker {

  public PlayerCraftObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCraftEvent(CraftItemEvent event) {
    if (event.getRecipe().getResult().getType().equals(Material.PISTON_BASE)) {
      reward(event.getActor().getPlayer());
    }
  }
}
