package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;

@Tracker("picky-voter")
public class PickyVoterObjective extends ObjectiveTracker {

  public HashMap<UUID, Vector> placedWater = new HashMap<>();

  private static final int MIN_FALL_HEIGHT = 100;

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerOnGroundChanged(final PlayerOnGroundEvent event) {

    //    if (mapOrder instanceof MapPoolManager) {
    //      MapPoolManager manager = (MapPoolManager) mapOrder;
    //      if (manager.getActiveMapPool() instanceof VotingPool) {
    //        VotingPool votePool = (VotingPool) manager.getActiveMapPool();
    //        if (votePool.getCurrentPoll() != null) {

  }
}
