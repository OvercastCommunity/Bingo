package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;

@Tracker("lava-fishing")
public class LavaFishingObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerFish(PlayerFishEvent event) {
    if (event.getState() != PlayerFishEvent.State.IN_GROUND) return;

    if (isHookInLava(event)) reward(event.getPlayer());
  }

  private boolean isHookInLava(PlayerFishEvent event) {
    Material hookMaterial = event.getHook().getLocation().getBlock().getType();
    return hookMaterial == Material.LAVA || hookMaterial == Material.STATIONARY_LAVA;
  }
}
