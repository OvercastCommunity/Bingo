package tc.oc.bingo.objectives;

import java.util.concurrent.TimeUnit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.card.BingoRewardEvent;
import tc.oc.bingo.card.RewardType;
import tc.oc.pgm.api.PGM;

@Tracker("line-get")
public class LineGetObjective extends ObjectiveTracker {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBingoReward(BingoRewardEvent event) {
    if (event.getReward().getType().equals(RewardType.LINE)) {
      PGM.get().getExecutor().schedule(() -> reward(event.getPlayer()), 1, TimeUnit.SECONDS);
    }
  }
}
