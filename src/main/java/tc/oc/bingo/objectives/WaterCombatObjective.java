package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.util.LocationUtils;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("water-combat")
public class WaterCombatObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || event.getKiller() == null) return;

    if (!(event.getDamageInfo() instanceof MeleeInfo info)) return;
    if (!(info.getWeapon() instanceof ItemInfo)) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null || !isStoodInWater(killer.getBukkit())) return;

    Player deadPlayer = event.getVictim().getBukkit();
    if (!isStoodInWater(deadPlayer)) return;

    reward(killer.getBukkit());
  }

  public boolean isStoodInWater(Player player) {
    return LocationUtils.stoodInMaterial(player.getLocation(), Material.WATER)
        || LocationUtils.stoodInMaterial(player.getLocation(), Material.STATIONARY_WATER);
  }
}
