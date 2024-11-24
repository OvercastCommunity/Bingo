package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

@Tracker("chest-store")
public class ChestStoreObjective extends ObjectiveTracker {

  private final Map<UUID, Vector> placedChests = useState(Scope.LIFE);

  private final Supplier<Material> ITEM_REQUIRED = useConfig("item", Material.DIAMOND);

  private final Set<InventoryAction> VALID_ACTIONS =
      Set.of(InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ALL);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChestPlace(BlockPlaceEvent event) {
    Block block = event.getBlock();
    if (block.getType() == Material.CHEST) {
      UUID playerId = event.getPlayer().getUniqueId();
      Vector chestLocation = block.getLocation().toVector();
      placedChests.put(playerId, chestLocation);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    // Ensure the click event involves a chest inventory
    if (event.getInventory().getType() != InventoryType.CHEST) return;
    if (!VALID_ACTIONS.contains(event.getAction())) return;

    HumanEntity whoClicked = event.getWhoClicked();
    if (!((whoClicked instanceof Player player))) return;

    // Ensure the clicked item matches the required item
    ItemStack clickedItem = player.getItemOnCursor();
    if (clickedItem == null || clickedItem.getType() != ITEM_REQUIRED.get()) return;

    InventoryHolder holder = event.getInventory().getHolder();
    if (!(holder instanceof Chest chest)) return;

    Vector chestLocation = placedChests.get(player.getUniqueId());
    if (chestLocation == null) return;

    if (!chest.getLocation().toVector().equals(chestLocation)) return;

    reward(player);
  }
}
