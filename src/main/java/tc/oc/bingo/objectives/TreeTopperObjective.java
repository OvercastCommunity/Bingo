package tc.oc.bingo.objectives;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;

@Tracker("tree-topper")
public class TreeTopperObjective extends ObjectiveTracker {
  private static final Set<Material> TREE_STAR_MATERIAL =
      Set.of(Material.GLOWSTONE, Material.BEACON, Material.REDSTONE_LAMP_ON, Material.SEA_LANTERN);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Block placedBlock = event.getBlock();
    if (!TREE_STAR_MATERIAL.contains(placedBlock.getType())) return;
    Block selectedBlock = placedBlock.getRelative(BlockFace.DOWN);
    // Look for spruce leaves
    boolean hasLeaves = false;
    while (isBlockSpruceLeaves(selectedBlock)) {
      hasLeaves = true;
      selectedBlock = selectedBlock.getRelative(BlockFace.DOWN);
    }
    if (!hasLeaves) return;
    // Look for spruce logs
    boolean hasLogs = false;
    while (isBlockSpruceLog(selectedBlock)) {
      hasLogs = true;
      selectedBlock = selectedBlock.getRelative(BlockFace.DOWN);
    }
    if (!hasLogs) return;
    // Check for dirt below
    if (isBlockDirt(selectedBlock)) reward(event.getPlayer());
  }

  private boolean isBlockSpruceLog(Block block) {
    MaterialData data = block.getState().getMaterialData();
    return data instanceof Tree t
        && t.getSpecies() == TreeSpecies.REDWOOD
        && t.getDirection() == BlockFace.UP
        && data.getItemType() == Material.LOG;
  }

  private boolean isBlockSpruceLeaves(Block block) {
    MaterialData data = block.getState().getMaterialData();
    return data instanceof Tree t
        && t.getSpecies() == TreeSpecies.REDWOOD
        && data.getItemType() == Material.LEAVES;
  }

  private boolean isBlockDirt(Block block) {
    return block.getState().getMaterial() == Material.DIRT
        || block.getState().getMaterial() == Material.GRASS;
  }
}
