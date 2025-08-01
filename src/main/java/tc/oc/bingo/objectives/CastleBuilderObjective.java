package tc.oc.bingo.objectives;

import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.Skin;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.util.CustomItem;
import tc.oc.pgm.api.event.BlockTransformEvent;

@Tracker("castle-builder")
public class CastleBuilderObjective extends ObjectiveTracker {

  private static final Supplier<CustomItem> SAND_BUCKET = CustomItem.of("sand_bucket");
  private static final Supplier<CustomItem> SAND_CASTLE = CustomItem.of("sand_castle");

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

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockPlace(BlockTransformEvent event) {
    if (!event.getNewState().getType().equals(Material.SKULL)) return;

    MetadataValue metadata =
        event.getNewState().getBlock().getMetadata("custom-item-id", Bingo.get());
    if (metadata == null || !metadata.asString().equals(SAND_BUCKET.get().id())) return;

    // event.getNewState().set

    // CUSTOM_ITEM_META.has(event.getBlock()

    // if (!CustomItemModule.isCustomBlock(event.getBlock(), SAND_BUCKET)) return;

    // event.getActor()
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

    // TODO: update the meta to be sand castle? or just turn to sand on pick up?

    // Reward the player
    reward(player);
  }
}
