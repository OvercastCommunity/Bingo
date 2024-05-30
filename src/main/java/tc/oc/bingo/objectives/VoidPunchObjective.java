package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.FallInfo;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("void-puncher")
public class VoidPunchObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    MatchPlayer player = getStatePlayer(event.getKiller());
    if (player == null) return;

    DamageInfo damageInfo = event.getDamageInfo();
    if (!(damageInfo instanceof FallInfo)) return;

    FallInfo info = (FallInfo) damageInfo;
    if (info.getTo() != FallInfo.To.VOID) return;

    TrackerInfo fallCause = info.getCause();
    if (!(fallCause instanceof MeleeInfo)) return;

    MeleeInfo meleeInfo = (MeleeInfo) fallCause;
    if (!(meleeInfo.getWeapon() instanceof ItemInfo)) return;

    ItemInfo weapon = (ItemInfo) meleeInfo.getWeapon();
    if (weapon.getItem().getType() == Material.AIR) {
      reward(player.getBukkit());
    }
  }
}
