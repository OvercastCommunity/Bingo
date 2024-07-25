package tc.oc.bingo.objectives;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("air-combat")
public class AirCombatObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    Player deadPlayer = event.getPlayer().getBukkit();
    if (deadPlayer.isOnGround()) return;

    if (event.getDamageInfo() instanceof MeleeInfo) {
      MeleeInfo info = (MeleeInfo) event.getDamageInfo();
      if (info.getWeapon() instanceof ItemInfo) {
        if (!player.getBukkit().isOnGround()) {
          reward(player.getBukkit());
        }
      }
    }
  }
}
