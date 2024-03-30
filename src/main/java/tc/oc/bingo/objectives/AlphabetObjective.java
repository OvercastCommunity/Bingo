package tc.oc.bingo.objectives;

import java.util.Locale;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("alphabet-killer")
public class AlphabetObjective extends ObjectiveTracker.Stateful<Character> {

  private static final char FIRST_CHAR = 'A';
  private static final char LAST_CHAR = 'Z';

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    // TODO: permission checks here too?

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    UUID playerId = player.getId();
    char currentChar = getObjectiveData(playerId);

    // Check if current character is in the name
    boolean found =
        event
            .getPlayer()
            .getBukkit()
            .getName(player.getBukkit())
            .toUpperCase(Locale.ROOT)
            .contains(currentChar + "");

    if (!found) return;

    // When they reach 'Z' reward
    if (currentChar >= LAST_CHAR) {
      storeObjectiveData(player.getId(), '-');
      reward(player.getBukkit());
    } else {
      currentChar++; // bump current char before storing
      storeObjectiveData(player.getId(), currentChar);
    }
  }

  @Override
  public @NotNull Character initial() {
    return FIRST_CHAR;
  }

  @Override
  public @NotNull Character deserialize(@NotNull String string) {
    return string.charAt(0);
  }

  @Override
  public @NotNull String serialize(@NotNull Character data) {
    return data + "";
  }
}
