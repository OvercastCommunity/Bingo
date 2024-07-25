package tc.oc.bingo.objectives;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.stats.StatsMatchModule;

@Tracker("playtime")
public class PlaytimeObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> REQUIRED_MINS = useConfig("required-mins", 120);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {
    StatsMatchModule statsMatchModule = event.getMatch().needModule(StatsMatchModule.class);
    if (statsMatchModule == null) return;

    List<Player> rewardingPlayers =
        event.getMatch().getPlayers().stream()
            .filter(
                matchPlayer -> {
                  int timePlayed =
                      (int) statsMatchModule.getPlayerStat(matchPlayer).getTimePlayed().toMinutes();
                  Integer totalTimePlayed =
                      updateObjectiveData(matchPlayer.getId(), i -> i + timePlayed);
                  return totalTimePlayed >= REQUIRED_MINS.get();
                })
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toList());

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
    return (double) data / REQUIRED_MINS.get();
  }
}
