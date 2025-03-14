package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

@Tracker("nerd-pole")
public class NerdPoleObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_HEIGHT = useConfig("min-height", 20);

  private final Map<UUID, Vector> lastPlacedBlock = useState(Scope.LIFE);
  private final Map<UUID, Integer> playerBuildHeight = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    // Check if the player is looking down
    if (!(player.getLocation().getPitch() > 60)) {
      resetProgress(playerId);
      return;
    }

    Block placedBlock = event.getBlock();
    Vector lastBlock = lastPlacedBlock.get(playerId);

    // Ensure the block is directly above the last placed block or starting fresh
    if (lastBlock == null
        || placedBlock.getX() == lastBlock.getX()
            && placedBlock.getZ() == lastBlock.getZ()
            && placedBlock.getY() == lastBlock.getY() + 1) {

      lastPlacedBlock.put(playerId, placedBlock.getLocation().toVector());
      int height = playerBuildHeight.compute(playerId, (uuid, h) -> h == null ? 1 : h + 1);

      // Reward if they reach the required height
      if (height >= MIN_HEIGHT.get()) {
        reward(player);
      }
    } else {
      resetProgress(playerId);
    }
  }

  public void resetProgress(UUID playerId) {
    lastPlacedBlock.remove(playerId);
    playerBuildHeight.remove(playerId);
  }
}
