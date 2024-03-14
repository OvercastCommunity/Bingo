package tc.oc.bingo.objectives;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("cobweb-killer")
public class CobwebKillerObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    Location playerLocation = player.getLocation();

    if (stoodInMaterial(playerLocation, Material.WEB)) {
      reward(player.getBukkit());
    }
  }

  public boolean stoodInMaterial(Location location, Material material) {
    // Player hit-box is 0.6 by 1.8
    double minX = Math.floor(location.getX() - 0.3);
    double maxX = Math.ceil(location.getX() + 0.3);
    double minZ = Math.floor(location.getZ() - 0.3);
    double maxZ = Math.ceil(location.getZ() + 0.3);
    double minY = Math.floor(location.getY());
    double maxY = Math.ceil(location.getY() + 1.8);

    for (double x = minX; x < maxX; x++) {
      for (double z = minZ; z < maxZ; z++) {
        for (double y = minY; y < maxY; y++) {
          Block block = location.getWorld().getBlockAt((int) x, (int) y, (int) z);
          if (block != null && block.getType() == material) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
