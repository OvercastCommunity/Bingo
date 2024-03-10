package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;

@Tracker("fisher")
public class FisherthemObjective extends ObjectiveTracker {

  public FisherthemObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerFish(PlayerFishEvent event) {
    if (event.getCaught() instanceof Item) {
      Item item = (Item) event.getCaught();
      if (item.getItemStack().getType().equals(Material.RAW_FISH)) {
        reward(event.getPlayer());
      }
    }
  }
}
