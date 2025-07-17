package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.CarePackageModule;
import tc.oc.bingo.modules.DependsOn;
import tc.oc.bingo.util.LocationUtils;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.tag.ItemTag;

@Tracker("lava-entity")
@DependsOn(CarePackageModule.class)
public class LavaEntityObjective extends ObjectiveTracker {

  public static final ItemTag<Boolean> LAVA_BUCKET_ITEM = ItemTag.newBoolean("custom-lava-bucket");
  private final Supplier<EntityType> ENTITY_TYPE = useConfig("entity-type", EntityType.CHICKEN);
  private final List<Function<MatchPlayer, ItemStack>> loot =
      List.of(
          this::loot,
          matchPlayer -> new ItemStack(Material.SADDLE),
          matchPlayer -> new ItemStack(Material.SAPLING, 1, (short) 3));

  @Override
  public void setupDependencies() {
    CarePackageModule.INSTANCE.addLoot(loot);
  }

  @Override
  public void teardownDependencies() {
    CarePackageModule.INSTANCE.removeLoot(loot);
  }

  private ItemStack loot(MatchPlayer player) {
    ItemStack itemStack = new ItemStack(Material.LAVA_BUCKET);
    LAVA_BUCKET_ITEM.set(itemStack, true);
    return itemStack;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
    Player player = event.getPlayer();

    // Check the player is playing
    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    Block block = event.getBlockClicked().getRelative(event.getBlockFace());

    // Check if the bucket contains lava
    if (event.getBucket() != Material.LAVA_BUCKET) return;
    boolean isCustomLavaBucket = LAVA_BUCKET_ITEM.has(event.getPlayer().getItemInHand());

    // When using custom lava buckets prevent placing actual
    if (isCustomLavaBucket) event.setCancelled(true);

    ItemStack itemStack = event.getItemStack();
    itemStack.setType(Material.BUCKET);
    event.getPlayer().setItemInHand(itemStack);

    // When player stood in block set them on fire
    if (LocationUtils.stoodInBlock(player.getLocation(), block.getLocation().toVector())) {
      // Set the player on fire
      player.setFireTicks(100);
    }

    // Check entities of type within 0.5 blocks of the lava
    Collection<? extends Entity> entities =
        block.getLocation().getNearbyEntitiesByType(ENTITY_TYPE.get().getEntityClass(), 1.5);

    if (entities.isEmpty()) return;

    entities.forEach(
        entity -> {
          // Not 100% accurate as expects player hitbox
          if (LocationUtils.stoodInBlock(entity.getLocation(), block.getLocation().toVector())) {
            entity.setFireTicks(100);
            reward(player);
          }
        });
  }

  @EventHandler(ignoreCancelled = true)
  public void onDispenserActivate(BlockDispenseEvent event) {
    if (event.getItem().getType() != Material.LAVA_BUCKET) return;

    // Cancel the lava from dispensing, removing breaks things
    if (LAVA_BUCKET_ITEM.has(event.getItem())) {
      event.setCancelled(true);
    }
  }
}
