package tc.oc.bingo.objectives;

import static org.bukkit.Bukkit.getServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.rotation.vote.events.MatchPlayerVoteEvent;

@Tracker("picky-voter")
public class PickyVoterObjective extends ObjectiveTracker {

  // TODO: change all hash maps to map
  public Map<UUID, List<String>> playerVotes = new HashMap<>();

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

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(MatchLoadEvent event) {
    List<Player> uuids =
        playerVotes.entrySet().stream()
            .filter(
                entry -> {
                  int voteCount = entry.getValue().size();
                  return voteCount >= minMapVotes && voteCount <= maxMapVotes;
                })
            .map(Map.Entry::getKey)
            .map(uuid -> getServer().getPlayer(uuid))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    playerVotes.clear();

    if (!uuids.isEmpty()) reward(uuids);
  }
}
