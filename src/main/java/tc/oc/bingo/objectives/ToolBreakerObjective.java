package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.jetbrains.annotations.NotNull;

@Tracker("tool-breaker")
public class ToolBreakerObjective extends ObjectiveTracker.Stateful<Integer> {

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
    Player player = event.getPlayer();
    UUID playerUUID = player.getUniqueId();

    if (!TARGET_TOOLS.get().contains(event.getBrokenItem().getType())) return;

    int breakCount = updateObjectiveData(playerUUID, count -> count + 1);
    if (breakCount >= BREAKS_REQUIRED.get()) {
      reward(player);
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
    return data.toString();
  }

  @Override
  public double progress(Integer data) {
    return (double) data / BREAKS_REQUIRED.get();
  }
}
