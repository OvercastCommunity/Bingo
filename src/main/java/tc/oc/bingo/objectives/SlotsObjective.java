package tc.oc.bingo.objectives;

import java.util.Arrays;
import java.util.Objects;
import org.bukkit.ChatColor;
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
import org.bukkit.inventory.ItemStack;
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

    // Ensure all item frames contain all fruit items (currently only checks for same type)
    ItemStack first = frames[0].getItem();
    if (first == null || first.getType() == Material.AIR) return;

    boolean allMatch =
        Arrays.stream(frames)
            .map(ItemFrame::getItem)
            .allMatch(item -> item != null && item.getType() == first.getType());

    if (!allMatch) return;

    // Success!
    player.sendMessage(ChatColor.GOLD + "You activated the slot machine!");
    reward(player);
  }

  private ItemFrame findItemFrame(Vector vec, BlockFace facing, World world) {
    return world.getNearbyEntities(vec.toLocation(world), 0.5, 0.5, 0.5).stream()
        .filter(entity -> entity instanceof ItemFrame)
        .map(entity -> (ItemFrame) entity)
        .filter(frame -> frame.getFacing() == facing.getOppositeFace())
        .findFirst()
        .orElse(null);
  }

  private BlockFace getLeverFacing(Block block) {
    byte data = block.getData();
    return switch (data) {
      case 1 -> BlockFace.WEST;
      case 2 -> BlockFace.EAST;
      case 3 -> BlockFace.NORTH;
      case 4 -> BlockFace.SOUTH;
      default -> null;
    };
  }

  private BlockFace getHorizontalPerpendicular(BlockFace face) {
    return switch (face) {
      case NORTH, SOUTH -> BlockFace.EAST;
      case EAST, WEST -> BlockFace.NORTH;
      default -> null;
    };
  }
}
