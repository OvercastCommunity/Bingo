package tc.oc.bingo.objectives;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Squid;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.util.bukkit.Effects;
import tc.oc.pgm.util.inventory.InventoryUtils;

@Tracker("squid-spawn")
public class SquidSpawnObjective extends ObjectiveTracker {

  private static final Random RANDOM = new Random();

  private final Supplier<Integer> MAX_SPAWNS = useConfig("max-spawns", 3);
  private final Supplier<Integer> MIN_SPAWN_SPOTS = useConfig("min-spawn-spots", 3);

  private final Set<Entity> spawnedSquids = new HashSet<>();
  private final Map<UUID, Location> storedLocations = useState(Scope.PARTICIPATION);

  @Override
  public Stream<ManagedListener> children() {
    Stream<Ticker> extraTickers =
        Stream.of(
            new Ticker(this::spawnAction, 1, 1, TimeUnit.MINUTES),
            new Ticker(this::glowAction, 1, 200, TimeUnit.MILLISECONDS));

    return Stream.concat(super.children(), extraTickers);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    spawnedSquids.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    Location location = event.getPlayer().getLocation();

    // Check if the location is within water
    if (!locationIsValid(location)) return;

    // Store a list of the last 50 locations that were in water
    storedLocations.put(event.getPlayer().getId(), location);
  }

  @EventHandler
  public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
    if (!(event.getRightClicked() instanceof Squid squid)) return;

    Player player = event.getPlayer();
    if (player == null) return;

    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null) return;

    // Check player is holding correct item
    ItemStack item = player.getInventory().getItemInHand();
    if (item == null || (item.getType() != Material.GLOWSTONE_DUST)) return;

    // Remove item and add squid to the list with glow
    InventoryUtils.consumeItem(event);
    player.playSound(squid.getLocation(), Sound.EAT, 1, 1);
    spawnedSquids.add(squid);
    playGlowingEffect(squid);
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    Entity entity = event.getEntity();

    // Check if entity was one of the lads
    if (!spawnedSquids.remove(entity)) return;
    if (!(entity instanceof Squid squid)) return;

    Player killer = squid.getKiller();
    if (killer == null) return;

    reward(killer);
  }

  private void spawnAction() {
    if (storedLocations.isEmpty() || storedLocations.size() < MIN_SPAWN_SPOTS.get()) return;

    // Get a random location from the stored locations
    List<Map.Entry<UUID, Location>> entries = new ArrayList<>(storedLocations.entrySet());
    if (entries.isEmpty()) return;

    Map.Entry<UUID, Location> randomEntry = entries.get(RANDOM.nextInt(entries.size()));
    Location location = randomEntry.getValue();

    // Check if the location is still within water
    if (!locationIsValid(location)) {
      storedLocations.remove(randomEntry.getKey());
      spawnAction(); // Try again
      return;
    }

    // Check if the max spawns have been reached
    if (spawnedSquids.size() >= MAX_SPAWNS.get()) return;

    // Check if any squids nearby
    if (!location.getNearbyEntitiesByType(Squid.class, 5).isEmpty()) return;

    // Spawn a squid at the location
    spawnedSquids.add(location.getWorld().spawnEntity(location, EntityType.SQUID));
  }

  private void glowAction() {
    // Check if any squids are spawned
    if (spawnedSquids.isEmpty()) return;

    // Iterate through the spawned squids
    for (Entity squid : spawnedSquids) {
      // Check if the squid is still valid
      if (squid == null || !squid.isValid()) {
        // Remove the squid from the set
        spawnedSquids.remove(squid);
        continue;
      }

      // Random chance to skip glowing
      if (RANDOM.nextBoolean()) return;

      // Spawn particles to make the squid glow
      playGlowingEffect(squid);
    }
  }

  private void playGlowingEffect(Entity entity) {
    // TODO:  random offset more than one?
    for (int i = 0; i < 3; i++) {
      Effects.EFFECTS.coloredDust(
          entity.getWorld(),
          entity
              .getLocation()
              .add(
                  -0.75 + (Math.random() * 1.5),
                  -0.25 + (Math.random() * 1.5),
                  -0.75 + (Math.random() * 1.5)),
          Color.YELLOW);
    }
    // entity.getWorld().playEffect(entity.getLocation(), Effect.STEP_SOUND, Material.GLOWSTONE);
    //    entity.getWorld().playEffect(entity.getLocation(), Effect.TILE_BREAK,
    // Material.GLOWSTONE_DUST);
  }

  private boolean locationIsValid(Location location) {
    Material type = location.getBlock().getType();
    return type.equals(Material.WATER) || type.equals(Material.STATIONARY_WATER);
  }
}
