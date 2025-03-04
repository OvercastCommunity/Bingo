package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemBreakEvent;

@Tracker("tool-breaker")
public class ToolBreakerObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Set<Material>> TARGET_TOOLS =
      useConfig(
          "target-tool",
          Set.of(
              Material.WOOD_HOE,
              Material.STONE_HOE,
              Material.IRON_HOE,
              Material.DIAMOND_HOE,
              Material.GOLD_HOE),
          MATERIAL_SET_READER);

  private final Supplier<Integer> BREAKS_REQUIRED = useConfig("breaks-required", 3);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onToolBreak(PlayerItemBreakEvent event) {
    if (!TARGET_TOOLS.get().contains(event.getBrokenItem().getType())) return;
    trackProgress(event.getPlayer());
  }

  @Override
  protected int maxValue() {
    return BREAKS_REQUIRED.get();
  }
}
