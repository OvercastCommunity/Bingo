package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.projectiles.ProjectileSource;
import tc.oc.bingo.util.LocationUtils;

@Tracker("hook-em")
public class HookEmObjective extends ObjectiveTracker {

  private final Supplier<Integer> MIN_SECONDS = useConfig("min-seconds", 3);
  private final Supplier<Boolean> REQUIRE_WATER = useConfig("require-water", true);

  // Key: Hooked player UUID, Value: (Fisher UUID, Hook timestamp)
  private final Map<UUID, Map.Entry<UUID, Long>> hookTracking = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerFish(PlayerFishEvent event) {
    if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;

    if (!(event.getPlayer() instanceof Player fisher)) return;
    if (!(event.getCaught() instanceof Player hooked)) return;

    UUID hookedId = hooked.getUniqueId();
    Map.Entry<UUID, Long> hookData = hookTracking.remove(hookedId);

    if (hookData == null || !hookData.getKey().equals(fisher.getUniqueId())) return;

    long elapsedTime = (System.currentTimeMillis() - hookData.getValue()) / 1000L;
    if (elapsedTime < MIN_SECONDS.get()) return;

    if (REQUIRE_WATER.get()) {
      boolean hookedInWater =
          LocationUtils.stoodInMaterial(hooked.getLocation(), Material.STATIONARY_WATER);
      boolean fisherOutOfWater =
          !LocationUtils.stoodInMaterial(fisher.getLocation(), Material.STATIONARY_WATER);
      if (!hookedInWater || !fisherOutOfWater) return;
    }

    reward(fisher);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof FishHook fishHook)) return;

    ProjectileSource shooter = fishHook.getShooter();
    if (!(shooter instanceof Player fisher)) return;
    if (!(event.getEntity() instanceof Player hooked)) return;

    hookTracking.put(
        hooked.getUniqueId(), Map.entry(fisher.getUniqueId(), System.currentTimeMillis()));
  }
}
