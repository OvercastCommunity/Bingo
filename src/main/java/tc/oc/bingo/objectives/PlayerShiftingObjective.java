package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("player-shifting")
public class PlayerShiftingObjective extends ObjectiveTracker {

  public static final int MAX_RANGE = 8;
  public static final int MIN_SHIFTERS = 4;

  public static final int SAME_TEAM_COUNT = 1;
  public static final int DIFF_TEAM_COUNT = 2;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    if (!event.isSneaking()) return;

    Match match = getMatch(event.getWorld());
    if (match == null) return;

    Player player = event.getPlayer();
    Collection<Player> nearbyPlayers =
        player.getWorld().getNearbyPlayers(player.getLocation(), MAX_RANGE);

    Collection<MatchPlayer> players =
        nearbyPlayers.stream()
            .filter(Player::isSneaking)
            .map(match::getPlayer)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (players.size() < MIN_SHIFTERS) return;

    // Create map for count of each team's players
    Map<Competitor, Long> teamCounts =
        players.stream()
            .filter(p -> p.getCompetitor() != null)
            .collect(Collectors.groupingBy(MatchPlayer::getCompetitor, Collectors.counting()));

    List<Player> shiftingPlayers =
        players.stream()
            .filter(
                mp -> {
                  Competitor playerTeam = mp.getCompetitor();
                  long sameTeamCount = teamCounts.getOrDefault(playerTeam, 0L) - 1;
                  long differentTeamCount = players.size() - sameTeamCount - 1;
                  return sameTeamCount >= SAME_TEAM_COUNT && differentTeamCount >= DIFF_TEAM_COUNT;
                })
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toList());

    reward(shiftingPlayers);
  }
}
