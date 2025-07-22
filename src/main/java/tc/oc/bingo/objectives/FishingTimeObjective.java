package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import tc.oc.pgm.api.map.GameRule;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;

@Tracker("fishing-time")
public class FishingTimeObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_CATCHES = useConfig("required-catches", 5);

  private final Supplier<Integer> MIN_TIME = useConfig("min-time", 13000);
  private final Supplier<Integer> MAX_TIME = useConfig("max-time", 18000);

  private boolean isEnabled = false;

  @EventHandler
  public void onMatchLoad(MatchAfterLoadEvent event) {
    String gameRuleValue = event.getWorld().getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE.getId());
    isEnabled = Boolean.parseBoolean(gameRuleValue);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerFish(PlayerFishEvent event) {
    if (!isEnabled) return;

    long time = event.getWorld().getTime();
    if (time < MIN_TIME.get() || time > MAX_TIME.get()) return;

    reward(event.getPlayer());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_CATCHES.get();
  }
}
