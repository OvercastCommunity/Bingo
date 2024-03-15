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
public class AlphabetObjective extends ObjectiveTracker {

  private int indexCountRequired = 25;

  public Map<UUID, Integer> alphabetProgress = new HashMap<>();

  //  @Override
  //  public void setConfig(ConfigurationSection config) {
  //    indexCountRequired = config.getInt("index-count-required", 25);
  //  }

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

    // Convert killed players name to alphabet index
    char firstChar = event.getPlayer().getNameLegacy().toUpperCase(Locale.ROOT).charAt(0);
    if (!Character.isAlphabetic(firstChar)) return;
    int killIndex = firstChar - 'A' + 1;

    UUID playerId = player.getId();
    Integer currentIndex = getCurrentIndex(playerId);

    if (currentIndex + 1 != killIndex) return;

    Integer newIndex = ++currentIndex;

    alphabetProgress.put(playerId, newIndex);

    // When they reach the index count required i.e 25 = 'Z'
    if (newIndex == indexCountRequired) {
      reward(player.getBukkit());
    } else {
      storeObjectiveData(player.getBukkit(), newIndex.toString());
    }
  }

  public Integer getCurrentIndex(UUID playerId) {
    // Create or fetch progress item to cache
    if (!alphabetProgress.containsKey(playerId)) {
      Integer index = 0;
      ProgressItem progressItem = getProgress(playerId);

      if (progressItem != null) {
        index = getDataFromString(progressItem.getData());
      }

      alphabetProgress.put(playerId, index);
      return index;
    }

    return alphabetProgress.get(playerId);
  }

  public Integer getDataFromString(@Nullable String string) {
    if (string == null) return -1;
    return Integer.parseInt(string);
  }
}
