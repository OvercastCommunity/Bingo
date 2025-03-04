package tc.oc.bingo.objectives;

import java.util.Collection;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.tracker.TrackerMatchModule;

@Tracker("anvil-smasher")
public class AnvilSmasher extends ObjectiveTracker {

  private TrackerMatchModule tracker;

  private final Supplier<Material> BLOCK_REQUIRED = useConfig("falling-block-name", Material.ANVIL);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) { // TODO: switch to match after load event
    tracker = event.getMatch().needModule(TrackerMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityBlockForm(EntityChangeBlockEvent event) {
    if (tracker == null || BLOCK_REQUIRED.get() == null) return;

    if (!event.getBlock().isEmpty() || !event.getTo().equals(BLOCK_REQUIRED.get())) {
      return;
    }

    Location bottomLocation =
        event.getBlock().getLocation().toCenterLocation().clone().subtract(0, 0.5, 0);

    Collection<Item> nearbyEntitiesByType =
        event.getWorld().getNearbyEntitiesByType(Item.class, bottomLocation, 0.3);

    if (nearbyEntitiesByType.isEmpty()) return;

    PhysicalInfo physicalInfo = tracker.getBlockTracker().resolveBlock(event.getBlock());
    ParticipantState owner = physicalInfo.getOwner();
    if (owner == null) return;

    MatchPlayer matchPlayer = owner.getPlayer().orElse(null);
    if (matchPlayer == null) return;

    reward(matchPlayer.getBukkit());
  }
}
