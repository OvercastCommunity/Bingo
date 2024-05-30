package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.tracker.TrackerMatchModule;

@Tracker("tnt-launcher")
public class TNTLauncherObjective extends ObjectiveTracker {

  private TrackerMatchModule tracker;

  private final Supplier<Integer> MIN_TNT_DISTANCE = useConfig("min-tnt-distance", 60);

  private final Map<Integer, Vector> primedLocations = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchLoadEvent event) {
    tracker = event.getMatch().needModule(TrackerMatchModule.class);
    primedLocations.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityPrime(final ExplosionPrimeEvent event) {
    if (!(event.getEntity() instanceof TNTPrimed)) return;

    primedLocations.put(
        event.getEntity().getEntityId(), event.getEntity().getLocation().toVector());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityExplode(final EntityExplodeEvent event) {
    if (!(event.getEntity() instanceof TNTPrimed)) return;

    Vector startLocation = primedLocations.remove(event.getEntity().getEntityId());

    Vector endLocation = event.getEntity().getLocation().toVector();

    double distanceTraveled = getXZDistance(startLocation, endLocation);

    if (distanceTraveled < MIN_TNT_DISTANCE.get()) return;

    MatchPlayer player = getStatePlayer(tracker.getOwner(event.getEntity()));
    if (player == null) return;

    reward(player.getBukkit());
  }

  // TODO: event for entity despawn? if it doesnt blow up?

  private double getXZDistance(Vector v1, Vector v2) {
    double dx = v1.getX() - v2.getX();
    double dz = v1.getZ() - v2.getZ();
    return Math.hypot(dx, dz);
  }
}
