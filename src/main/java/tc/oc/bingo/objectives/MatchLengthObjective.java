package tc.oc.bingo.objectives;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("match-length")
public class MatchLengthObjective extends ObjectiveTracker {

  private final Supplier<Integer> REQUIRED_MINS = useConfig("required-mins", 60);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {

    Duration duration = event.getMatch().getDuration();

    if (duration.toMinutes() < REQUIRED_MINS.get()) return;

    List<Player> players =
        event.getMatch().getParticipants().stream()
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toList());

    reward(players);
  }
}
