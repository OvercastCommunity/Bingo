package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("kill-assist-streak")
public class KillAssistStreakObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_ASSIST_COUNT = useConfig("min-assist-count", 5);

  private final Map<UUID, Integer> assistCount = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    // Reset streak if the player gets a kill
    ParticipantState killer = event.getKiller();
    if (killer != null) assistCount.remove(killer.getId());

    ParticipantState assister = event.getAssister();
    if (assister == null) return;

    MatchPlayer matchPlayer = getPlayer(assister);
    if (matchPlayer == null) return;

    Integer assists =
        assistCount.compute(matchPlayer.getId(), (uuid, count) -> (count == null) ? 1 : count + 1);

    if (assists >= MIN_ASSIST_COUNT.get()) {
      reward(matchPlayer.getBukkit());
    }
  }
}
