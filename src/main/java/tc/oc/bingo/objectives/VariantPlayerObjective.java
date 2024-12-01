package tc.oc.bingo.objectives;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.event.MatchFinishEvent;

@Tracker("variant-player")
public class VariantPlayerObjective extends ObjectiveTracker.Stateful<Integer> {

  private final Supplier<String> REQUIRED_VARIANT = useConfig("required-variant", "christmas");
  private final Supplier<Integer> REQUIRED_MATCHES = useConfig("required-matches", 25);

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onMatchFinish(MatchFinishEvent event) {

    String variantId = event.getMatch().getMap().getVariant().getVariantId();
    if (!variantId.equals(REQUIRED_VARIANT.get())) return;

    List<Player> rewardingPlayers =
        event.getMatch().getParticipants().stream()
            .map(
                player -> {
                  Integer matchesPlayed = updateObjectiveData(player.getId(), i -> i + 1);
                  if (matchesPlayed < REQUIRED_MATCHES.get()) return null;
                  return player.getBukkit();
                })
            .collect(Collectors.toList());

    reward(rewardingPlayers);
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return Integer.valueOf(string);
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return String.valueOf(data);
  }

  @Override
  public double progress(Integer data) {
    return (double) data / REQUIRED_MATCHES.get();
  }
}
