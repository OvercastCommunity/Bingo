package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.tracker.info.GenericFallInfo;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;
import tc.oc.pgm.util.material.Materials;

@Tracker("water-dropper")
public class WaterDropperObjective extends ObjectiveTracker {

  public Map<UUID, Vector> placedWater = useState(Scope.LIFE);

  private int minFallHeight = 100;
  private TrackerMatchModule tracker = null;

  @Override
  public void setConfig(ConfigurationSection config) {
    minFallHeight = config.getInt("min-fall-height", 100);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    tracker = event.getMatch().getModule(TrackerMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    if (tracker == null) return;

    Block relative = event.getBlockClicked().getRelative(event.getBlockFace());

    // Don't allow placing water in water
    if (Materials.isWater(relative.getType())) return;

    Location location = relative.getLocation().toCenterLocation();

    location.setY(Math.floor(location.getY()));

    placedWater.put(event.getPlayer().getUniqueId(), location.toVector());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerOnGroundChanged(final PlayerOnGroundEvent event) {
    if (tracker == null) return;

    if (!event.getOnGround()) return;

    Match match = getMatch(event.getWorld());
    if (match == null) return;

    UUID playerId = event.getPlayer().getUniqueId();
    Location location = event.getPlayer().getLocation();

    Vector lastWaterVector = placedWater.get(playerId);
    if (lastWaterVector == null) return;

    double distanceFromWater = location.toVector().distance(lastWaterVector);
    // TODO: correct?
    // Half a block plus half a hit-box
    double maxRange = 0.5 + 0.3;
    if (distanceFromWater > maxRange) return;

    DamageInfo damageInfo =
        tracker.resolveDamage(EntityDamageEvent.DamageCause.FALL, event.getPlayer());

    if (damageInfo instanceof GenericFallInfo) {

      GenericFallInfo info = (GenericFallInfo) damageInfo;
      double distance = Trackers.distanceFromRanged(info, event.getPlayer().getLocation());

      if (!Double.isNaN(distance) && distance >= minFallHeight) {
        reward(event.getPlayer());
      }
    }
  }
}
