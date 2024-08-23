package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.regions.Bounds;

@Tracker("portal-lighter")
public class PortalLighterObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_HEIGHT = useConfig("min-height", 3);
  private final Supplier<Integer> MAX_HEIGHT = useConfig("max-height", 3);

  private final Supplier<Integer> MIN_WIDTH = useConfig("min-width", 2);
  private final Supplier<Integer> MAX_WIDTH = useConfig("max-width", 2);

  @EventHandler
  public void onPlayerLightPortal(PlayerInteractEvent event) {
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK
        && event.getItem() != null
        && event.getItem().getType() == Material.FLINT_AND_STEEL) {

      Block clickedBlock = event.getClickedBlock();
      if (clickedBlock == null || clickedBlock.getType() != Material.OBSIDIAN) return;

      if (checkPortalStructure(clickedBlock, event.getBlockFace())) {
        reward(event.getPlayer());
      }
    }
  }

  private boolean isPortalBlock(Block block) {
    return block.getType() == Material.OBSIDIAN;
  }

  private boolean checkPortalStructure(Block clickedBlock, BlockFace blockFace) {
    // Get the block adjacent to the clicked face
    Block adjacentAirBlock = clickedBlock.getRelative(blockFace);

    // Check if the adjacent block is air
    if (adjacentAirBlock.getType() != Material.AIR) return false;

    // Check directions, down, up for frame blocks with air
    PortalCheck downCheck = checkPortalFrame(adjacentAirBlock, BlockFace.DOWN, MAX_HEIGHT.get());
    if (downCheck == null) return false;

    PortalCheck upCheck =
        checkPortalFrame(adjacentAirBlock, BlockFace.UP, MAX_HEIGHT.get() - downCheck.length);
    if (upCheck == null) return false;

    if ((downCheck.length + upCheck.length + 1) < MIN_HEIGHT.get()) return false;

    // Check both potential axis directions for the portal
    BlockFace[] directions = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST};
    for (BlockFace direction : directions) {
      PortalCheck directionCheck = checkPortalFrame(adjacentAirBlock, direction, MAX_WIDTH.get());
      if (directionCheck == null) continue;

      PortalCheck altDirectionCheck =
          checkPortalFrame(
              adjacentAirBlock,
              direction.getOppositeFace(),
              MAX_WIDTH.get() - directionCheck.length);
      if (altDirectionCheck == null) continue;

      if ((directionCheck.length + altDirectionCheck.length + 1) < MIN_WIDTH.get()) return false;

      Bounds boundingBox =
          getBoundingBox(
              downCheck.block.getLocation().toVector(),
              upCheck.block.getLocation().toVector(),
              directionCheck.block.getLocation().toVector(),
              altDirectionCheck.block.getLocation().toVector());

      Vector min = boundingBox.getMin();
      Vector max = boundingBox.getMax();
      World world = clickedBlock.getWorld();

      // Check the center of the portal is clear
      for (int x = (int) min.getX(); x <= max.getX(); x++) {
        for (int y = (int) min.getY(); y <= max.getY(); y++) {
          for (int z = (int) min.getZ(); z <= max.getZ(); z++) {
            Block blockLocation = world.getBlockAt(x, y, z);
            if (!blockLocation.isEmpty() && blockLocation.getType() != Material.FIRE) return false;
          }
        }
      }

      // Check the top and bottom portal frame blocks
      for (int x = (int) min.getX(); x <= max.getX(); x++) {
        for (int z = (int) min.getZ(); z <= max.getZ(); z++) {
          if (!isPortalBlock(world.getBlockAt(x, (int) min.getY() - 1, z))) return false;
          if (!isPortalBlock(world.getBlockAt(x, (int) max.getY() + 1, z))) return false;
        }
      }

      // Check the edges expanding the coordinates depending on the direction
      for (int y = (int) min.getY(); y <= max.getY(); y++) {
        if (direction == BlockFace.NORTH) {
          if (!isPortalBlock(world.getBlockAt((int) min.getX(), y, (int) min.getZ() - 1)))
            return false;
          if (!isPortalBlock(world.getBlockAt((int) max.getX(), y, (int) max.getZ() + 1)))
            return false;
        } else if (direction == BlockFace.EAST) {
          if (!isPortalBlock(world.getBlockAt((int) min.getX() - 1, y, (int) min.getZ())))
            return false;
          if (!isPortalBlock(world.getBlockAt((int) max.getX() + 1, y, (int) min.getZ())))
            return false;
        }
      }

      return true;
    }

    return false;
  }

  public Bounds getBoundingBox(
      Vector downVector, Vector upVector, Vector directionVector, Vector altDirectionVector) {
    Vector minVector =
        new Vector(
            Math.min(directionVector.getX(), altDirectionVector.getX()),
            downVector.getY(),
            Math.min(directionVector.getZ(), altDirectionVector.getZ()));

    Vector maxVector =
        new Vector(
            Math.max(directionVector.getX(), altDirectionVector.getX()),
            upVector.getY(),
            Math.max(directionVector.getZ(), altDirectionVector.getZ()));

    return new Bounds(minVector, maxVector);
  }

  private PortalCheck checkPortalFrame(Block startBlock, BlockFace direction, int maxLength) {
    Block currentBlock = startBlock;
    int portalHeight = 0;

    // Traverse in the specified direction to the maximum length
    for (int i = 0; i < maxLength; i++) {
      Block nextBlock = currentBlock.getRelative(direction);

      boolean isPortal = isPortalBlock(nextBlock);
      boolean isAir = nextBlock.isEmpty();

      // If neither a portal nor air block is found
      // return null (invalid portal frame)
      if (!(isPortal || isAir)) {
        return null;
      }

      // If a portal block is found, stop and return the current block
      if (isPortal) return new PortalCheck(currentBlock, portalHeight);

      currentBlock = nextBlock;
      portalHeight++;
    }

    return null;
  }

  public static class PortalCheck {

    public final Block block;
    public final int length;

    public PortalCheck(Block currentBlock, int length) {

      this.block = currentBlock;
      this.length = length;
    }
  }
}
