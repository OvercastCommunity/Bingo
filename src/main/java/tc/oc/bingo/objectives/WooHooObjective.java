package tc.oc.bingo.objectives;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

@Tracker("woo-hoo")
public class WooHooObjective extends ObjectiveTracker {

  private final Supplier<Double> MAX_RANGE = useConfig("max-range", 5.0);
  private final Supplier<Integer> MAX_TIME = useConfig("max-time-seconds", 5);
  private final Supplier<Double> SPAWN_CHANCE = useConfig("spawn-chance", 0.1);

  private final Map<UUID, UUID> lastWooHooBy = useState(Scope.LIFE);
  private final Map<UUID, Long> lastWooHooTime = useState(Scope.LIFE);
  private final Set<UUID> spawnedCooldown = Collections.newSetFromMap(useState(Scope.LIFE));

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerWooHoo(FlowerBearerObjective.PlayerWooHooEvent event) {
    Player wooer = event.getPlayer().getBukkit();
    Player target = event.getTarget().getBukkit();

    UUID wooerId = wooer.getUniqueId();
    UUID targetId = target.getUniqueId();
    long currentTime = System.currentTimeMillis();

    // Update maps with current wooed data
    lastWooHooBy.put(targetId, wooerId);
    lastWooHooTime.put(targetId, currentTime);

    // Get nearby players within range of the target player
    Optional<Player> partner =
        target.getLocation().getNearbyPlayers(MAX_RANGE.get()).stream()
            .filter(p -> passesVibeCheck(wooer, target, p, currentTime))
            .findFirst();

    if (partner.isPresent()) {
      reward(wooer);

      // Apply life based cooldown and random chance
      if (!spawnedCooldown.contains(wooerId) && SPAWN_CHANCE.get() > Math.random()) {
        spawnedCooldown.add(wooerId);
        spawn(target, partner.get());
      }
    }
  }

  private boolean passesVibeCheck(
      Player wooer, Player primary, Player secondary, long currentTime) {
    // Ignore wooer and clicked player
    if (secondary.equals(wooer) || secondary.equals(primary)) return false;

    UUID otherPlayerId = secondary.getUniqueId();

    // Player was not wooed or wooed by somebody else
    UUID lastWooerId = lastWooHooBy.get(secondary.getUniqueId());
    if (lastWooerId == null || lastWooerId != wooer.getUniqueId()) return false;

    // Check that the timestamp since last wooed
    Long otherPlayerWooHooTime = lastWooHooTime.get(otherPlayerId);
    return otherPlayerWooHooTime != null
        && (currentTime - otherPlayerWooHooTime) <= MAX_TIME.get() * 1000;
  }

  private void spawn(Player target, Player player) {
    // Randomly pick either the target or player
    Player selectedPlayer = Math.random() < 0.5 ? target : player;
    Player otherPlayer = selectedPlayer == target ? player : target;

    // Spawn a baby zombie at the feet of the selected player
    World world = selectedPlayer.getWorld();
    Location spawnLocation = selectedPlayer.getLocation();
    Zombie zombie = (Zombie) world.spawnEntity(spawnLocation, EntityType.ZOMBIE);
    zombie.setBaby(true);

    // Make the zombie wear a skull of either the target or player
    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
    SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
    skullMeta.setOwner(selectedPlayer.getName());
    skull.setItemMeta(skullMeta);
    zombie.getEquipment().setHelmet(skull);

    // Make the zombie wear the armor of the other player
    zombie.getEquipment().setChestplate(otherPlayer.getInventory().getChestplate());
    zombie.getEquipment().setLeggings(otherPlayer.getInventory().getLeggings());
    zombie.getEquipment().setBoots(otherPlayer.getInventory().getBoots());

    // Ensure the armor and helmet do not drop
    zombie.getEquipment().setHelmetDropChance(0.0f);
    zombie.getEquipment().setChestplateDropChance(0.0f);
    zombie.getEquipment().setLeggingsDropChance(0.0f);
    zombie.getEquipment().setBootsDropChance(0.0f);
    zombie.setCanPickupItems(true);
  }
}
