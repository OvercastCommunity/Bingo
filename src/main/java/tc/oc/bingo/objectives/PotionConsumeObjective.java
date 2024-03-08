package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;

public class PotionConsumeObjective extends ObjectiveTracker {

  public PotionConsumeObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPotionConsume(PlayerItemConsumeEvent event) {
    if (event.getItem().getType().equals(Material.POTION)) {

      PotionMeta meta = (PotionMeta) event.getItem().getItemMeta();
      if (meta.getCustomEffects().isEmpty()) return;

      reward(event.getPlayer());
    }
  }
}
