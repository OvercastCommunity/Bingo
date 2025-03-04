package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("night-killer")
public class NightKillerObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> KILLS_REQUIRED = useConfig("kills-required", 100);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    long time = event.getMatch().getWorld().getTime();
    if (time < 13000 || time > 23000) return;
    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null) return;
    trackProgress(killer.getBukkit());
  }

  @Override
  protected int maxValue() {
    return KILLS_REQUIRED.get();
  }
}
