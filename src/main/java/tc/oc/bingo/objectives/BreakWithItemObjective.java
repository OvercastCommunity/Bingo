package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_LIST_READER;
import static tc.oc.bingo.config.ConfigReader.MATERIAL_READER;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

@Tracker("break-with-item")
public class BreakWithItemObjective extends ObjectiveTracker {

  private final Supplier<Material> ITEM_IN_HAND =
      useConfig("item-in-hand", Material.SHEARS, MATERIAL_READER);

  private final Supplier<List<Material>> MATERIALS_REQUIRED =
      useConfig(
          "block-list",
          Lists.newArrayList(Material.LEAVES, Material.LEAVES_2),
          MATERIAL_LIST_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    Block block = event.getBlock();

    Material blockType = block.getType();
    if (!MATERIALS_REQUIRED.get().contains(blockType)) return;

    ItemStack itemInHand = player.getInventory().getItemInHand();
    if (itemInHand == null || itemInHand.getType() != ITEM_IN_HAND.get()) return;

    reward(player);
  }
}
