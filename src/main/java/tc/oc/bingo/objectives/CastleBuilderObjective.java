package tc.oc.bingo.objectives;

import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Skin;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.util.CustomItem;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.material.MaterialData;

@Tracker("castle-builder")
public class CastleBuilderObjective extends ObjectiveTracker {

  private static final Supplier<CustomItem> SAND_BUCKET = CustomItem.of("sand_bucket");
  private static final Supplier<CustomItem> SAND_CASTLE = CustomItem.of("sand_castle");

  private final Random random = new Random();

  @EventHandler(ignoreCancelled = true)
  public void onPlayerBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    Player player = event.getPlayer();

    // Ensure breaking sand with a water bucket in hand
    if (block.getType() != Material.SAND) return;
    ItemStack hand = player.getItemInHand();
    if (hand == null || hand.getType() != Material.WATER_BUCKET) return;

    // Replace the water bucket with a sand bucket
    player.setItemInHand(SAND_BUCKET.get().toItemStack());

    // Prevent sand from dropping
    block.setType(Material.AIR);
    event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onCastleBreak(BlockTransformEvent event) {
    Block block = event.getBlock();
    if (!event.isBreak()) return;
    if (!CustomItemModule.isCustomBlock(block, SAND_BUCKET)) return;

    // If block broken by player holding a bucket allow drop
    if (event.getActor() instanceof Player player) {
      ItemStack itemInHand = player.getInventory().getItemInHand();
      if (itemInHand != null && itemInHand.getType().equals(Material.BUCKET)) {
        InventoryUtils.consumeItem(event, player);
        return;
      }
    }

    event.setCancelled(true);
    final BlockState newState = event.getNewState();

    MaterialData.block(newState).applyTo(block, true);

    Location dropLocation = block.getLocation().clone();
    dropLocation.setX(dropLocation.getBlockX() + random.nextDouble() * 0.5 + 0.25);
    dropLocation.setY(dropLocation.getBlockY() + random.nextDouble() * 0.5 + 0.25);
    dropLocation.setZ(dropLocation.getBlockZ() + random.nextDouble() * 0.5 + 0.25);
    dropLocation.getWorld().dropItem(dropLocation, new ItemStack(Material.SAND));
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerPlace(BlockPlaceEvent event) {
    // Only respond if player is placing the sand_bucket item
    if (!CustomItemModule.isCustomItem(event.getItemInHand(), SAND_BUCKET)) return;

    Block block = event.getBlockPlaced();
    Player player = event.getPlayer();

    CustomItem sandCastle = SAND_CASTLE.get();

    BlockFace rotation = null;
    if (event.getBlock().getState() instanceof Skull skull) {
      rotation = skull.getRotation();
    }

    block.setType(Material.SKULL);
    if (block.getState() instanceof Skull skull) {
      skull.setSkullType(SkullType.PLAYER);
      Skin skin = new Skin(sandCastle.texture(), null);
      skull.setOwner("Sand Castle", UUID.nameUUIDFromBytes(sandCastle.name().getBytes()), skin);
      if (rotation != null) skull.setRotation(rotation);
      skull.update();
    }

    // Reward the player
    reward(player);
  }
}
