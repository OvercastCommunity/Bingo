package tc.oc.bingo.objectives;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.jetbrains.annotations.NotNull;

// Pick up "x" arrows shot by enemy players.
@Tracker("arrow-collect")
public class ArrowCollectObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> ARROWS_TO_PICK_UP = useConfig("arrow-pickups", 384);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onStrayArrowPickup(PlayerPickupItemEvent event) {
    if (!NMS_HACKS.isCraftItemArrowEntity(event)) return;

    Integer arrows = updateObjectiveData(event.getPlayer().getUniqueId(), i -> i + 1);

    if (arrows >= ARROWS_TO_PICK_UP.get()) {
      reward(event.getPlayer());
    }
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return Integer.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Integer data) {
    return (double) data / ARROWS_TO_PICK_UP.get();
  }
}
