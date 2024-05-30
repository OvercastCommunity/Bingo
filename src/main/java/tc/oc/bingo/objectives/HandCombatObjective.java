package tc.oc.bingo.objectives;

import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("hand-combat")
public class HandCombatObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    if (event.getDamageInfo() instanceof MeleeInfo) {
      DamageInfo damageInfo = event.getDamageInfo();
      MeleeInfo info = (MeleeInfo) damageInfo;
      if (info.getWeapon() instanceof ItemInfo) {
        ItemInfo weapon = (ItemInfo) info.getWeapon();
        if (weapon.getItem().getType() == Material.AIR && isNaked(player.getBukkit())) {
          reward(player.getBukkit());
        }
      }
    }
  }

  public boolean isNaked(Player player) {
    return Arrays.stream(player.getInventory().getArmorContents())
        .allMatch(item -> item == null || item.getType() == Material.AIR);
  }
}
