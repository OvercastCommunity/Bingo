package tc.oc.bingo.objectives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.card.BingoRewardEvent;
import tc.oc.bingo.card.RewardType;

@Tracker("line-get")
public class LineGetObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBingoReward(BingoRewardEvent event) {
    // TODO: delay by 1 seconds
    if (event.getReward().getType().equals(RewardType.LINE)) {
      reward(event.getPlayer());
    }
  }
}
