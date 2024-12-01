package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("vehicle-kill")
public class VehicleKillObjective extends ObjectiveTracker {

  private final Supplier<Boolean> CHECK_ATTACKER = useConfig("check-attacker", true);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer killer = getStatePlayer(event.getKiller());
    if (killer == null) return;

    // Gets either the attacker or the victim
    MatchPlayer relevantPlayer = CHECK_ATTACKER.get() ? killer : event.getVictim();
    if (relevantPlayer == null) return;

    if (relevantPlayer.getBukkit().isInsideVehicle()) {
      reward(killer.getBukkit());
    }
  }
}
