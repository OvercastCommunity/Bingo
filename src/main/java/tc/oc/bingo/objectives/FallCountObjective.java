package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.FallInfo;

@Tracker("fall-count")
public class FallCountObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> MIN_FALLS = useConfig("min-falls", 50);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    if (!(event.getDamageInfo() instanceof FallInfo fall)) return;

    if (fall.getTo().equals(FallInfo.To.VOID)) return;

    /*
     Ideally it should be just: `trackProgress(killer.getBukkit());`
     However, this objective is bugged in that it tracks for the victim, but rewards for the killer.
     To keep the bug we have to maintain the manual update & check
    */

    Integer count = updateObjectiveData(event.getPlayer().getId(), i -> i + 1);

    if (count >= MIN_FALLS.get()) {
      reward(killer.getBukkit());
    }
  }

  @Override
  protected int maxValue() {
    return MIN_FALLS.get();
  }
}
