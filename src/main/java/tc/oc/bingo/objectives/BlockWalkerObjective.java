package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

// author: dakups

@Tracker("block-walker")
public class BlockWalkerObjective extends ObjectiveTracker {

  // Walk `x` distance on `x` block
  // Doesn't need to be distance (from start point) just add up the coarse player move events.
  // Stepping on a different block type breaks the streak.

  private final Supplier<Material> BLOCK = useConfig("floor", Material.DIRT);
  private final Supplier<Integer> DISTANCE = useConfig("distance", 10);
  private final Map<UUID, Integer> playerWalkingDistance = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (event.getBlockTo().getBlock().getRelative(BlockFace.DOWN).getType().equals(BLOCK.get())) {
      int distance = playerWalkingDistance.compute(playerId, (uuid, i) -> i == null ? 1 : i + 1);

      if (distance >= DISTANCE.get()) {
        reward(event.getPlayer());
      }
    } else {
      playerWalkingDistance.put(playerId, 0);
    }
  }
}
