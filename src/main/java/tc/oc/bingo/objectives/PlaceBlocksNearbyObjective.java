package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;
import tc.oc.bingo.config.ConfigReader;

@Tracker("place-blocks-nearby")
public class PlaceBlocksNearbyObjective extends ObjectiveTracker {

  private final Supplier<Set<Material>> blocksAllowed =
      useConfig(
          "blocks-allowed",
          EnumSet.of(Material.CAULDRON, Material.BREWING_STAND),
          ConfigReader.MATERIAL_SET_READER);

  private final Supplier<Integer> REQUIRED_BLOCKS = useConfig("required-blocks", 2);
  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 5);

  private final Supplier<Integer> MAX_DISTANCE_SQR =
      useComputedConfig(() -> MAX_DISTANCE.get() * MAX_DISTANCE.get());

  // Track last placed location and types placed in the current sequence
  private final Map<UUID, Vector> lastPlacedLocation = useState(Scope.LIFE);
  private final Map<UUID, Set<String>> placedBlocks = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    Block newBlock = event.getBlock();
    Material newBlockType = newBlock.getType();

    if (!blocksAllowed.get().contains(newBlockType)) return;

    UUID playerId = player.getUniqueId();
    Vector newLocation = newBlock.getLocation().toVector();

    Vector lastLocation = lastPlacedLocation.get(playerId);
    Set<String> blocksSoFar = placedBlocks.computeIfAbsent(playerId, k -> new HashSet<>());

    // If last location exists, check distance
    if (lastLocation != null
        && newLocation.distanceSquared(lastLocation) > MAX_DISTANCE_SQR.get()) {
      blocksSoFar.clear();
    }

    // Record this block type and location
    blocksSoFar.add(newBlockType.name());
    lastPlacedLocation.put(playerId, newLocation);

    // Check if objective is complete
    if (blocksSoFar.size() >= REQUIRED_BLOCKS.get()) {
      reward(player);
      blocksSoFar.clear();
      lastPlacedLocation.remove(playerId);
    }
  }
}
