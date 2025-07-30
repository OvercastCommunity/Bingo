package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import tc.oc.pgm.api.map.GameRule;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;

@Tracker("midnight-snack")
public class MidnightSnackObjective extends ObjectiveTracker {

  private final Supplier<Double> CYCLE_CHANCE = useConfig("cycle-chance", 0.2);

  private boolean isEnabled = false;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    isEnabled = Math.random() <= CYCLE_CHANCE.get();
  }

  @EventHandler(ignoreCancelled = true)
  public void onMatchStartEvent(MatchStartEvent event) {
    if (isEnabled) {
      event.getWorld().setGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE.getId(), "true");
    }
  }

  @EventHandler
  public void onPlayerEat(PlayerItemConsumeEvent event) {
    if (!isEnabled) return;
    Material item = event.getItem().getType();
    if (!item.isEdible()) return;

    if (isMidnight(event.getWorld())) {
      reward(event.getPlayer());
    }
  }

  private boolean isMidnight(World world) {
    long time = world.getTime();
    return time >= 17000 && time <= 19000;
  }
}
