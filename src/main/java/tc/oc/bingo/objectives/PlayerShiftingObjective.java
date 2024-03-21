package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  public int minRange = 8;
  public int minShifters = 4;
  public int sameTeamCount = 1;
  public int otherTeamCount = 2;

  @Override
  public void setConfig(ConfigurationSection config) {
    minRange = config.getInt("min-range", 8);
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
        player.getWorld().getNearbyPlayers(player.getLocation(), minRange);

    Collection<MatchPlayer> players =
        nearbyPlayers.stream()
            .filter(
                player1 -> {
                  return player1.isSneaking() || player1.equals(player);
                })
            .map(match::getPlayer)
            .filter(
                obj -> {
                  return Objects.nonNull(obj) && obj.canInteract();
                })
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
