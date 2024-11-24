package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;

import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.inventory.ItemStack;

@Tracker("block-pyro")
public class BlockPyroObjective extends ObjectiveTracker {

  private final Supplier<Set<Material>> BLOCKS_ALLOWED =
      useConfig("blocks-allowed", Set.of(Material.LOG, Material.LOG_2), MATERIAL_SET_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockIgnite(BlockIgniteEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;

    ItemStack itemInHand = player.getItemInHand();
    if (itemInHand == null || itemInHand.getType() != Material.FLINT_AND_STEEL) return;

    Material targetBlock = event.getPlayer().getTargetedBlock(false, false).getBlock().getType();

    if (BLOCKS_ALLOWED.get().contains(targetBlock)) reward(player);
  }
}
