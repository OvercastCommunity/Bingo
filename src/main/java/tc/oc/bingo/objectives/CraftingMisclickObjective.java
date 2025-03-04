package tc.oc.bingo.objectives;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@Tracker("crafting-misclick")
public class CraftingMisclickObjective extends ObjectiveTracker {

  private final Supplier<Material> MATERIAL_REQUIRED =
      useConfig("material-name", Material.IRON_FENCE);

  private final Set<UUID> craftingPlayers = Collections.newSetFromMap(useState(Scope.LIFE));

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryOpenEvent(InventoryOpenEvent event) {
    Player player = event.getActor();
    if (player == null) return;

    if (event.getInventory().getType() != InventoryType.WORKBENCH
        && event.getInventory().getType() != InventoryType.CRAFTING) return;

    if (checkCondition(player)) {
      craftingPlayers.add(event.getPlayer().getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryCloseEvent(InventoryCloseEvent event) {
    if (event.getInventory().getType() != InventoryType.WORKBENCH
        && event.getInventory().getType() != InventoryType.CRAFTING) return;

    craftingPlayers.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCraftEvent(CraftItemEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;

    if (!craftingPlayers.contains(player.getUniqueId())) return;
    if (!event.getRecipe().getResult().getType().equals(MATERIAL_REQUIRED.get())) return;

    reward(player);
  }

  private boolean checkCondition(Player player) {
    PlayerInventory inventory = player.getInventory();

    // Count iron ingots and blocks in the player's inventory (in ingot form)
    int materialCount = 0;
    for (ItemStack item : inventory.getContents()) {
      if (item != null) {
        if (item.getType() == Material.IRON_INGOT) {
          materialCount += item.getAmount();
        } else if (item.getType() == Material.IRON_BLOCK) {
          materialCount += item.getAmount() * 9;
        }
      }
    }

    // Can't even mistakenly craft the item
    if (materialCount < 6) return false;

    // Check how much iron is required for a set craft
    int ironRequiredForArmor = 0;
    // Get player's armor items
    ItemStack head = player.getInventory().getItem(EquipmentSlot.HEAD);
    ItemStack chest = player.getInventory().getItem(EquipmentSlot.CHEST);
    ItemStack legs = player.getInventory().getItem(EquipmentSlot.LEGS);
    ItemStack feet = player.getInventory().getItem(EquipmentSlot.FEET);

    // Check player's armor and add required iron ingots for non-iron armor
    if (head == null || !head.getType().equals(Material.IRON_HELMET)) ironRequiredForArmor += 5;
    if (chest == null || !chest.getType().equals(Material.IRON_CHESTPLATE))
      ironRequiredForArmor += 8;
    if (legs == null || !legs.getType().equals(Material.IRON_LEGGINGS)) ironRequiredForArmor += 7;
    if (feet == null || !feet.getType().equals(Material.IRON_BOOTS)) ironRequiredForArmor += 4;

    // Doesn't need any armour or doesn't have enough material for it
    if (ironRequiredForArmor == 0 || materialCount < ironRequiredForArmor) return false;

    // Check that they have just enough iron for the craft (with a leeway)
    if (ironRequiredForArmor + 5 < materialCount) return false;

    return true;
  }
}
