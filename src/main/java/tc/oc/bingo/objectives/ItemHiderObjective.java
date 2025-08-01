package tc.oc.bingo.objectives;

import static org.bukkit.event.inventory.InventoryType.CHEST;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("item-hider")
public class ItemHiderObjective extends ObjectiveTracker {

  private final Supplier<Material> TRACKED_ITEM = useConfig("tracked-item", Material.LEATHER);

  private final Map<UUID, Vector> storedLocations = useState(Scope.MATCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    HumanEntity clicker = event.getWhoClicked();
    if (!(clicker instanceof Player player)) return;

    Inventory clickedInventory = event.getClickedInventory();
    if (clickedInventory == null) return;

    // Prevent non-block inventories and chests from being tracked
    InventoryHolder holder = clickedInventory.getHolder();
    if (!(holder instanceof BlockState container)) return;
    if (clickedInventory.getType().equals(CHEST)) return;

    Location location = container.getLocation();
    if (location == null) return;
    Vector vector = location.toVector();

    ItemStack currentItem = event.getCurrentItem();
    ItemStack cursorItem = event.getCursor();
    Material tracked = TRACKED_ITEM.get();

    // Player places tracked item into block inventory
    if (cursorItem != null && cursorItem.getType() == tracked) {
      storedLocations.put(player.getUniqueId(), vector);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryOpenEvent(InventoryOpenEvent event) {
    Inventory inventory = event.getInventory();
    if (!inventory.contains(TRACKED_ITEM.get())) return;

    InventoryHolder holder = inventory.getHolder();
    if (!(holder instanceof BlockState container)) return;

    Vector containerLocation = container.getLocation().toVector();
    if (!(event.getPlayer() instanceof Player player)) return;

    storedLocations.entrySet().stream()
        .filter(
            entry -> {
              UUID hiderId = entry.getKey();
              Vector location = entry.getValue();
              boolean locationMatch = location.equals(containerLocation);
              return !hiderId.equals(player.getUniqueId()) && locationMatch;
            })
        .findFirst()
        .ifPresent(
            entry -> {
              UUID hiderId = entry.getKey();
              storedLocations.remove(hiderId);

              MatchPlayer hider = getPlayer(hiderId);
              if (hider == null) return;

              reward(Set.of(player, hider.getBukkit()));
            });
  }
}
