package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.util.LocationUtils;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("cobweb-killer")
public class CobwebKillerObjective extends ObjectiveTracker {

  private final Supplier<Boolean> CHECK_ATTACKER = useConfig("check-attacker", true);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    // Gets either the attacker or the victim
    MatchPlayer relevantPlayer =
        CHECK_ATTACKER.get() ? getStatePlayer(event.getKiller()) : event.getVictim();
    if (relevantPlayer == null) return;

    Location playerLocation = relevantPlayer.getLocation();

    // TODO: get killed whilst in a cobweb? versus kill someone in a cobweb
    if (LocationUtils.stoodInMaterial(playerLocation, Material.WEB)) {
      reward(relevantPlayer.getBukkit());
    }
  }
}
