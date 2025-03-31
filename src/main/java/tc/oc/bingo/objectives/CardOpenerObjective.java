package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.bingo.card.BingoCardOpenEvent;

@Tracker("card-opener")
public class CardOpenerObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_OPENS = useConfig("required-opens", 50);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBingoCardOpen(BingoCardOpenEvent event) {
    trackProgress(event.getPlayer());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_OPENS.get();
  }
}
