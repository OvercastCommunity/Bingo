package tc.oc.bingo.objectives;

import static tc.oc.bingo.modules.CustomItemModule.CUSTOM_ITEM_META;

import java.util.Arrays;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

@Tracker("slot-machine")
public class SlotsObjective extends ObjectiveTracker {

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    Block block = event.getClickedBlock();
    if (block == null || block.getType() != Material.LEVER) return;

    // TODO: cooldown

    Player player = event.getPlayer();
    BlockFace face = getLeverFacing(block);
    if (face == null) return;

    BlockFace axis = getHorizontalPerpendicular(face);
    if (axis == null) return;

    // Use the directions to get item frame locations
    Vector direction = new Vector(axis.getModX(), axis.getModY(), axis.getModZ());
    Location leverLocation = block.getLocation().add(0.5, 0.5, 0.5);
    Vector end = leverLocation.toVector().add(direction);
    Vector mid = end.clone().add(direction);
    Vector start = mid.clone().add(direction);

    ItemFrame[] frames =
        Arrays.stream(new Vector[] {start, mid, end})
            .map(vec -> findItemFrame(vec, face, player.getWorld()))
            .toArray(ItemFrame[]::new);

    if (Arrays.stream(frames).anyMatch(Objects::isNull)) return;

    // Ensure all item frames contain all fruit items
    boolean allMatch =
        Arrays.stream(frames)
            .map(ItemFrame::getItem)
            .allMatch(item -> item != null && CUSTOM_ITEM_META.has(item));

    if (!allMatch) return;

    reward(player);
  }

  private ItemFrame findItemFrame(Vector vec, BlockFace facing, World world) {
    return world.getNearbyEntities(vec.toLocation(world), 0.5, 0.5, 0.5).stream()
        .filter(entity -> entity instanceof ItemFrame)
        .map(entity -> (ItemFrame) entity)
        .filter(frame -> frame.getFacing() == facing)
        .findFirst()
        .orElse(null);
  }

  private BlockFace getLeverFacing(Block block) {
    byte data = block.getData();
    return switch (data) {
      case 1 -> BlockFace.EAST;
      case 2 -> BlockFace.WEST;
      case 3 -> BlockFace.SOUTH;
      case 4 -> BlockFace.NORTH;
      default -> null;
    };
  }

  private BlockFace getHorizontalPerpendicular(BlockFace face) {
    return switch (face) {
      case NORTH -> BlockFace.EAST;
      case EAST -> BlockFace.SOUTH;
      case SOUTH -> BlockFace.WEST;
      case WEST -> BlockFace.NORTH;
      default -> null;
    };
  }
}
