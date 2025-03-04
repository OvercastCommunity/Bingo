package tc.oc.bingo.objectives;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

@Tracker("projectile-hitter")
public class ProjectileHitterObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_HITS = useConfig("required-hits", 50);

  private final Map<UUID, Set<UUID>> hitPlayers = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityHitByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Snowball projectile)) return;
    if (!(event.getEntity() instanceof Player victim)) return;
    if (!(projectile.getShooter() instanceof Player attacker)) return;

    // When target is not accepted to list (non-unique)
    if (!hitPlayers
        .computeIfAbsent(attacker.getUniqueId(), uuid -> new HashSet<>())
        .add(victim.getUniqueId())) return;

    trackProgress(attacker);
  }

  @Override
  protected int maxValue() {
    return REQUIRED_HITS.get();
  }
}
