package tc.oc.bingo.objectives;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.MeleeInfo;
import tc.oc.pgm.tracker.info.ItemInfo;

@Tracker("grim-sneaker")
public class GrimSneakerObjective extends ObjectiveTracker {

  private final Map<UUID, Long> lastSneaking = useState(Scope.LIFE);

  private final Supplier<Double> MIN_SNEAK_SECONDS = useConfig("min-sneak-seconds", 5d);

  private final Set<Material> HOE_MATERIALS =
      EnumSet.of(
          Material.WOOD_HOE,
          Material.STONE_HOE,
          Material.GOLD_HOE,
          Material.IRON_HOE,
          Material.DIAMOND_HOE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    Player killer = getBukkit(event.getKiller());
    if (killer == null) return;

    if (!killer.isSneaking()) return;

    // Check if the killer is holding a hoe
    ItemStack directKillItem = getDirectKillItem(event.getDamageInfo());
    if (directKillItem == null) return;

    if (!HOE_MATERIALS.contains(directKillItem.getType())) return;

    Player player = event.getVictim().getBukkit();
    Location location = player.getLocation().add(0, 0.5, 0);
    if (location.getY() < 0) return;

    location
        .getWorld()
        .dropItemNaturally(location, new ItemStack(Material.BONE, (int) (Math.random() % 0.3)));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(final EntityDamageByEntityEvent event) {
    if (!event.getCause().equals(ENTITY_ATTACK)) return;
    if (!(event.getEntity() instanceof Player victim)) return;
    if (!(event.getDamager() instanceof Player attacker)) return;

    Material type = attacker.getInventory().getItemInHand().getType();
    if (!HOE_MATERIALS.contains(type)) return;

    Location location = victim.getLocation().add(0, 0.5, 0);
    location.getWorld().playEffect(location, Effect.STEP_SOUND, Material.REDSTONE_WIRE);

    // Randomly 0.05% chance to drop a bone
    if (Math.random() < 0.05) {
      location.getWorld().dropItemNaturally(location, new ItemStack(Material.BONE));
    }

    // Check if player has been sneaking for the required time
    Long lastSneakTime = lastSneaking.get(attacker.getUniqueId());
    if (lastSneakTime == null) return;
    long currentTime = System.currentTimeMillis();
    long secondsSneaking = (currentTime - lastSneakTime) / 1000;
    if (secondsSneaking < MIN_SNEAK_SECONDS.get()) return;

    reward(attacker);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSneak(PlayerToggleSneakEvent event) {
    UUID uniqueId = event.getPlayer().getUniqueId();

    if (event.isSneaking()) {
      lastSneaking.computeIfAbsent(uniqueId, k -> System.currentTimeMillis());
    }

    if (!event.isSneaking()) {
      lastSneaking.remove(uniqueId);
    }
  }

  public @Nullable ItemStack getDirectKillItem(DamageInfo damageInfo) {
    if (damageInfo instanceof MeleeInfo info) {
      if (info.getWeapon() instanceof ItemInfo itemInfo) {
        return itemInfo.getItem();
      }
    }

    return null;
  }
}
