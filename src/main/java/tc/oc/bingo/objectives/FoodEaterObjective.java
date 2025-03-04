package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;

@Tracker("food-eater")
public class FoodEaterObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Material> FOOD_REQUIRED = useConfig("food-name", Material.GOLDEN_APPLE);

  private final Supplier<Integer> FOOD_MIN_COUNT = useConfig("min-count", 100);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    Material item = event.getItem().getType();
    if (item != FOOD_REQUIRED.get()) return;

    trackProgress(event.getPlayer());
  }

  @Override
  protected int maxValue() {
    return FOOD_MIN_COUNT.get();
  }
}
