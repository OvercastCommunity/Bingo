package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.ENTITY_TYPE_READER;
import static tc.oc.bingo.config.ConfigReader.MATERIAL_READER;

import java.util.function.Supplier;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

@Tracker("mob-punch")
public class MobPunchObjective extends ObjectiveTracker {

  // TODO: enable chicken egg poop'in

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

    Item item =
        event
            .getWorld()
            .dropItem(playerLocation.clone().add(0, 0.4, 0), new ItemStack(Material.EGG, 1));

    Vector direction = player.getLocation().getDirection().setY(0).normalize();
    Vector oppositeDirection = direction.multiply(-0.2);
    item.setVelocity(oppositeDirection);

    event.getWorld().playSound(playerLocation, Sound.CHICKEN_EGG_POP, 1.0f, 1.0f);
  }
}
