package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("item-hider")
public class ItemHiderObjective extends ObjectiveTracker {

  private final Supplier<Material> TRACKED_ITEM = useConfig("tracked-item", Material.LEATHER);

  private final Map<UUID, ItemLocation> storedLocations = useState(Scope.MATCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    HumanEntity clicker = event.getWhoClicked();
    if (!(clicker instanceof Player player)) return;

    Inventory clickedInventory = event.getClickedInventory();
    if (clickedInventory == null) return;

    InventoryHolder holder = clickedInventory.getHolder();
    if (holder instanceof Player || holder == null) return;

    Location location;
    if (holder instanceof BlockState state) {
      Block block = state.getBlock();
      location = block.getLocation();
    } else {
      location = null;
    }

    if (location == null) return;
    Vector vector = location.toVector();

    ItemStack currentItem = event.getCurrentItem();
    ItemStack cursorItem = event.getCursor();
    Material tracked = TRACKED_ITEM.get();
    int slot = event.getSlot();

    // Player places tracked item into block inventory
    if (cursorItem != null && cursorItem.getType() == tracked) {
      storedLocations.put(player.getUniqueId(), new ItemLocation(vector, slot));
      return;
    }

    // Player takes tracked item out
    if (currentItem != null && currentItem.getType() == tracked) {
      storedLocations.entrySet().stream()
          .filter(
              entry -> {
                UUID hiderId = entry.getKey();
                ItemLocation itemLocation = entry.getValue();
                boolean locationMatch = itemLocation.location.equals(vector);

                return !hiderId.equals(player.getUniqueId())
                    && itemLocation.slot() == slot
                    && locationMatch;
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

  private record ItemLocation(Vector location, int slot) {}
}
