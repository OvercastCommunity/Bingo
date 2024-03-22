package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("alphabet-killer")
public class AlphabetObjective extends ObjectiveTracker implements PersistentStore<Character> {

  private static final char LAST_CHAR = 'Z';

  public Map<UUID, Character> alphabetProgress = new HashMap<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(PlayerQuitEvent event) {
    alphabetProgress.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill()) return;

    ParticipantState killer = event.getKiller();
    if (killer == null) return;

    MatchPlayer player = killer.getPlayer().orElse(null);
    if (player == null) return;

    UUID playerId = player.getId();
    char currentChar = getCurrentIndex(playerId);

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
      reward(player.getBukkit());
    } else {
      currentChar++; // bump current char before storing
      storeObjectiveData(player.getBukkit(), getStringForStore(currentChar));
    }

    alphabetProgress.put(playerId, currentChar);
  }

  public char getCurrentIndex(UUID playerId) {
    // Create or fetch progress item to cache
    if (!alphabetProgress.containsKey(playerId)) {
      char index = 'A';
      ProgressItem progressItem = getProgress(playerId);

      if (progressItem != null) {
        index = getDataFromString(progressItem.getData());
      }

      alphabetProgress.put(playerId, index);
      return index;
    }

    return alphabetProgress.get(playerId);
  }

  @Override
  public Character getDataFromString(@Nullable String string) {
    if (string == null) return 'A';
    return string.charAt(0);
  }

  @Override
  public String getStringForStore(Character character) {
    return character + "";
  }
}
