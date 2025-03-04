package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("item-whack")
public class ItemWhackObjective extends ObjectiveTracker {

  private final Supplier<Material> MATERIAL_REQUIRED = useConfig("material-name", Material.STICK);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player)) return;
    if (!(event.getDamager() instanceof Player)) return;

    Player damager = (Player) event.getDamager();
    MatchPlayer matchPlayer = getPlayer(damager);
    if (matchPlayer == null) return;

    if (damager.getItemInHand().getType().equals(MATERIAL_REQUIRED.get())) {
      reward(damager);
    }
  }
}
