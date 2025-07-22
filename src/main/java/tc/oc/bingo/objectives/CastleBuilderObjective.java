package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

    ItemStack sandCastle = SAND_CASTLE.get().toItemStack();
    block.setType(Material.SANDSTONE); // Or any block you want to represent the sand castle

    // Reward the player
    reward(player);
  }
}
