package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_READER;
import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.Set;
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

  private final Supplier<Set<Material>> MATERIALS_REQUIRED =
      useConfig("block-list", Set.of(Material.LEAVES, Material.LEAVES_2), MATERIAL_SET_READER);

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
