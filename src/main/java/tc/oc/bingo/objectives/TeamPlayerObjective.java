package tc.oc.bingo.objectives;

import com.google.common.collect.Iterables;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("team-player")
public class TeamPlayerObjective extends ObjectiveTracker.Stateful<Set<Integer>> {

  private final Supplier<Integer> REQUIRED_TEAMS = useConfig("required-teams", 5);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {

    // TODO: something for FFA, min play time? primary team?
    Duration duration = event.getMatch().getDuration();

    List<Player> rewardingPlayers =
        event.getMatch().getCompetitors().stream()
            .flatMap(
                competitor ->
                    competitor.getPlayers().stream()
                        .filter(matchPlayer -> storeColorIndex(matchPlayer, competitor)))
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toList());

    reward(rewardingPlayers);
  }

  private boolean storeColorIndex(MatchPlayer player, Competitor team) {
    if (player == null) return false;

    return updateObjectiveData(
                player.getId(),
                idx -> {
                  idx.add(team.getColor().ordinal());
                  return idx;
                })
            .size()
        >= REQUIRED_TEAMS.get();
  }

  @Override
  public @NotNull Set<Integer> initial() {
    return new HashSet<>();
  }

  @Override
  public @NotNull Set<Integer> deserialize(@NotNull String string) {
    if (string.isEmpty()) return initial();
    return Arrays.stream(string.split(",")).map(Integer::valueOf).collect(Collectors.toSet());
  }

  @Override
  public @NotNull String serialize(@NotNull Set<Integer> data) {
    return String.join(",", Iterables.transform(data, Object::toString));
  }

  @Override
  public double progress(Set<Integer> data) {
    return (double) data.size() / REQUIRED_TEAMS.get();
  }
}
