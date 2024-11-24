package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;

@Tracker("block-head")
public class BlockHeadObjective extends ObjectiveTracker {

  // Require the player to have their head inside an `x` block type
  // Create a ticking task checker (once every `x` ticks) defaulting to 1 second
  // Check only for alive participating match players (whilst match running)

  private final Supplier<Material> BLOCK = useConfig("block-head-type", Material.ICE);

  private TrackerMatchModule tracker;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    tracker = event.getMatch().needModule(TrackerMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerHeadStuck(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;

    if (tracker == null) return;

    if (player.getEyeLocation().getBlock().getType().equals(BLOCK.get())) {
      if (!event.getCause().equals(EntityDamageEvent.DamageCause.SUFFOCATION)) return;
      reward(player);
    }
  }
}
