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
import org.jetbrains.annotations.NotNull;

@Tracker("projectile-hitter")
public class ProjectileHitterObjective extends ObjectiveTracker.Stateful<Integer> {

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

    Integer projectilesHit = updateObjectiveData(attacker.getUniqueId(), i -> i + 1);

    if (projectilesHit >= REQUIRED_HITS.get()) {
      reward(attacker);
    }
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return Integer.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Integer data) {
    return (double) data / REQUIRED_HITS.get();
  }
}
