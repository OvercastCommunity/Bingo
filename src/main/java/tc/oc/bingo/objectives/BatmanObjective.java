package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("batman-killer")
public class BatmanObjective extends ObjectiveTracker {

  private final Supplier<Double> BAT_SPAWN_CHANCE = useConfig("bat-spawn-chance", 0.2);

  private final Supplier<Double> SKELETON_CHANCE = useConfig("skeleton-chance", 0.001);
  private final Supplier<Double> SKELETON_SUGAR_CHANCE = useConfig("skeleton-sugar-chance", 0.2);

  private final Supplier<Integer> SPAWN_HEIGHT = useConfig("spawn-height", 10);
  private final Supplier<Integer> AUTO_REMOVE_SECONDS = useConfig("spawn-seconds", 30);

  private final Supplier<Double> EGG_CHANCE = useConfig("egg-chance", 0.05);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDeath(MatchPlayerDeathEvent event) {
    if (Math.random() > BAT_SPAWN_CHANCE.get()) return;

    Location spawnLocation = event.getPlayer().getLocation().add(0, SPAWN_HEIGHT.get(), 0);
    if (spawnLocation.getY() > 5
        && event.getWorld().getBlockAt(spawnLocation).getType() == Material.AIR) {

      // Increase chance of spawning with skeleton if sugar in hand
      MatchPlayer matchPlayer = getPlayer(event.getKiller());
      boolean hasSugar =
          matchPlayer != null && matchPlayer.getBukkit().getInventory().contains(Material.SUGAR);
      boolean withSkeleton =
          Math.random() < (hasSugar ? SKELETON_SUGAR_CHANCE.get() : SKELETON_CHANCE.get());
      spawnBat(spawnLocation, withSkeleton);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSneak(PlayerToggleSneakEvent event) {
    if (!event.isSneaking() || Math.random() > EGG_CHANCE.get()) return;

    PlayerInventory inventory = event.getPlayer().getInventory();
    if (inventory == null || inventory.getHelmet().getType() != Material.PUMPKIN) return;

    Player player = event.getPlayer();
    Location playerLocation = player.getLocation();

    Item item =
        event
            .getWorld()
            .dropItem(playerLocation.clone().add(0, 0.4, 0), new ItemStack(Material.EGG, 1));

    Vector direction = player.getLocation().getDirection().setY(0).normalize();
    Vector oppositeDirection = direction.multiply(-0.2);
    item.setVelocity(oppositeDirection);

    event.getWorld().playSound(playerLocation, Sound.CHICKEN_EGG_POP, 1.0f, 1.0f);
  }

  private void spawnBat(Location location, boolean withSkeleton) {
    Bat bat = (Bat) location.getWorld().spawnEntity(location, EntityType.BAT);
    bat.setMaxHealth(1);

    if (withSkeleton) {
      Skeleton skeleton = (Skeleton) location.getWorld().spawnEntity(location, EntityType.SKELETON);
      skeleton.getEquipment().setHelmet(new ItemStack(Material.PUMPKIN));
      skeleton.getEquipment().setHelmetDropChance(1);
      skeleton.setMaxHealth(1);
      bat.setPassenger(skeleton);
    }

    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            Bingo.get(),
            () -> {
              if (!bat.isDead()) bat.remove();
            },
            AUTO_REMOVE_SECONDS.get() * 20L);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamage(EntityDeathEvent event) {
    Entity entity = event.getEntity();
    if (!(entity instanceof Bat)) return;

    Player killer = event.getEntity().getKiller();
    if (killer == null) return;

    reward(killer);
  }
}
