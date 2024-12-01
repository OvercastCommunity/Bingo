package tc.oc.bingo.objectives;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@Tracker("snowball-fight")
public class SnowballFightObjective extends ObjectiveTracker {

  private final Map<UUID, UUID> snowballFight = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityHitByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Snowball snowball)) return;
    if (!(event.getEntity() instanceof Player victim)) return;
    if (!(snowball.getShooter() instanceof Player attacker)) return;

    UUID victimId = victim.getUniqueId();
    UUID attackerId = attacker.getUniqueId();
    UUID lastHit = snowballFight.get(victimId);

    if (lastHit != null && lastHit.equals(attackerId)) {
      reward(List.of(attacker, victim));
      snowballFight.remove(victimId);
      return;
    }

    snowballFight.put(attackerId, victimId);
  }
}
