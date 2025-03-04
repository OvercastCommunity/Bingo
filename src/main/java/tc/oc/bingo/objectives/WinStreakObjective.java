package tc.oc.bingo.objectives;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("win-streak")
public class WinStreakObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_STREAK = useConfig("required-streak", 3);

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    if (event.getMatch().getWinners().size() > 1) return;

    // Create a Collection of winners and players
    Set<Player> winners =
        event.getMatch().getWinners().stream()
            .flatMap(competitor -> competitor.getPlayers().stream())
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toSet());

    // Reset the progress of any losers
    event.getMatch().getPlayers().stream()
        .filter(player -> !winners.contains(player.getBukkit()))
        .forEach(player -> storeObjectiveData(player.getId(), 0));

    // Update the progress of any winners and reward those who meet the requirements
    trackProgress(winners);
  }

  @Override
  protected int maxValue() {
    return REQUIRED_STREAK.get();
  }
}
