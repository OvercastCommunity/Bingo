package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.rotation.vote.events.MatchPlayerVoteEvent;

@Tracker("picky-voter")
public class PickyVoterObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_MAP_VOTES = useConfig("min-map-votes", 4);
  private final Supplier<Integer> MAX_MAP_VOTES = useConfig("max-map-votes", 4);

  private final Map<UUID, List<String>> playerVotes = useState(Scope.FULL_MATCH);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerVote(MatchPlayerVoteEvent event) {
    UUID playerId = event.getPlayer().getId();
    String mapId = event.getMap().getId();

    List<String> votes = playerVotes.computeIfAbsent(playerId, uuid -> new ArrayList<>());

    if (event.isAdd()) {
      votes.add(mapId);
    } else {
      votes.remove(mapId);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    List<Player> players =
        playerVotes.entrySet().stream()
            .filter(
                entry -> {
                  int voteCount = entry.getValue().size();
                  return voteCount >= MIN_MAP_VOTES.get() && voteCount <= MAX_MAP_VOTES.get();
                })
            .map(Map.Entry::getKey)
            .map(uuid -> Bukkit.getServer().getPlayer(uuid))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (!players.isEmpty()) reward(players);
  }
}
