package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;
import tc.oc.bingo.config.ConfigReader;

@Tracker("ride-distance")
public class RideDistanceObjective extends ObjectiveTracker {

  private final Map<UUID, Location> playersInVehicle = useState(Scope.LIFE);

  private static final ConfigReader<EntityType> ENTITY_READER =
      (cfg, key, def) ->
          com.google.common.base.Objects.firstNonNull(EntityType.fromName(cfg.getString(key)), def);

  private final Supplier<Double> RIDING_DISTANCE = useConfig("riding-distance", 100.0);
  private final Supplier<Boolean> ALLOW_VERTICAL = useConfig("allow-vertical", false);
  private final Supplier<EntityType> ENTITY_TYPE =
      useConfig("entity-type", EntityType.MINECART, ENTITY_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityMount(EntityMountEvent event) {
    if (!(event.getEntity() instanceof Player)) return;

    Player player = (Player) event.getEntity();
    Entity vehicle = event.getMount();

    if (player == null || (!(vehicle.getType().equals(ENTITY_TYPE.get())))) return;

    playersInVehicle.put(player.getUniqueId(), player.getLocation());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDismount(EntityDismountEvent event) {
    if (!(event.getEntity() instanceof Player)) return;

    Player player = (Player) event.getEntity();
    Entity vehicle = event.getDismounted();

    if (player == null || (!(vehicle.getType().equals(ENTITY_TYPE.get())))) return;

    Location entry = playersInVehicle.remove(player.getUniqueId());
    if (entry == null) return;

    Location exit = player.getLocation();
    double distance =
        (ALLOW_VERTICAL.get())
            ? entry.distance(exit)
            : getXZDistance(entry.toVector(), exit.toVector());

    if (distance >= RIDING_DISTANCE.get()) {
      reward(player);
    }
  }

  private double getXZDistance(Vector v1, Vector v2) {
    double dx = v1.getX() - v2.getX();
    double dz = v1.getZ() - v2.getZ();
    return Math.hypot(dx, dz);
  }
}
