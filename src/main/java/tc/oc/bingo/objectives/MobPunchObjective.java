package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.ENTITY_TYPE_READER;
import static tc.oc.bingo.config.ConfigReader.MATERIAL_READER;

import java.util.function.Supplier;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.inventory.tag.ItemTag;

@Tracker("mob-punch")
public class MobPunchObjective extends ObjectiveTracker {

  private static final ItemTag<Boolean> EGG_ITEM = ItemTag.newBoolean("custom-egg-item");
  public static final String EGG_META = "bingo-egg";
  private final MetadataValue eggMetaValue = MetadataUtils.createMetadataValue(Bingo.get(), true);

  private final Supplier<Material> ITEM_REQUIRED =
      useConfig("item-type", Material.FEATHER, MATERIAL_READER);

  private final Supplier<EntityType> ENTITY_REQUIRED =
      useConfig("entity-type", EntityType.CHICKEN, ENTITY_TYPE_READER);

  private final Supplier<Double> EGG_CHANCE = useConfig("egg-chance", 0.05);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerHitEntity(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player player)) return;
    if (!(event.getEntity() instanceof LivingEntity entity)) return;

    // Check if the player is using the correct item and hitting the correct type of entity
    if (player.getInventory().getItemInHand().getType() != ITEM_REQUIRED.get()) return;

    if (entity.getType() == ENTITY_REQUIRED.get()) reward(player);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerSneak(PlayerToggleSneakEvent event) {
    if (!event.isSneaking()
        || Math.random() > EGG_CHANCE.get()
        || notParticipating(event.getPlayer())) return;

    Player player = event.getPlayer();
    if (player.getGameMode().equals(GameMode.CREATIVE)) return;
    Location playerLocation = player.getLocation();

    ItemStack itemStack = new ItemStack(Material.EGG, 1);
    EGG_ITEM.set(itemStack, true);

    Item item = event.getWorld().dropItem(playerLocation.clone().add(0, 0.4, 0), itemStack);

    Vector direction = player.getLocation().getDirection().setY(0).normalize();
    Vector oppositeDirection = direction.multiply(-0.2);
    item.setVelocity(oppositeDirection);

    event.getWorld().playSound(playerLocation, Sound.CHICKEN_EGG_POP, 1.0f, 1.0f);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onProjectileLaunch(final ProjectileLaunchEvent event) {
    if (!(event.getEntity() instanceof Egg)) return;

    if (!(event.getActor() instanceof Player player)) return;
    ItemStack itemInHand = player.getItemInHand();

    if (itemInHand == null) return;
    if (!EGG_ITEM.has(itemInHand)) return;

    event.getEntity().setMetadata(EGG_META, eggMetaValue);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onCreatureSpawn(final CreatureSpawnEvent event) {
    if (!event.isCancelled()) return;

    // Only listen for chicken egg spawns
    if (!event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.EGG)) return;
    if (!event.getEntity().getType().equals(EntityType.CHICKEN)) return;

    if (event.getEntity().getNearbyEntities(0, 0, 0).stream()
        .noneMatch(entity -> entity.hasMetadata(EGG_META))) return;

    event.setCancelled(false);
  }
}
