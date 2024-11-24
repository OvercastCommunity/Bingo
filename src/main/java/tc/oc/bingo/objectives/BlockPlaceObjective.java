package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_READER;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

@Tracker("block-place")
public class BlockPlaceObjective extends ObjectiveTracker {

  private final Supplier<Material> BLOCK_TYPE =
      useConfig("tracked-block", Material.WALL_BANNER, MATERIAL_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (event.getBlock().getType().equals(BLOCK_TYPE.get())) {
      reward(event.getPlayer());
    }
  }
}
