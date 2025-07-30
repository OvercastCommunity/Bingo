package tc.oc.bingo.objectives;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.InventoryUtils;

@Tracker("pet-grower")
public class PetGrowerObjective extends ObjectiveTracker {

  private static final String OLD_PET_META = "pet";
  private static final String NEW_PET_META = "converted-pet-meta";

  private static final FixedMetadataValue PET_META = new FixedMetadataValue(Bingo.get(), true);

  private final Map<UUID, List<ItemStack>> droppedLoot = useState(Scope.MATCH);

  private Plugin otherPlugin;

  @EventHandler
  public void onMatchLoadEvent(MatchAfterLoadEvent event) {
    if (otherPlugin != null) return;
    otherPlugin = Bukkit.getPluginManager().getPlugin("Ember");
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteractAtEntityEvent(PlayerInteractAtEntityEvent event) {
    if (otherPlugin == null) return;

    if (!(event.getRightClicked() instanceof LivingEntity entity)) return;
    if (entity.hasMetadata(NEW_PET_META)) return;

    // Intercept interaction with the entity and remove pet metadata if necessary
    if (!entity.hasMetadata(OLD_PET_META)) return;

    // Relocate the metadata before another plugin can use it, mwahaha
    UUID ownerId = UUID.fromString(entity.getMetadata(OLD_PET_META, otherPlugin).asString());
    entity.removeMetadata(OLD_PET_META, otherPlugin);
    entity.setMetadata(NEW_PET_META, new FixedMetadataValue(Bingo.get(), ownerId));

    if (entity instanceof Ageable ageable) {
      // If the entity is an age-able allow it to age
      ageable.setAgeLock(false);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof LivingEntity entity)) return;
    if (!entity.hasMetadata(NEW_PET_META)) return;
    if (!(entity instanceof Ageable ageable)) return;
    if (ageable.isAdult()) return;

    // Check player is holding correct item
    Material handItem = event.getPlayer().getItemInHand().getType();
    boolean superFeed = handItem.equals(Material.GOLDEN_APPLE);
    if (!handItem.isEdible() || handItem.equals(Material.WHEAT)) return;

    // Check player is participating in the match
    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    // Consume one food item from players hand and play eating sound from entity
    InventoryUtils.consumeItem(event);

    // Moves age closer to 0 by a percentage
    int current = ageable.getAge();
    double percentageIncrease = superFeed ? 0.8 : 0.2;
    ageable.setAge(current + (int) ((-current) * percentageIncrease));
    spawnGrowingParticles(ageable.getLocation(), 5);

    reward(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDeathEventBefore(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();
    if (!entity.hasMetadata(NEW_PET_META)) return;

    // Store loot for use later on
    UUID entityId = entity.getUniqueId();
    droppedLoot.put(entityId, List.copyOf(event.getDrops()));

    PGM.get().getExecutor().schedule(
            () -> droppedLoot.remove(entityId), 5L, TimeUnit.SECONDS);

    // Sorry Ember, but we need to keep the loot
    // You can have your metadata back now sorry :(
    String uuid = entity.getMetadata(NEW_PET_META, Bingo.get()).asString();
    entity.setMetadata(OLD_PET_META, new FixedMetadataValue(otherPlugin, uuid));
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDeathEventAfter(EntityDeathEvent event) {
    if (!event.getEntity().hasMetadata(OLD_PET_META)) return;

    // Casually add the loot back when nobody is looking
    List<ItemStack> loot = droppedLoot.remove(event.getEntity().getUniqueId());
    if (loot == null || loot.isEmpty()) return;

    event.getDrops().addAll(loot);
  }

  private static void spawnGrowingParticles(Location location, int count) {
    World world = location.getWorld();
    if (world == null) return;

    for (int i = 0; i < count; i++) {
      Location spawnLocation =
          location
              .clone()
              .add(
                  (Math.random() - 0.5) * 0.8,
                  0.4 + Math.random() * 0.8,
                  (Math.random() - 0.5) * 0.8);

      world.playEffect(spawnLocation, Effect.HAPPY_VILLAGER, 0);
    }
  }
}
