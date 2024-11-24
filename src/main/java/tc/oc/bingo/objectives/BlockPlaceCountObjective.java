package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

@Tracker("block-place-count")
public class BlockPlaceCountObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> BLOCKS_TO_PLACE = useConfig("block-place-count", 1000);

  private final Supplier<Material> BLOCK = useConfig("block-place-type", Material.WORKBENCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {

    if (event.getBlock().getType().equals(BLOCK.get())) {
      Integer blocksPlaced = updateObjectiveData(event.getPlayer().getUniqueId(), i -> i + 1);

      if (blocksPlaced >= BLOCKS_TO_PLACE.get()) {
        reward(event.getPlayer());
      }
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
    return (double) data / BLOCKS_TO_PLACE.get();
  }
}
