package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.FallInfo;

@Tracker("bridge-fail")
public class BridgeFailObjective extends ObjectiveTracker {

  private static final Logger log = MinecraftServer.LOGGER;

  private final Supplier<Integer> MIN_BRIDGE_LENGTH = useConfig("required-bridge-length", 5);
  private final Supplier<Integer> DEATH_TIME_WINDOW = useConfig("death-time-window", 15);

  private final Map<UUID, Predicate<Block>> bridgeDirection = useState(Scope.LIFE);
  private final Map<UUID, Integer> bridgeDistance = useState(Scope.PARTICIPATION);
  private final Map<UUID, Location> lastBlockPlaced = useState(Scope.LIFE);

  // Map to track when the player stopped sneaking (timestamps in milliseconds)
  private final Map<UUID, Long> sneakEndTimes = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (event.getPlayer() == null) return;
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (!player.isSneaking()) {
      resetPlayerProgress(playerId, null);
      return;
    }

    Predicate<Block> blockPredicate = bridgeDirection.get(playerId);
    // Check if block placed is adjacent to the last block placed
    if (event
            .getBlock()
            .getLocation()
            .distance(lastBlockPlaced.getOrDefault(playerId, event.getBlock().getLocation()))
        != 1) {
      resetPlayerProgress(playerId, event.getBlock());
      return;
    }

    // PHASE 1
    if (blockPredicate == null) {
      if (event.getBlock().getLocation().getBlockY() != lastBlockPlaced.get(playerId).getBlockY()) {
        resetPlayerProgress(playerId, event.getBlock());
        return;
      }
      final BlockFace direction =
          getPlaceDirection(event.getBlock().getLocation(), lastBlockPlaced.get(playerId));
      final int initialX = event.getBlock().getX();
      final int initialZ = event.getBlock().getZ();

      Predicate<Block> heightCheck =
          (block ->
              block.getY() == lastBlockPlaced.get(playerId).getY()
                  && block.getY() + 1 == player.getLocation().getY());

      Predicate<Block> directionCheck =
          switch (direction) {
            case NORTH -> block -> block.getX() == initialX && block.getZ() <= initialZ;
            case EAST -> block -> block.getX() >= initialX && block.getZ() == initialZ;
            case SOUTH -> block -> block.getX() == initialX && block.getZ() >= initialZ;
            case WEST -> block -> block.getX() <= initialX && block.getZ() == initialZ;
            default -> block -> false;
          };

      bridgeDirection.put(playerId, heightCheck.and(directionCheck));

    } else if (!blockPredicate.test(event.getBlock())) {
      resetPlayerProgress(playerId, event.getBlock());
    }

    bridgeDistance.compute(playerId, (uuid, i) -> i == null ? 1 : i + 1);
    lastBlockPlaced.put(playerId, event.getBlock().getLocation());
  }

  // PHASE 3
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    if (event.isSneaking()) return; // We only care about un-sneaking
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    // Check if the player has built a sufficient length of bridge
    if (bridgeDistance.getOrDefault(playerId, 0) < MIN_BRIDGE_LENGTH.get()) return;

    // Record the current time as the moment the player stops sneaking
    sneakEndTimes.put(playerId, System.currentTimeMillis());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    MatchPlayer player = event.getPlayer();
    UUID playerId = player.getId();

    // Ignore if it's not a void fall or if the player was killed by someone else
    if (event.isChallengeKill() || event.getKiller() != null) return;
    if (!(event.getDamageInfo() instanceof FallInfo)) return;
    if (((FallInfo) event.getDamageInfo()).getTo() != FallInfo.To.VOID) return;

    // Check if the player died within the death time window after unsneaking
    Long sneakEndTime = sneakEndTimes.get(playerId);
    if (sneakEndTime != null) {
      long timeSinceSneakEnd = System.currentTimeMillis() - sneakEndTime;
      if (timeSinceSneakEnd <= DEATH_TIME_WINDOW.get() * 1000) {
        reward(player.getBukkit()); // Reward the player for failing the bridge
      }
    }
  }

  private BlockFace getPlaceDirection(Location pos1, Location pos2) {
    int x1 = pos1.getBlockX();
    int z1 = pos1.getBlockZ();
    int x2 = pos2.getBlockX();
    int z2 = pos2.getBlockZ();
    if (x1 == x2) {
      if (z1 > z2) return BlockFace.SOUTH;
      else return BlockFace.NORTH;
    } else {
      if (x1 > x2) return BlockFace.EAST;
      else return BlockFace.WEST;
    }
  }

  private void resetPlayerProgress(UUID playerId, @Nullable Block block) {
    if (block != null) lastBlockPlaced.put(playerId, block.getLocation());
    else lastBlockPlaced.remove(playerId);
    bridgeDirection.put(playerId, null);
    bridgeDistance.put(playerId, 0);
  }
}
