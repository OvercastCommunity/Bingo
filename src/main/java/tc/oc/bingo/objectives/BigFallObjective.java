package tc.oc.bingo.objectives;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.tracker.Trackers;

@Tracker("big-fall")
public class BigFallObjective extends ObjectiveTracker {

  private int minFallHeight = 100;

  @Override
  public void setConfig(ConfigurationSection config) {
    minFallHeight = config.getInt("min-fall-height", 100);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isSuicide()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    if (event.getDamageInfo() instanceof FallInfo) {
      FallInfo fallInfo = (FallInfo) event.getDamageInfo();

      if (fallInfo.getTo() == FallInfo.To.GROUND) {
        double distance =
            Trackers.distanceFromRanged(fallInfo, event.getVictim().getBukkit().getLocation());

        if (!Double.isNaN(distance)) {
          if (distance >= minFallHeight) {
            reward(player.getBukkit());
          }
        }
      }
    }
  }
}
