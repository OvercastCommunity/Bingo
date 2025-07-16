package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.util.CustomItem;

@Tracker("armor-stand-head-gear")
public class ArmorStandHeadgearObjective extends ObjectiveTracker {

  private static final Supplier<CustomItem> LEMON_ITEM = CustomItem.of("lemon");

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onArmorStandChange(PlayerArmorStandManipulateEvent event) {
    Player player = event.getPlayer();
    ItemStack playerItem = event.getPlayerItem();

    if (!CustomItemModule.isCustomItem(playerItem, LEMON_ITEM)) return;

    reward(player);
  }
}
