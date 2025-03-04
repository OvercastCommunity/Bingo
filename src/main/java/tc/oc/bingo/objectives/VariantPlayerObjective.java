package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("variant-player")
public class VariantPlayerObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<String> REQUIRED_VARIANT = useConfig("required-variant", "christmas");
  private final Supplier<Integer> REQUIRED_MATCHES = useConfig("required-matches", 25);

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {

    String variantId = event.getMatch().getMap().getVariant().getVariantId();
    if (!variantId.equals(REQUIRED_VARIANT.get())) return;

    trackProgress(event.getMatch().getParticipants().stream().map(MatchPlayer::getBukkit).toList());
  }

  @Override
  protected int maxValue() {
    return REQUIRED_MATCHES.get();
  }
}
