package tc.oc.bingo.objectives;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPickupItemEvent;

// Pick up "x" arrows shot by enemy players.
@Tracker("arrow-collect")
public class ArrowCollectObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> ARROWS_TO_PICK_UP = useConfig("arrow-pickups", 384);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onStrayArrowPickup(PlayerPickupItemEvent event) {
    if (!NMS_HACKS.isCraftItemArrowEntity(event)) return;

    trackProgress(event.getPlayer());
  }

  @Override
  protected int maxValue() {
    return ARROWS_TO_PICK_UP.get();
  }
}
