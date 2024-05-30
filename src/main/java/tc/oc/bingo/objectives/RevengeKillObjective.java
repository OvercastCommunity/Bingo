package tc.oc.bingo.objectives;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("revenge-kill")
public class RevengeKillObjective extends ObjectiveTracker {

  private final Map<UUID, Set<UUID>> playerKillTracker = useState(Scope.MATCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    UUID victimId = event.getVictim().getId();

    Set<UUID> victimKills = playerKillTracker.remove(victimId);

    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null) return;

    Set<UUID> killerKills = playerKillTracker.computeIfAbsent(killer.getId(), k -> new HashSet<>());
    killerKills.add(victimId);

    if (victimKills != null && victimKills.contains(killer.getId())) {
      reward(killer.getBukkit());
    }
  }
}
