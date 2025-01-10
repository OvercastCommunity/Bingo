package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.RangedInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.tracker.trackers.EntityTracker;

@Tracker("arrow-land-distance")
public class ArrowLandDistanceObjective extends ObjectiveTracker {

  private EntityTracker tracker;

  private final Supplier<Integer> MIN_ARROW_DISTANCE = useConfig("min-arrow-distance", 100);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchAfterLoad(MatchAfterLoadEvent event) {
    tracker = event.getMatch().needModule(TrackerMatchModule.class).getEntityTracker();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player)
        || !(event.getDamager() instanceof Arrow)
        || tracker == null) return;

    TrackerInfo trackerInfo = tracker.resolveInfo(event.getDamager());
    if (!(trackerInfo instanceof OwnerInfo) || !(trackerInfo instanceof RangedInfo)) return;

    MatchPlayer player = getStatePlayer(((OwnerInfo) trackerInfo).getOwner());
    if (player == null) return;
    double distance =
        Trackers.distanceFromRanged((RangedInfo) trackerInfo, event.getActor().getLocation());

    if (Double.isNaN(distance)) return;

    if (distance >= MIN_ARROW_DISTANCE.get()) {
      reward(player.getBukkit());
    }
  }
}
