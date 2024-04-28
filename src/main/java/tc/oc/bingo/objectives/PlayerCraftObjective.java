package tc.oc.bingo.objectives;

import com.google.common.base.Objects;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import tc.oc.bingo.config.ConfigReader;

@Tracker("player-craft")
public class PlayerCraftObjective extends ObjectiveTracker {

  private static final ConfigReader<Material> MATERIAL_READER =
      (cfg, key, def) -> Objects.firstNonNull(Material.getMaterial(cfg.getString(key)), def);

  private final Supplier<Material> MATERIAL_REQUIRED =
      useConfig("material-name", Material.PISTON_BASE, MATERIAL_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCraftEvent(CraftItemEvent event) {
    if (event.getRecipe().getResult().getType().equals(MATERIAL_REQUIRED.get())) {
      reward(event.getActor().getPlayer());
    }
  }
}
