package tc.oc.bingo.objectives;

import static tc.oc.bingo.config.ConfigReader.MATERIAL_SET_READER;
import static tc.oc.bingo.modules.GravesModule.GRAVE_META;

import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.DependsOn;
import tc.oc.bingo.modules.GravesModule;
import tc.oc.pgm.util.inventory.InventoryUtils;

@Tracker("pay-respect")
@DependsOn(GravesModule.class)
public class PayRespectObjective extends ObjectiveTracker {

  private final Supplier<Set<Material>> REQUIRED_MATERIAL =
      useConfig(
          "material-list", Set.of(Material.RED_ROSE, Material.YELLOW_FLOWER), MATERIAL_SET_READER);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityInteractEvent(PlayerInteractEntityEvent event) {
    ItemStack itemInHand = event.getPlayer().getInventory().getItemInHand();
    if (itemInHand == null) return;

    if (!REQUIRED_MATERIAL.get().contains(itemInHand.getType())) return;

    Entity rightClicked = event.getRightClicked();

    if (rightClicked.hasMetadata(GRAVE_META)) {
      InventoryUtils.consumeItem(event);
      reward(event.getPlayer());
    }
  }
}
