package tc.oc.bingo.card;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.bingo.database.ObjectiveItem;

public class BingoRewardEvent extends Event {

  private final Player player;
  private final ObjectiveItem objectiveItem;
  private final RewardManager.Reward reward;

  private static final HandlerList handlers = new HandlerList();

  public BingoRewardEvent(Player player, ObjectiveItem objectiveItem, RewardManager.Reward reward) {
    this.player = player;
    this.objectiveItem = objectiveItem;
    this.reward = reward;
  }

  public Player getPlayer() {
    return player;
  }

  public ObjectiveItem getObjectiveItem() {
    return objectiveItem;
  }

  public RewardManager.Reward getReward() {
    return reward;
  }
}
