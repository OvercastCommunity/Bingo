package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;
import tc.oc.bingo.config.ConfigReader;

@Tracker("food-eater")
public class FoodEaterObjective extends ObjectiveTracker.Stateful<Integer> {

  private static final ConfigReader<Material> MATERIAL_NAME_READER =
      (cfg, key, def) -> Material.getMaterial(cfg.getString(key));

  private final Supplier<Material> FOOD_REQUIRED =
      useConfig("food-name", Material.GOLDEN_APPLE, MATERIAL_NAME_READER);

  private final Supplier<Integer> FOOD_MIN_COUNT = useConfig("min-count", 100);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    Material item = event.getItem().getType();
    if (item != Material.GOLDEN_APPLE) return;

    Integer eaten = updateObjectiveData(event.getPlayer().getUniqueId(), i -> i + 1);

    if (eaten >= FOOD_MIN_COUNT.get()) {
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
    return (double) data / FOOD_MIN_COUNT.get();
  }
}
