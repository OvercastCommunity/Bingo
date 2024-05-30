package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.RangedInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.Trackers;

@Tracker("arrow-total-distance")
public class ArrowTotalDistanceObjective extends ObjectiveTracker.Stateful<Double> {

  private TrackerMatchModule tracker;

  private final Supplier<Integer> MIN_ARROW_DISTANCE_COUNT =
      useConfig("min-arrow-distance-count", 10000);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    tracker = event.getMatch().needModule(TrackerMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) { // todo: change name
    if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Arrow)) return;

    ParticipantState owner = tracker.getEntityTracker().getOwner(event.getDamager());
    MatchPlayer player = getStatePlayer(owner);
    if (player == null) return;

    TrackerInfo trackerInfo = tracker.getEntityTracker().resolveInfo(event.getDamager());
    if (!(trackerInfo instanceof RangedInfo)) return;

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
