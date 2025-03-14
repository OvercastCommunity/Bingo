package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import tc.oc.bingo.card.BingoCardOpenEvent;

@Tracker("card-opener")
public class CardOpenerObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_OPENS = useConfig("required-opens", 50);

  public void onBingoCardOpen(BingoCardOpenEvent event) {
    System.out.println("Bingo card opened?");
    trackProgress(event.getPlayer());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_OPENS.get();
  }
}
