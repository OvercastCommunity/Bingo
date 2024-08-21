package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

@Tracker("harvester")
public class HarvesterObjective extends ObjectiveTracker {

  private final Map<UUID, Integer> cropsHarvested = useState(Scope.MATCH);

  private final Supplier<Integer> REQUIRED_CROPS = useConfig("required-crops", 32);

  private final Set<Material> cropMaterials =
      EnumSet.of(Material.CROPS, Material.CARROT, Material.POTATO, Material.NETHER_WARTS);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();

    if (!cropMaterials.contains(event.getBlock().getType()) || !isFullyGrown(event.getBlock()))
      return;

    Integer cropsCount =
        cropsHarvested.compute(
            player.getUniqueId(), (uuid, count) -> (count == null) ? 1 : count + 1);

    if (cropsCount >= REQUIRED_CROPS.get()) {
      reward(player);
    }
  }

  private boolean isFullyGrown(Block block) {
    Material material = block.getType();
    byte data = block.getData();

    return switch (material) {
      case CROPS, CARROT, POTATO -> data == 7;
      case NETHER_WARTS -> data == 3;
      default -> false;
    };
  }
}
