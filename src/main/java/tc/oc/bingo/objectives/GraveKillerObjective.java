package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("beyond-the-grave")
public class GraveKillerObjective extends ObjectiveTracker {

  private final Map<UUID, Long> diedAt = useState(Scope.MATCH);

  private final Supplier<Integer> MIN_DEAD_TICKS = useConfig("min-dead-ticks", 10);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    long now = event.getMatch().getTick().tick;
    diedAt.put(event.getVictim().getId(), now);

    if (!event.isChallengeKill()) return;

    MatchPlayer matchPlayer = getPlayer(event.getKiller());
    if (matchPlayer == null || !matchPlayer.isDead()) return;

    long deadTicks = now - diedAt.getOrDefault(matchPlayer.getId(), now);

    if (deadTicks >= MIN_DEAD_TICKS.get()) {
      reward(matchPlayer.getBukkit());
    }
  }
}
