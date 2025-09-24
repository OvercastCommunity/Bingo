package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import tc.oc.bingo.config.ConfigReader;

@Tracker("place-blocks-nearby")
public class PlaceBlocksNearbyObjective extends ObjectiveTracker {

  // TODO: track just latest block of each type
  private record PlacedBlock(Material type, Location loc) {}

  private final Supplier<Set<Material>> blocksAllowed =
      useConfig(
          "blocks-allowed",
          EnumSet.of(Material.CAULDRON, Material.BREWING_STAND),
          ConfigReader.MATERIAL_SET_READER);
  private final Supplier<Integer> requiredBlocks = useConfig("required-blocks", 2);
  private final Supplier<Integer> maxDistance = useConfig("max-distance", 5);

  private final Map<UUID, List<PlacedBlock>> progress = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    Block newBlock = event.getBlock();
    Material newBlockType = newBlock.getType();

    if (!blocksAllowed.get().contains(newBlockType)) return;

    List<PlacedBlock> placedByPlayer =
        progress.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
    placedByPlayer.add(new PlacedBlock(newBlockType, newBlock.getLocation()));

    Set<Material> nearbyUniqueTypes = new HashSet<>();
    nearbyUniqueTypes.add(newBlockType);

    double distanceSq = Math.pow(maxDistance.get(), 2);

    for (PlacedBlock existingBlock : placedByPlayer) {
      if (newBlock.getLocation().distanceSquared(existingBlock.loc()) <= distanceSq) {
        nearbyUniqueTypes.add(existingBlock.type());
      }
    }

    if (nearbyUniqueTypes.size() >= requiredBlocks.get()) {
      reward(player);
    }
  }
}
