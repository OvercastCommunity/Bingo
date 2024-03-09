package tc.oc.bingo.objectives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.tracker.info.SpleefInfo;

public class SpleeferObjective extends ObjectiveTracker {

  public SpleeferObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    if (event.getDamageInfo() instanceof FallInfo) {
      FallInfo fallInfo = (FallInfo) event.getDamageInfo();

      if (fallInfo.getCause() instanceof SpleefInfo) {
        reward(player.getBukkit());
      }
    }
  }
}
