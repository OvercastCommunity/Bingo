package tc.oc.bingo.objectives;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

@Tracker("match-length")
public class MatchLengthObjective extends ObjectiveTracker {

  public static final int REQUIRED_MINS = 60;

  public MatchLengthObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {

    Duration duration = event.getMatch().getDuration();

    if (duration.minus(REQUIRED_MINS, ChronoUnit.MINUTES).isNegative()) return;

    event
        .getMatch()
        .getParticipants()
        .forEach(
            player -> {
              reward(player.getBukkit());
            });
  }
}
