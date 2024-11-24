package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.api.tracker.info.MeleeInfo;

@Tracker("fall-kill")
public class FallKillObjective extends ObjectiveTracker {
  private final Supplier<Integer> MIN_FALL = useConfig("min-fall-height", 24);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!(event.getDamageInfo() instanceof FallInfo info)) return;

    if (!(info.getTo() == FallInfo.To.GROUND)) return;
    if (!(info.getCause() instanceof MeleeInfo)) return;
    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null) return;

    // Check fall height
    double yPos = info.getOrigin().getY();
    double fallHeight = yPos - event.getVictim().getLocation().getY();
    if (fallHeight < MIN_FALL.get()) return;

    reward(killer.getBukkit());
  }
}
