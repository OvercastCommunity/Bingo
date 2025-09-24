package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

@Tracker("grave-death")
public class GraveDeathObjective extends ObjectiveTracker {

  private static final EnumSet<Material> GRAVE_BLOCKS =
      EnumSet.of(Material.DIRT, Material.STONE, Material.GRASS);
  private final Supplier<Double> requiredPercentage = useConfig("required-percentage", 0.5);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    Location loc = player.getLocation();

    if (!isGrave(loc)) return;

    reward(player);
  }

  private boolean isGrave(Location loc) {
    Block feetBlock = loc.getBlock();
    Block headBlock = feetBlock.getRelative(0, 1, 0);

    if (!feetBlock.isEmpty() || !headBlock.isEmpty()) {
      return false;
    }

    int graveBlocks = 0;
    int totalBlocks = 0;

    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 2; y++) {
        for (int z = -1; z <= 1; z++) {
          if (x == 0 && z == 0 && (y == 0 || y == 1)) {
            continue; // Skip the 2x1 hole itself
          }

          Block relative = loc.getBlock().getRelative(x, y, z);
          if (relative.getType() != Material.AIR) {
            totalBlocks++;
            if (GRAVE_BLOCKS.contains(relative.getType())) {
              graveBlocks++;
            }
          }
        }
      }
    }

    if (totalBlocks == 0) return false;

    return (double) graveBlocks / totalBlocks >= requiredPercentage.get();
  }
}
