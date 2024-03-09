package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.tracker.info.BlockInfo;

public class AnvilKillerObjective extends ObjectiveTracker {

  public AnvilKillerObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    if (event.getDamageInfo() instanceof BlockInfo) {
      final Material material = ((BlockInfo) event.getDamageInfo()).getMaterial().getItemType();
      if (material == Material.ANVIL) {
        reward(player.getBukkit());
      }
    }
  }
}
