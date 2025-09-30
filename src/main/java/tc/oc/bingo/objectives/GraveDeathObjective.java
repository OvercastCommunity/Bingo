package tc.oc.bingo.objectives;

import java.util.EnumSet;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("grave-death")
public class GraveDeathObjective extends ObjectiveTracker {

  private static final EnumSet<Material> GRAVE_BLOCKS =
      EnumSet.of(Material.DIRT, Material.STONE, Material.GRASS);

  private final Supplier<Double> REQUIRED_PERCENTAGE = useConfig("required-percentage", 0.75);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer victim = event.getVictim();
    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    // Require the killer to be above the victim
    Player killerBukkit = killer.getBukkit();
    if (killerBukkit.getLocation().getY() < victim.getLocation().getY() + 0.4) return;

    if (!isGrave(victim.getLocation())) return;

    reward(victim.getBukkit());
  }

  private boolean isGrave(Location loc) {
    // Adjust location if not standing on top of a block
    Block feetBlock = loc.getBlock();
    if (!feetBlock.getType().isSolid() && (loc.getY() % 1) < 0.45) {
      Block blockBelow = loc.subtract(0, 0.5, 0).getBlock();
      if (!blockBelow.getType().isSolid()) {
        feetBlock = blockBelow;
      }
    }

    Block headBlock = feetBlock.getRelative(0, 1, 0);

    if (feetBlock.getType().isSolid() || headBlock.getType().isSolid()) {
      return false;
    }

    int graveBlocks = 0;

    int totalBlocks = (3 * 4 * 3) - 2; // 3x4x3 area minus the 2 blocks of the hole

    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 2; y++) {
        for (int z = -1; z <= 1; z++) {
          if (x == 0 && z == 0 && y >= 0) {
            continue; // Skip the 2x1 hole itself
          }

          Block relative = feetBlock.getRelative(x, y, z);
          if (!relative.getType().isSolid()) return false;

          if (GRAVE_BLOCKS.contains(relative.getType())) {
            graveBlocks++;
          }
        }
      }
    }

    return (double) graveBlocks / totalBlocks >= REQUIRED_PERCENTAGE.get();
  }
}
