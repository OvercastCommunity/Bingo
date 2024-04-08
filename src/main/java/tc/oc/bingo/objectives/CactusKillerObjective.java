package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.tracker.info.BlockInfo;

@Tracker("cactus-killer")
public class CactusKillerObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer player = getStatePlayer(event.getKiller());
    if (player == null) return;

    if (event.getDamageInfo() instanceof BlockInfo) {
      final Material material = ((BlockInfo) event.getDamageInfo()).getMaterial().getItemType();
      if (material == Material.CACTUS) {
        reward(player.getBukkit());
      }
    }
  }
}
