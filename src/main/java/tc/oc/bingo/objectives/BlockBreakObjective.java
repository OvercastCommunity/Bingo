package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import tc.oc.bingo.config.ConfigReader;

// Break "x" blocks of material "y", block data "z".
// Notes:
// - If "z" is left as -1, block data will not be factored into the "x" count.
// - If "y" is left as null, all block-breaks will increment towards "x", regardless of "z".
@Tracker("block-break")
public class BlockBreakObjective extends ObjectiveTracker.Stateful<Integer> {

  private static final ConfigReader<Material> MATERIAL_NAME_READER =
      (cfg, key, def) -> Material.getMaterial(cfg.getString(key));

  private final Supplier<Material> MATERIAL_REQUIRED =
      useConfig("material-name", null, MATERIAL_NAME_READER);
  private final Supplier<Integer> BLOCK_DATA_REQUIRED = useConfig("block-data", -1);

  private final Supplier<Integer> BLOCKS_TO_BREAK = useConfig("block-break-count", 1000);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if ((MATERIAL_REQUIRED.get() == null)
        || ((event.getBlock().getType().equals(MATERIAL_REQUIRED.get())
            && (BLOCK_DATA_REQUIRED.get() == -1
                || BLOCK_DATA_REQUIRED.get() == event.getBlock().getData())))) {

      Integer blocksBroken = updateObjectiveData(event.getPlayer().getUniqueId(), i -> i + 1);

      if (blocksBroken >= BLOCKS_TO_BREAK.get()) {
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
    return (double) data / BLOCKS_TO_BREAK.get();
  }
}
