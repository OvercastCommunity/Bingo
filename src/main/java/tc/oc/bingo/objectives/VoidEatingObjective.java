package tc.oc.bingo.objectives;

import com.google.common.base.Objects;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import tc.oc.bingo.config.ConfigReader;

@Tracker("void-eating")
public class VoidEatingObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_HEIGHT = useConfig("max-height", 0);

  private static final ConfigReader<Material> MATERIAL_READER =
      (cfg, key, def) -> Objects.firstNonNull(Material.getMaterial(cfg.getString(key)), def);

  private final Supplier<Material> MATERIAL_REQUIRED =
      useConfig("food-name", Material.GOLDEN_APPLE, MATERIAL_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    if (event.getPlayer().getLocation().getY() > MAX_HEIGHT.get()) return;

    Material item = event.getItem().getType();
    if (item.isEdible() && item.equals(MATERIAL_REQUIRED.get())) {
      reward(event.getPlayer());
    }
  }
}
