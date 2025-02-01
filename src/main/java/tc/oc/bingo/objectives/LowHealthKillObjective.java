package tc.oc.bingo.objectives;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("low-health-kill")
public class LowHealthKillObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || event.getKiller() == null) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    Player player = killer.getBukkit();
    if (player == null) return;

    // Require player to be low
    if (player.getHealth() <= 0.5) return;
    // and have hearts for this to be impressive
    if (player.getMaxHealth() < 10) return;

    reward(player);
  }
}
