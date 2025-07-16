package tc.oc.bingo.objectives;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.metadata.MetadataValue;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.event.BlockTransformEvent;

@Tracker("head-breaker")
public class HeadBreakerObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBlockBreak(BlockTransformEvent event) {
    if (!event.getOldState().getType().equals(Material.SKULL)) return;

    if (!(event.getActor() instanceof Player player)) return;

    Block block = event.getBlock();
    MetadataValue metadata = block.getMetadata("custom-item-id", Bingo.get());
    if (metadata == null) return;

    String customItemId = metadata.asString();
    // Close your eyes Pablo

    if (Objects.equals(customItemId, "apple")) {
      reward(player);
    }
  }
}
