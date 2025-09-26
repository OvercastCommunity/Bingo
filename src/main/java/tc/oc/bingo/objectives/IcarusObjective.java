package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("icarus-height")
public class IcarusObjective extends ObjectiveTracker {

  private final Supplier<Double> MIN_VERTICAL_VELOCITY = useConfig("min-vertical-velocity", 6d);

  // This delay may be affected by ping, so keep configurable
  private final Supplier<Integer> DELAY_TICKS = useConfig("delay-ticks", 2);

  private Match match;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    match = event.getMatch();
  }

  @EventHandler
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (match == null) return;

    if (event.getDamager() instanceof TNTPrimed && event.getEntity() instanceof Player player) {
      MatchPlayer matchPlayer = match.getPlayer(player);
      if (matchPlayer == null || !matchPlayer.isParticipating()) return;

      // Schedule a task to get player's velocity a few ticks later
      Bukkit.getServer()
          .getScheduler()
          .runTaskLater(
              Bingo.get(),
              () -> {
                if (matchPlayer.isDead()) return;

                Vector velocity = player.getVelocity();
                if (velocity.getY() >= MIN_VERTICAL_VELOCITY.get()) {
                  reward(player);
                }
              },
              DELAY_TICKS.get());
    }
  }
}
