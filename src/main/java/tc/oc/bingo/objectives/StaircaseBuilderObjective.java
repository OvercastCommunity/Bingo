package tc.oc.bingo.objectives;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.Stairs;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.regions.RegionMatchModule;

@Tracker("staircase-builder")
public class StaircaseBuilderObjective extends ObjectiveTracker {

  private final Supplier<Integer> REQUIRED_HEIGHT = useConfig("required-height", 18);

  private final Map<UUID, Set<Integer>> stairsPlaced = useState(Scope.LIFE);
  private final Map<UUID, Predicate<BlockFace>> stairDirection = useState(Scope.LIFE);

  private Integer maxBuildHeight = null;

  private static final Set<Material> STAIR_BLOCKS =
      Set.of(
          Material.WOOD_STAIRS,
          Material.COBBLESTONE_STAIRS,
          Material.BRICK_STAIRS,
          Material.SMOOTH_STAIRS,
          Material.NETHER_BRICK_STAIRS,
          Material.SANDSTONE_STAIRS,
          Material.SPRUCE_WOOD_STAIRS,
          Material.BIRCH_WOOD_STAIRS,
          Material.JUNGLE_WOOD_STAIRS,
          Material.QUARTZ_STAIRS,
          Material.ACACIA_STAIRS,
          Material.DARK_OAK_STAIRS,
          Material.RED_SANDSTONE_STAIRS);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    maxBuildHeight = null;

    RegionMatchModule module = event.getMatch().getModule(RegionMatchModule.class);
    if (module == null) return;

    Integer maxHeight = module.getMaxBuildHeight();
    if (maxHeight == null) return;

    this.maxBuildHeight = maxHeight;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlaceEvent(BlockPlaceEvent event) {
    if (maxBuildHeight == null) return;

    Block block = event.getBlock();
    Material type = block.getType();

    if (!STAIR_BLOCKS.contains(type)) return;

    if (!(block.getState().getMaterialData() instanceof Stairs stairs)) return;

    BlockFace direction = stairs.getAscendingDirection();
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    Predicate<BlockFace> directionPredicate = stairDirection.get(uuid);

    // If predicate exists and fails, reset progress
    if (directionPredicate != null && !directionPredicate.test(direction)) {
      stairsPlaced.remove(uuid);
      stairDirection.remove(uuid);
      // Set new direction predicate
      directionPredicate = dir -> dir == direction;
      stairDirection.put(uuid, directionPredicate);
    }

    // If no predicate yet, set it
    if (directionPredicate == null) {
      directionPredicate = dir -> dir == direction;
      stairDirection.put(uuid, directionPredicate);
    }

    // Record the y of this block
    stairsPlaced.computeIfAbsent(uuid, k -> new HashSet<>()).add(block.getY());
    Set<Integer> placed = stairsPlaced.get(uuid);

    // Reward if the player has placed enough stairs and reached the required height
    if (placed.size() >= REQUIRED_HEIGHT.get() && placed.contains(maxBuildHeight - 1)) {
      reward(player);
    }
  }
}
