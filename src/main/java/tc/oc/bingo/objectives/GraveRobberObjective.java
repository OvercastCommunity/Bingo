package tc.oc.bingo.objectives;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import tc.oc.bingo.objectives.ObjectiveTracker.StatefulInt;

@Tracker("grave-robber")
public class GraveRobberObjective extends StatefulInt {

  private final Supplier<Integer> REQUIRED_COUNT = useConfig("required-count", 10);
  private final Supplier<Integer> MAX_DURATION = useConfig("duration-seconds", 30);
  private final Supplier<Double> PICKUP_RADIUS = useConfig("radius", 5.0);

  // Using a Cache to store death locations and the UUID of the player who died there.
  private final Cache<Location, UUID> recentDeathLocations =
      CacheBuilder.newBuilder().expireAfterWrite(MAX_DURATION.get(), TimeUnit.SECONDS).build();

  @Override
  protected int maxValue() {
    return REQUIRED_COUNT.get();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (event.getKeepInventory()) return;
    recentDeathLocations.put(event.getEntity().getLocation(), event.getEntity().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPickupItem(PlayerPickupItemEvent event) {
    Location pickupLocation = event.getItem().getLocation();
    double radiusSq = Math.pow(PICKUP_RADIUS.get(), 2);

    for (Location deathLocation : recentDeathLocations.asMap().keySet()) {
      if (deathLocation.getWorld().equals(pickupLocation.getWorld())
          && deathLocation.distanceSquared(pickupLocation) <= radiusSq) {

        UUID victimUUID = recentDeathLocations.getIfPresent(deathLocation);
        if (victimUUID != null && event.getPlayer().getUniqueId().equals(victimUUID)) {
          // Player is trying to rob their own grave, ignore.
          continue;
        }

        trackProgress(event.getPlayer());
        return;
      }
    }
  }
}
