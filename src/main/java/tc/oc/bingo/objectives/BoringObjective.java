package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

@Tracker("boring-machine")
public class BoringObjective extends ObjectiveTracker {

  private final Map<UUID, Predicate<Block>> playerDirection = useState(Scope.LIFE);
  private final Map<UUID, Integer> playerTunnelDistance = useState(Scope.LIFE);

  private final Supplier<Integer> REQUIRED_DISTANCE = useConfig("required-tunnel-distance", 20);

  BlockFace[] CLOCKWISE =
      new BlockFace[] {
        BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST,
      };

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (!player.isSneaking()) {
      resetPlayerProgress(playerId);
      return;
    }

    if (!isEnclosed(player)) {
      resetPlayerProgress(playerId);
      return;
    }

    Predicate<Block> blockPredicate = playerDirection.get(playerId);

    if (blockPredicate == null || !blockPredicate.test(event.getBlock())) {
      playerTunnelDistance.put(playerId, 0);

      Location location = event.getPlayer().getLocation();
      final int playerY = location.getBlockY();

      final BlockFace direction = yawToFace(event.getPlayer().getLocation().getYaw());
      final int initialX = event.getBlock().getX();
      final int initialZ = event.getBlock().getZ();

      Predicate<Block> heightCheck =
          (block -> block.getY() == playerY || block.getY() == playerY + 1);

      Predicate<Block> directionCheck =
          switch (direction) {
            case NORTH -> block -> block.getX() == initialX && block.getZ() <= initialZ;
            case EAST -> block -> block.getX() >= initialX && block.getZ() == initialZ;
            case SOUTH -> block -> block.getX() == initialX && block.getZ() >= initialZ;
            case WEST -> block -> block.getX() <= initialX && block.getZ() == initialZ;
            default -> block -> false;
          };

      playerDirection.put(playerId, heightCheck.and(directionCheck));

      return;
    }

    int distance = playerTunnelDistance.compute(playerId, (uuid, i) -> i == null ? 1 : i + 1);

    if (distance >= REQUIRED_DISTANCE.get()) {
      reward(player);
    }
  }

  private void resetPlayerProgress(UUID playerId) {
    playerDirection.put(playerId, null);
    playerTunnelDistance.put(playerId, 0);
  }

  private boolean isEnclosed(Player player) {
    Block blockAbove = player.getLocation().add(0, 2, 0).getBlock();
    if (!blockAbove.getType().isSolid()) return false;

    Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
    return blockBelow.getType().isSolid();
  }

  private BlockFace yawToFace(float yaw) {
    return CLOCKWISE[Math.round(((yaw + 360) % 360) / 90f) & 0x3];
  }
}
