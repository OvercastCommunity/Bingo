package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.tracker.info.GenericFallInfo;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;
import tc.oc.pgm.util.material.Materials;

@Tracker("icarus-height")
public class IcarusObjective extends ObjectiveTracker {

  public HashMap<UUID, Vector> placedWater = new HashMap<>();

  private static final int MIN_RISE_HEIGHT = 100;

  public IcarusObjective(Objective objective) {
    super(objective);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerOnGroundChanged(final PlayerOnGroundEvent event) {
    if (!event.getOnGround()) return;

    Match match = getMatch(event.getWorld());
    if (match == null) return;

    Location location = event.getPlayer().getLocation();

    Block block = location.getBlock();

    if (Materials.isWater(block.getType())) {

      Vector lastWaterVector = placedWater.getOrDefault(event.getPlayer().getUniqueId(), null);
      if (lastWaterVector == null) return;

      if (lastWaterVector.equals(block.getLocation().toVector())) {

        TrackerMatchModule mm = match.getMatch().getModule(TrackerMatchModule.class);
        if (mm == null) return;

        DamageInfo damageInfo =
            mm.resolveDamage(EntityDamageEvent.DamageCause.FALL, event.getPlayer());

        if (damageInfo instanceof GenericFallInfo) {

          GenericFallInfo info = (GenericFallInfo) damageInfo;
          double distance = Trackers.distanceFromRanged(info, event.getPlayer().getLocation());

          if (!Double.isNaN(distance) && distance >= MIN_RISE_HEIGHT) {
            event.getPlayer().sendMessage("Distance: " + distance);
            reward(event.getPlayer());
          }
        }
      }
    }
  }
}
