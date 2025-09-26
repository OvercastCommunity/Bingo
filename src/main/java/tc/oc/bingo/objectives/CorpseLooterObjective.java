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
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;

@Tracker("corpse-looter")
public class CorpseLooterObjective extends StatefulInt {

  private final Supplier<Integer> REQUIRED_COUNT = useConfig("required-count", 32);
  private final Supplier<Integer> MAX_DURATION = useConfig("duration-seconds", 10);
  private final Supplier<Double> PICKUP_RADIUS = useConfig("radius", 3.0);

  private final Supplier<Double> PICKUP_RADIUS_SQR =
      useComputedConfig(() -> PICKUP_RADIUS.get() * PICKUP_RADIUS.get());

  // Using a Cache to store death locations and the UUID of the player who died there.
  private final Cache<Location, UUID> recentDeathLocations =
      CacheBuilder.newBuilder().expireAfterWrite(MAX_DURATION.get(), TimeUnit.SECONDS).build();

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    recentDeathLocations.invalidateAll();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (event.getKeepInventory()) return;
    recentDeathLocations.put(event.getEntity().getLocation(), event.getEntity().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerPickupItem(PlayerPickupItemEvent event) {
    Location pickupLocation = event.getItem().getLocation();
    for (Location deathLocation : recentDeathLocations.asMap().keySet()) {
      if (deathLocation.distanceSquared(pickupLocation) <= PICKUP_RADIUS_SQR.get()) {

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

  @Override
  protected int maxValue() {
    return REQUIRED_COUNT.get();
  }
}
