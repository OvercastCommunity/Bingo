package tc.oc.bingo.util;

import java.util.function.Predicate;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

  public static boolean containsAny(Inventory inventory, Predicate<ItemStack> match) {
    for (ItemStack itemStack : inventory) {
      if (itemStack != null && match.test(itemStack)) return true;
    }
    return false;
  }
}
