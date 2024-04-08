package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.rotation.vote.events.MatchPlayerVoteEvent;

@Tracker("picky-voter")
public class PickyVoterObjective extends ObjectiveTracker {

  public Map<UUID, List<String>> playerVotes = useState(Scope.FULL_MATCH);

  private int minMapVotes = 4;
  private int maxMapVotes = 4;

  @Override
  public void setConfig(ConfigurationSection config) {
    minMapVotes = config.getInt("min-map-votes", 4);
    maxMapVotes = config.getInt("max-map-votes", 4);
  }

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
                  return voteCount >= minMapVotes && voteCount <= maxMapVotes;
                })
            .map(Map.Entry::getKey)
            .map(uuid -> Bukkit.getServer().getPlayer(uuid))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (!players.isEmpty()) reward(players);
  }
}
