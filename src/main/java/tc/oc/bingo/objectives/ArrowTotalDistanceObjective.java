package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.RangedInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.tracker.trackers.EntityTracker;

@Tracker("arrow-total-distance")
public class ArrowTotalDistanceObjective extends ObjectiveTracker.Stateful<Double> {

  private EntityTracker tracker;

  private final Supplier<Integer> MIN_ARROW_DISTANCE_COUNT =
      useConfig("min-arrow-distance-count", 10000);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchAfterLoad(MatchAfterLoadEvent event) {
    tracker = event.getMatch().needModule(TrackerMatchModule.class).getEntityTracker();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) { // todo: change name
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

    trackArrowDistance(player.getBukkit(), distance);
  }

  private void trackArrowDistance(Player player, Double arrowDistance) {
    if (arrowDistance == null) return;

    Double totalDistance = updateObjectiveData(player.getUniqueId(), d -> d + arrowDistance);

    if (totalDistance >= MIN_ARROW_DISTANCE_COUNT.get()) {
      reward(player);
    }
  }

  @Override
  public @NotNull Double initial() {
    return 0d;
  }

  @Override
  public @NotNull Double deserialize(@NotNull String string) {
    if (string.isEmpty()) return initial();
    return Double.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Double data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Double data) {
    return data / MIN_ARROW_DISTANCE_COUNT.get();
  }
}
