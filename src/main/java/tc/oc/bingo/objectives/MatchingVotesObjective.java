package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchVoteFinishEvent;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.rotation.vote.events.MatchPlayerVoteEvent;

@Tracker("matching-votes")
public class MatchingVotesObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<Integer> PERSONAL_VOTES = useConfig("required-personal-votes", 2);
  private final Supplier<Integer> GLOBAL_VOTES = useConfig("required-total-votes", 3);

  private final Supplier<Integer> INCLUDE_TOP_MAPS = useConfig("included-top-maps", 2);

  private final Supplier<Integer> REQUIRED_COUNT = useConfig("required-count", 5);

  private final Map<UUID, Set<String>> playerVotes = useState(Scope.FULL_MATCH);
  private final Set<String> topVotes = new HashSet<>();

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerVote(MatchPlayerVoteEvent event) {
    UUID playerId = event.getPlayer().getId();
    String mapId = event.getMap().getId();

    Set<String> votes = playerVotes.computeIfAbsent(playerId, uuid -> new HashSet<>());

    if (event.isAdd()) {
      votes.add(mapId);
    } else {
      votes.remove(mapId);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchVoteFinishEvent(MatchVoteFinishEvent event) {
    topVotes.clear();

    MapOrder mapOrder = PGM.get().getMapOrder();
    if (!(mapOrder instanceof VotingPool votingPool)) return;

    MapPoll currentPoll = votingPool.getCurrentPoll();
    if (currentPoll == null) return;

    Map<MapInfo, Set<UUID>> votes = currentPoll.getVotes();

    // Require at least the min count of voters
    Set<UUID> uniqueVoters =
        votes.values().stream().flatMap(Set::stream).collect(Collectors.toSet());

    if (uniqueVoters.size() < GLOBAL_VOTES.get()) return;

    // Get top maps based on votes
    Map<MapInfo, Integer> voteCounts =
        votes.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> countVotes(entry.getValue())));

    voteCounts.entrySet().stream()
        .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
        .limit(INCLUDE_TOP_MAPS.get())
        .forEach(entry -> topVotes.add(entry.getKey().getId()));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    List<UUID> rewardingPlayers = new ArrayList<>();

    playerVotes.forEach(
        (playerId, votes) -> {
          if (votes.size() == PERSONAL_VOTES.get()
              && topVotes.containsAll(votes)) { // TODO: contains all checks which way? if extra?
            int currentCount = updateObjectiveData(playerId, i -> i + 1);
            if (currentCount >= REQUIRED_COUNT.get()) {
              rewardingPlayers.add(playerId);
            }
          }
        });

    reward(rewardingPlayers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList());
  }

  private int countVotes(Collection<UUID> uuids) {
    return uuids.stream().map(Bukkit::getPlayer).mapToInt(this::calcVoteMultiplier).sum();
  }

  private int calcVoteMultiplier(Player player) {
    if (player == null || !PGM.get().getConfiguration().allowExtraVotes()) return 1;
    int maxVotes = Math.abs(PGM.get().getConfiguration().getMaxExtraVotes());

    for (int i = maxVotes; i > 1; i--) {
      if (player.hasPermission(Permissions.EXTRA_VOTE + "." + i)) {
        return i;
      }
    }

    return player.hasPermission(Permissions.EXTRA_VOTE) ? 2 : 1;
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
    return (double) data / REQUIRED_COUNT.get();
  }
}
