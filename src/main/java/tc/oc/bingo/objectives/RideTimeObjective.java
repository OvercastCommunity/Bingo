package tc.oc.bingo.objectives;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;
import tc.oc.bingo.config.ConfigReader;

@Tracker("ride-time")
public class RideTimeObjective extends ObjectiveTracker {

  private final Map<UUID, Instant> playersInVehicle = useState(Scope.LIFE);

  private static final ConfigReader<EntityType> ENTITY_READER =
      (cfg, key, def) ->
          com.google.common.base.Objects.firstNonNull(EntityType.fromName(cfg.getString(key)), def);

  private final Supplier<Integer> RIDING_SECONDS = useConfig("riding-seconds", 30);
  private final Supplier<EntityType> ENTITY_TYPE =
      useConfig("entity-type", EntityType.MINECART, ENTITY_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityMount(EntityMountEvent event) {
    if (!(event.getEntity() instanceof Player)) return;

    Player player = (Player) event.getEntity();
    Entity vehicle = event.getMount();

    if (player == null || (!(vehicle.getType().equals(ENTITY_TYPE.get())))) return;

    playersInVehicle.put(player.getUniqueId(), Instant.now());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDismount(EntityDismountEvent event) {
    if (!(event.getEntity() instanceof Player)) return;

    Player player = (Player) event.getEntity();
    Entity vehicle = event.getDismounted();

    if (player == null || (!(vehicle.getType().equals(ENTITY_TYPE.get())))) return;

    Instant mountInstant = playersInVehicle.remove(player.getUniqueId());
    if (mountInstant != null
        && Duration.between(mountInstant, Instant.now()).getSeconds() >= RIDING_SECONDS.get()) {
      reward(player);
    }
  }
}
