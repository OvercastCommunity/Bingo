package tc.oc.bingo.util;

import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

public class CombatUtils {

  public static boolean isDirectSwordKill(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || event.getKiller() == null || event.isPredicted()) return false;

    if (!(event.getDamageInfo() instanceof MeleeInfo info)) return false;

    return info.getWeapon() instanceof ItemInfo;
  }
}
