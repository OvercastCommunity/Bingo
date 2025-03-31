package tc.oc.bingo.objectives;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityMountEvent;

@Tracker("crossy-road")
public class CrossyRoadObjective extends ObjectiveTracker {

  // The UUID is of the chicken (not a player)
  private final Map<UUID, Vector> startLocation = useState(Scope.MATCH);
  private final Map<UUID, Vector> lastLocation = useState(Scope.MATCH);
  private final Map<UUID, Set<UUID>> mineCartPushers = useState(Scope.MATCH);

  // Put a chicken in a mine-cart and push it on a rail
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityMount(EntityMountEvent event) {
    if (!(event.getEntity() instanceof Chicken chicken)) return;
    if (!(event.getMount() instanceof Minecart minecart)) return;

    // When a chicken enters a cart track its starting location
    startLocation.put(chicken.getUniqueId(), minecart.getLocation().toVector());
    lastLocation.put(chicken.getUniqueId(), minecart.getLocation().toVector());
    mineCartPushers.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onVehicleMove(VehicleMoveEvent event) {
    Entity passenger = event.getVehicle().getPassenger();
    if (passenger == null || passenger.getType() != EntityType.CHICKEN) return;
    if (!(event.getVehicle() instanceof Minecart)) return;

    // Check if the minecart is tracked and has a last location
    Vector vector = lastLocation.get(passenger.getUniqueId());
    if (vector == null) return;

    Vector newLocation = event.getTo().toVector();
    if (vector.distance(newLocation) > 1) {
      // Reward the player who pushed the minecart
      lastLocation.put(passenger.getUniqueId(), newLocation);

      Set<UUID> nearbyPlayerIds =
          event.getWorld().getNearbyPlayers(event.getFrom(), 1).stream()
              .map(Entity::getUniqueId)
              .collect(Collectors.toSet());

      mineCartPushers
          .computeIfAbsent(passenger.getUniqueId(), k -> new HashSet<>())
          .addAll(nearbyPlayerIds);
    }

    // Reward players who pushed it 10 blocks
    if (vector.distance(startLocation.get(passenger.getUniqueId())) > 10) {

      // Reset the start location and pushers
      startLocation.put(passenger.getUniqueId(), newLocation);
      Set<UUID> pushers = mineCartPushers.remove(passenger.getUniqueId());
      if (pushers != null)
        reward(
            pushers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    }
  }
}
