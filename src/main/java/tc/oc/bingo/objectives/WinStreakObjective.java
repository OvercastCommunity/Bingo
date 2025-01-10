package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("win-streak")
public class WinStreakObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> REQUIRED_STREAK = useConfig("required-streak", 3);

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {

    // Create a Collection of winners and players
    Collection<MatchPlayer> players = event.getMatch().getPlayers();
    Collection<MatchPlayer> winners =
        event.getMatch().getWinners().stream()
            .flatMap(competitor -> competitor.getPlayers().stream())
            .toList();

    // Reset the progress of any losers
    players.stream()
        .filter(player -> !winners.contains(player))
        .forEach(player -> storeObjectiveData(player.getId(), 0));

    // Update the progress of any winners and reward those who meet the requirements
    List<Player> rewardingPlayers =
        winners.stream()
            .filter(
                player -> updateObjectiveData(player.getId(), i -> i + 1) >= REQUIRED_STREAK.get())
            .map(MatchPlayer::getBukkit)
            .toList();

    reward(rewardingPlayers);
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return Integer.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Integer data) {
    return (double) data / REQUIRED_STREAK.get();
  }
}
