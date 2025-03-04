package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;

@Tracker("void-eating")
public class VoidEatingObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_HEIGHT = useConfig("max-height", 0);

  private final Supplier<Material> MATERIAL_REQUIRED =
      useConfig("food-name", Material.GOLDEN_APPLE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerConsume(PlayerItemConsumeEvent event) {
    if (event.getPlayer().getLocation().getY() > MAX_HEIGHT.get()) return;

    Material item = event.getItem().getType();
    if (item.isEdible() && item.equals(MATERIAL_REQUIRED.get())) {
      reward(event.getPlayer());
    }
  }
}
