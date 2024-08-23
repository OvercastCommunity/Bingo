package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("back-stabber")
public class BackStabberObjective extends ObjectiveTracker {

  private final Supplier<Integer> MAX_DISTANCE = useConfig("max-distance", 3);
  private final Supplier<Double> MAX_HEIGHT = useConfig("max-height", 0.8);

  private final Supplier<Double> DOT_PRODUCT_MIN = useConfig("min-dot-product", 0.9);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || event.getKiller() == null) return;

    if (!(event.getDamageInfo() instanceof MeleeInfo info)) return;
    if (!(info.getWeapon() instanceof ItemInfo)) return;

    MatchPlayer killer = getPlayer(event.getKiller());
    if (killer == null) return;

    MatchPlayer victim = event.getVictim();
    if (victim == null) return;

    if (victim.getBukkit().isSprinting()) return;

    double distance = killer.getLocation().distance(event.getPlayer().getLocation());
    if (distance >= MAX_DISTANCE.get()) return;

    if (isBackStabbed(killer.getBukkit(), victim.getBukkit())) {
      reward(killer.getBukkit());
    }
  }

  private boolean isBackStabbed(Player attacker, Player victim) {
    Vector attackerDirection = attacker.getLocation().getDirection().normalize();
    Vector toVictim = victim.getLocation().getDirection().normalize();

    double dotProduct = attackerDirection.dot(toVictim);
    if (dotProduct <= DOT_PRODUCT_MIN.get()) return false;

    double heightDifference = Math.abs(attacker.getLocation().getY() - victim.getLocation().getY());
    if (heightDifference > MAX_HEIGHT.get()) return false;

    double distance = attacker.getLocation().distance(victim.getLocation());
    if (distance > MAX_DISTANCE.get()) return false;

    return true;
  }
}
