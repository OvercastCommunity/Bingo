package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;

@Tracker("icarus-height")
public class IcarusObjective extends ObjectiveTracker {

  public Map<UUID, Vector> placedWater = new HashMap<>();

  private int minRiseHeight = 100;
  private TrackerMatchModule tracker = null;

  @Override
  public void setConfig(ConfigurationSection config) {
    minRiseHeight = config.getInt("min-rise-height", 100);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    tracker = event.getMatch().getModule(TrackerMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerOnGroundChanged(final PlayerOnGroundEvent event) {}
}
