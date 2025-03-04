package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

@Tracker("block-place-count")
public class BlockPlaceCountObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> BLOCKS_TO_PLACE = useConfig("block-place-count", 1000);

  private final Supplier<Material> BLOCK = useConfig("block-place-type", Material.WORKBENCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (event.getBlock().getType().equals(BLOCK.get())) {
      trackProgress(event.getPlayer());
    }
  }

  @Override
  protected int maxValue() {
    return BLOCKS_TO_PLACE.get();
  }
}
