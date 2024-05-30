package tc.oc.bingo.objectives;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

@Tracker("flaming-arrow")
public class FlamingArrowObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onProjectileHit(ProjectileHitEvent event) {
    if (!(event.getEntity() instanceof Arrow)) return;

    ProjectileSource shooter = event.getEntity().getShooter();
    if (!(shooter instanceof Player)) return;

    if (event.getEntity().getFireTicks() > 0) {
      reward(((Player) shooter).getPlayer());
    }
  }
}
