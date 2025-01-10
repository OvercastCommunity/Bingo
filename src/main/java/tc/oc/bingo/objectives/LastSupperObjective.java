package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("last-supper")
public class LastSupperObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_SECONDS = useConfig("maximum-seconds", 1);

  private final Map<UUID, Long> eatenPlayers = useState(Scope.PARTICIPATION);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDamage(PlayerItemConsumeEvent event) {
    eatenPlayers.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || event.getKiller() == null) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    // TODO: maybe?
    // if (!(event.getDamageInfo() instanceof MeleeInfo info)) return;

    long thresholdTime = System.currentTimeMillis() - (MAX_SECONDS.get() * 1000L);

    Long lastEaten = eatenPlayers.get(event.getVictim().getId());
    if (lastEaten == null || lastEaten < thresholdTime) return;

    reward(killer.getBukkit());
  }
}
