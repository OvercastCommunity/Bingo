package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.Bed;
import org.bukkit.util.Vector;

@Tracker("bed-placement")
public class BedPlacementObjective extends ObjectiveTracker {

  private final Map<UUID, Vector> playerBeds = useState(Scope.MATCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (event.getBlockPlaced().getType() != Material.BED) return;

    if (event.getBlock() instanceof Bed bed) {
      BlockFace facing = bed.getFacing();
    }

    Player player = event.getPlayer();
    Vector bedLocation = event.getBlockPlaced().getLocation().toVector();
    UUID playerId = player.getUniqueId();

    // Store the player's bed location
    playerBeds.put(playerId, bedLocation);
  }
}
