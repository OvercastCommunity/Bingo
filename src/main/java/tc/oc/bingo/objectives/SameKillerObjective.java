package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("same-killer")
public class SameKillerObjective extends ObjectiveTracker {

  private final Map<UUID, UUID> lastKiller = useState(Scope.MATCH);
  private final Map<UUID, Integer> deathCount = useState(Scope.MATCH);

  private final Supplier<Integer> REQUIRED_DEATHS = useConfig("required-deaths", 3);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    Player player = event.getPlayer().getBukkit();
    UUID playerId = player.getUniqueId();

    if (!event.isChallengeKill() || event.isTeamKill() || event.getKiller() == null) {
      resetDeathStreak(playerId);
      return;
    }

    Player killer = player.getKiller();
    if (killer == null) {
      resetDeathStreak(playerId);
      return;
    }

    UUID killerId = killer.getUniqueId();
    UUID lastKillerId = this.lastKiller.getOrDefault(playerId, null);

    // Reset death streak if killer is different to last
    if (!killerId.equals(lastKillerId)) {
      lastKiller.put(playerId, killerId);
      deathCount.put(playerId, 1);
      return;
    }

    int newDeathCount = deathCount.getOrDefault(playerId, 0) + 1;
    deathCount.put(playerId, newDeathCount);

    if (newDeathCount >= REQUIRED_DEATHS.get()) {
      reward(player);
    }
  }

  private void resetDeathStreak(UUID player) {
    lastKiller.remove(player);
    deathCount.remove(player);
  }
}
