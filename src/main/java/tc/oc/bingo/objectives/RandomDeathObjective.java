package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("random-death")
public class RandomDeathObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQ_HITS = useConfig("hit-counts", 10);
  private final Supplier<Double> HIT_CHANCE = useConfig("hit-chance", 0.05);

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onDeath(MatchPlayerDeathEvent event) {
    MatchPlayer player = event.getPlayer();

    if (Math.random() < HIT_CHANCE.get()) {
      trackProgress(player.getBukkit());
    }
  }

  @Override
  protected int maxValue() {
    return REQ_HITS.get();
  }
}
