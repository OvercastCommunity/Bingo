package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.InventoryUtils;

@Tracker("item-launcher")
public class ItemLauncherObjective extends ObjectiveTracker {

  private final Supplier<Material> THROWABLE_ITEM =
      useConfig("throwable-item", Material.POTATO_ITEM);

  private final Supplier<Double> THROW_VELOCITY = useConfig("throw-velocity", 1.0);

  @EventHandler
  public void onRightClick(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR) return;

    Player player = event.getPlayer();
    ItemStack item = player.getInventory().getItemInHand();

    if (item == null || item.getType() != THROWABLE_ITEM.get()) return;

    // Launch the potato like as an item projectile
    Item thrownItem =
        player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(THROWABLE_ITEM.get()));
    thrownItem.setVelocity(player.getLocation().getDirection().multiply(THROW_VELOCITY.get()));
    thrownItem.setPickupDelay(40);

    InventoryUtils.consumeItem(event, event.getPlayer());
    event.setCancelled(true);

    reward(player);
  }
}
