package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.util.Vector;

@Tracker("candle-lit")
public class CandleLitObjective extends ObjectiveTracker {

  private final Map<UUID, Vector> placedBlocks = useState(Scope.LIFE);
  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 5);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (event.getBlock().getType() != Material.TORCH) return;

    placedBlocks.put(event.getPlayer().getUniqueId(), event.getBlock().getLocation().toVector());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (!placedBlocks.containsKey(playerId)) return;

    Vector torchLocation = placedBlocks.get(playerId);
    Vector playerLocation = player.getLocation().toVector();

    // Check if the player is within the specified distance of the torch
    if (playerLocation.isInSphere(torchLocation, MAX_DISTANCE.get())) {
      reward(player);
    }
  }
}
