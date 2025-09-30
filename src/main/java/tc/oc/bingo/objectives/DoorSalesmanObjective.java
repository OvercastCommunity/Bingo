package tc.oc.bingo.objectives;

import java.util.*;
import java.util.function.Supplier;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.material.TrapDoor;

@Log
@Tracker("door-salesman")
public class DoorSalesmanObjective extends ObjectiveTracker {

  // Generic open door and give item objective
  // 1. A player opens a door
  // 2. The player throws a specific item (product-item)
  // 3. A different player must pick it up within x seconds of the first player opening the door (x:
  // sale-opportunity-time)

  // Intended to be used for a "trick or treat" objective, but door salesman sounded funnier
  // internally

  private final Supplier<Material> PRODUCT = useConfig("product-item", Material.GOLDEN_APPLE);
  private final Supplier<Integer> SALE_TIME = useConfig("sale-opportunity-time", 8);

  private final Map<UUID, Long> doorOpened = useState(Scope.LIFE);
  private final Map<UUID, UUID> droppedProducts = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public final void onDoorOpen(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (isDoorOpen(event.getClickedBlock())) return;
    doorOpened.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public final void onItemDrop(PlayerDropItemEvent event) {
    if (event.getItemDrop().getItemStack().getType() != PRODUCT.get()) return;
    if (passesVibeCheck(event.getPlayer().getUniqueId())) {
      droppedProducts.put(event.getPlayer().getUniqueId(), event.getItemDrop().getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public final void onItemPickup(PlayerPickupItemEvent event) {
    if (event.getItem().getItemStack().getType() != PRODUCT.get()) return;
    droppedProducts.forEach(
        (player, item) -> {
          if (event.getItem().getUniqueId() != item) return;
          if (!passesVibeCheck(player)) return;

          Player thrower = Bukkit.getPlayer(player);
          if (thrower == null || thrower.getUniqueId() == event.getPlayer().getUniqueId()) return;
          reward(thrower);
        });
  }

  public boolean passesVibeCheck(UUID uuid) {
    if (!doorOpened.containsKey(uuid)) return false;

    return doorOpened.getOrDefault(uuid, 0L) > System.currentTimeMillis() - SALE_TIME.get() * 1000;
  }

  private boolean isDoorOpen(Block block) {
    MaterialData materialData = block.getState().getMaterialData();

    if (materialData instanceof Door door) {
      // Top doors do not report the state change
      if (door.isTopHalf()) {
        return isDoorOpen(block.getRelative(BlockFace.DOWN));
      }

      return door.isOpen();
    }

    if (materialData instanceof TrapDoor trapDoor) {
      return trapDoor.isOpen();
    }

    return false;
  }
}
