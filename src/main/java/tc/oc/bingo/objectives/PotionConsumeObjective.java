package tc.oc.bingo.objectives;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;

@Tracker("potion-consume")
public class PotionConsumeObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPotionConsume(PlayerItemConsumeEvent event) {
    if (event.getItem().getType().equals(Material.POTION)) {
      if (event.getItem().getDurability() == 0) return;

      reward(event.getPlayer());
    }
  }
}
