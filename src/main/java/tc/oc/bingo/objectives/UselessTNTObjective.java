package tc.oc.bingo.objectives;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.TrackerMatchModule;

@Tracker("useless-tnt")
public class UselessTNTObjective extends ObjectiveTracker {

  private TrackerMatchModule tracker;

  private final Set<UUID> failingPlayers = Collections.newSetFromMap(useState(Scope.LIFE));
  private final int MIN_TNT_DISTANCE = 5 * 5; // Distance squared

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
    if (startLocation == null) return;

    if (startLocation.distanceSquared(event.getLocation().toVector()) < MIN_TNT_DISTANCE) return;

    ParticipantState owner = tracker.getEntityTracker().getOwner(event.getEntity());
    if (owner == null) return;

    if (!event.blockList().isEmpty()) {
      failingPlayers.add(owner.getId());
    }

    boolean failed = failingPlayers.remove(owner.getId());
    if (failed) return;

    MatchPlayer player = getPlayer(owner);
    if (player != null) reward(player.getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.getDamager().getType() != EntityType.PRIMED_TNT) return;

    ParticipantState owner = tracker.getEntityTracker().getOwner(event.getEntity());
    if (owner == null) return;

    failingPlayers.add(owner.getId());
  }
}
