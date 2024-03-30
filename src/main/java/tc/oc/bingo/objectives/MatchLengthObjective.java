package tc.oc.bingo.objectives;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("match-length")
public class MatchLengthObjective extends ObjectiveTracker {

  public int requiredMins = 60;

  // TODO: make it so they have to be in a short and long match? duality of defender?

  @Override
  public void setConfig(ConfigurationSection config) {
    requiredMins = config.getInt("required-mins", 60);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {

    Duration duration = event.getMatch().getDuration();

    if (duration.toMinutes() < requiredMins) return;

    List<Player> players =
        event.getMatch().getParticipants().stream()
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toList());

    reward(players);
  }
}
