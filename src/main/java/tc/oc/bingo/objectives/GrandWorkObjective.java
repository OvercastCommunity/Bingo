package tc.oc.bingo.objectives;

import org.bukkit.Art;
import org.bukkit.entity.Painting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.hanging.HangingPlaceEvent;

@Tracker("grand-work")
public class GrandWorkObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onHangingPlace(HangingPlaceEvent event) {
    if (!(event.getEntity() instanceof Painting painting)) return;

    Art art = painting.getArt();

    if (art.getBlockHeight() >= 4 || art.getBlockWidth() >= 4) reward(event.getPlayer());
  }
}
