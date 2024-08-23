package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

@Tracker("cauldron-fill")
public class CauldronFillObjective extends ObjectiveTracker {

  private final Map<UUID, Location> cauldronFilled = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (event.getClickedBlock().getType() != Material.CAULDRON) return;

    ItemStack itemInHand = event.getItem();
    if (itemInHand == null) return;

    UUID playerId = event.getPlayer().getUniqueId();

    if (itemInHand.getType() == Material.WATER_BUCKET) {
      cauldronFilled.put(playerId, event.getClickedBlock().getLocation());
      return;
    }

    if (itemInHand.getType() == Material.GLASS_BOTTLE) {
      // Handle using a glass bottle on the cauldron
      Location filledLocation = cauldronFilled.get(playerId);
      if (filledLocation == null) return;

      if (filledLocation.equals(event.getClickedBlock().getLocation())) {
        reward(event.getPlayer());
      }
    }
  }
}
