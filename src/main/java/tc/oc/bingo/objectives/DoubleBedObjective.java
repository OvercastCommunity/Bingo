package tc.oc.bingo.objectives;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.Bed;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.block.BlockVectors;

@Tracker("double-bed")
public class DoubleBedObjective extends ObjectiveTracker {

  private final Map<UUID, BlockVector> bedOwners = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {

    Player player = event.getPlayer();
    Block bed = event.getBlock();

    // Check if placedBlock is a bed
    if (bed.getType() != Material.BED_BLOCK) return;
    BlockFace direction = getBedDirection(bed);
    if (direction == BlockFace.DOWN) return;
    var sideways =
        direction == BlockFace.EAST || direction == BlockFace.WEST
            ? BlockFace.NORTH
            : BlockFace.EAST;

    Player a = getBedOwner(bed.getRelative(sideways), direction);
    Player b = getBedOwner(bed.getRelative(sideways.getOppositeFace()), direction);

    if (a != null || b != null) {
      List<Player> toReward = new ArrayList<>();
      toReward.add(player);
      if (a != null) toReward.add(a);
      if (b != null) toReward.add(b);
      reward(toReward);
    }
    bedOwners.put(player.getUniqueId(), BlockVectors.position(bed.getState()));
  }

  public BlockFace getBedDirection(Block block) {
    if (block.getState().getMaterialData() instanceof Bed b) {
      return b.getFacing();
    }
    return BlockFace.DOWN;
  }

  public Player getBedOwner(Block block, BlockFace direction) {
    if (block.getType() != Material.BED_BLOCK) return null;
    if (direction != getBedDirection(block)) return null;
    BlockVector vector = BlockVectors.position(block.getState());
    for (var entry : bedOwners.entrySet()) {
      if (entry.getValue().equals(vector)) {
        return Bukkit.getPlayer(entry.getKey());
      }
    }
    return null;
  }
}
