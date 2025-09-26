package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.modules.DependsOn;
import tc.oc.bingo.modules.GravesModule;

@Tracker("grave-break")
@DependsOn(GravesModule.class)
public class GraveBreakObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_INTERACTIONS = useConfig("required-interactions", 12);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGraveBreak(GravesModule.GraveBreakEvent event) {
    trackProgress(event.getPlayer());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_INTERACTIONS.get();
  }
}
