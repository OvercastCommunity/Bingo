package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@Tracker("cactus-hugger")
public class CactusHuggerObjective extends ObjectiveTracker {

  private final Supplier<Integer> CACTUS_HEIGHT = useConfig("cactus-height", 5);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDamageByBlock(EntityDamageByBlockEvent event) {
    if (!(event.getEntity() instanceof Player)) return;
    Player player = (Player) event.getEntity();
    if (event.getCause().equals(EntityDamageEvent.DamageCause.CONTACT) && isCactusTall(event)) {
      reward(player);
    }
  }

  private boolean isCactusTall(EntityDamageByBlockEvent event) {
    Block currentBlock = event.getDamager();
    for (int i = 0; i < CACTUS_HEIGHT.get(); i++) {
      if (Material.CACTUS != currentBlock.getType()) return false;
      currentBlock = currentBlock.getRelative(BlockFace.UP);
    }
    return true;
  }
}
