package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

@Tracker("boring-machine")
public class BoringObjective extends ObjectiveTracker {

  private final Map<UUID, Integer> playerTunnelDistance = useState(Scope.LIFE);
  private final Map<UUID, Boolean> playerIsTunneling = useState(Scope.LIFE);
  private final Map<UUID, Location> playerLastLocation = useState(Scope.LIFE);

  private final Supplier<Integer> REQUIRED_DISTANCE = useConfig("required-tunnel-distance", 20);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(BlockBreakEvent event) {
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

    if (!playerIsTunneling.getOrDefault(playerId, false)) {
      playerIsTunneling.put(playerId, true);
      playerLastLocation.put(playerId, player.getLocation());
      playerTunnelDistance.put(playerId, 0);
      return;
    }

    Location lastLocation = playerLastLocation.get(playerId);
    if (isStraightLine(lastLocation, player.getLocation())) {
      int distance = playerTunnelDistance.get(playerId) + 1;
      playerTunnelDistance.put(playerId, distance);
      playerLastLocation.put(playerId, player.getLocation());

      if (distance >= REQUIRED_DISTANCE.get()) {
        reward(player);
        resetPlayerProgress(playerId);
      }
    } else {
      resetPlayerProgress(playerId);
    }
  }

  private void resetPlayerProgress(UUID playerId) {
    playerIsTunneling.put(playerId, false);
    playerTunnelDistance.put(playerId, 0);
    playerLastLocation.remove(playerId);
  }

  private boolean isEnclosed(Player player) {
    Block blockAbove = player.getLocation().add(0, 1, 0).getBlock();
    if (!blockAbove.isEmpty()) return false;

    Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
    return blockBelow.isEmpty();
  }

  private boolean isStraightLine(Location from, Location to) {
    return from.getBlockX() == to.getBlockX() || from.getBlockZ() == to.getBlockZ();
  }
}
