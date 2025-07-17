package tc.oc.bingo.modules;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Door;

@BingoModule.Config("fridge-recipes")
public class FridgeModule extends BingoModule {
  public static final FridgeModule INSTANCE = new FridgeModule();

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void openFridge(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    var block = event.getClickedBlock();
    if (block == null || block.getType() != Material.IRON_DOOR_BLOCK) return;

    var state = (Door) block.getState();
    if (state.isOpen() || event.getBlockFace() != state.getFacing()) return;
  }
}
