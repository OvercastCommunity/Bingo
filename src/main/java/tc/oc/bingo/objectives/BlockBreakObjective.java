package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

// Break "x" blocks of material "y", block data "z".
// Notes:
// - If "z" is left as -1, block data will not be factored into the "x" count.
// - If "y" is left as null, all block-breaks will increment towards "x", regardless of "z".
@Tracker("block-break")
public class BlockBreakObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Material> MATERIAL_REQUIRED = useConfig("material-name", (Material) null);
  private final Supplier<Integer> BLOCK_DATA_REQUIRED = useConfig("block-data", -1);

  private final Supplier<Integer> BLOCKS_TO_BREAK = useConfig("block-break-count", 1000);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    var block = event.getBlock();
    var mat = MATERIAL_REQUIRED.get();
    var bd = BLOCK_DATA_REQUIRED.get();
    if (mat == null || (block.getType().equals(mat) && (bd == -1 || bd == block.getData()))) {
      trackProgress(event.getPlayer());
    }
  }

  @Override
  protected int maxValue() {
    return BLOCKS_TO_BREAK.get();
  }
}
