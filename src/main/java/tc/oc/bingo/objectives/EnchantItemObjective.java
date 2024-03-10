package tc.oc.bingo.objectives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;

@Tracker("enchant")
public class EnchantItemObjective extends ObjectiveTracker {

  public EnchantItemObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerEnchant(EnchantItemEvent event) {
    reward(event.getEnchanter());
  }
}
