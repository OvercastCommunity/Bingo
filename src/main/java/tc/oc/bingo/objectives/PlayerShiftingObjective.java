package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("player-shifting")
public class PlayerShiftingObjective extends ObjectiveTracker {

  // Due to the algorithm used, the actual max distance is twice this range
  public int radius = 4;
  public int minShifters = 4;
  public int sameTeamCount = 1;
  public int otherTeamCount = 2;

  @Override
  public void setConfig(ConfigurationSection config) {
    radius = config.getInt("radius", 4);
    sameTeamCount = config.getInt("same-team-count", 1);
    otherTeamCount = config.getInt("other-team-count", 2);
    minShifters = sameTeamCount + otherTeamCount;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    if (!event.isSneaking()) return;

    Match match = getMatch(event.getWorld());
    if (match == null) return;

    Player player = event.getPlayer();
    Collection<Player> nearbyPlayers =
        player.getWorld().getNearbyPlayers(player.getLocation(), radius);

    Collection<MatchPlayer> players =
        nearbyPlayers.stream()
            .filter(p -> p.isSneaking() || p.equals(player))
            .map(match::getPlayer)
            .filter(mp -> mp != null && mp.canInteract())
            .collect(Collectors.toList());

    if (players.size() < minShifters) return;

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
                  return sameTeamCount >= this.sameTeamCount
                      && differentTeamCount >= otherTeamCount;
                })
            .map(MatchPlayer::getBukkit)
            .collect(Collectors.toList());

    reward(shiftingPlayers);
  }
}
