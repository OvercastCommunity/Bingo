package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.tracker.Trackers;

@Tracker("big-fall")
public class BigFallObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_FALL_HEIGHT = useConfig("min-fall-height", 100);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!(event.getKiller() == null || event.isKiller(event.getPlayer()))) return;

    MatchPlayer player = event.getPlayer();
    if (player == null) return;

    if (event.getDamageInfo() instanceof FallInfo) {
      FallInfo fallInfo = (FallInfo) event.getDamageInfo();

      if (fallInfo.getTo() == FallInfo.To.GROUND) {
        double distance =
            Trackers.distanceFromRanged(fallInfo, event.getVictim().getBukkit().getLocation());

        if (!Double.isNaN(distance)) {
          if (distance >= MIN_FALL_HEIGHT.get()) {
            reward(player.getBukkit());
          }
        }
      }
    }
  }
}
