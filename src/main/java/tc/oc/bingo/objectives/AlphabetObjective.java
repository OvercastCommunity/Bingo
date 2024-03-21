package tc.oc.bingo.objectives;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("alphabet-killer")
public class AlphabetObjective extends ObjectiveTracker implements PersistentStore<Integer> {

  private int indexCountRequired = 25;

  public Map<UUID, Integer> alphabetProgress = new HashMap<>();

  @Override
  public void setConfig(ConfigurationSection config) {
    indexCountRequired = config.getInt("index-count-required", 25);
  }

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
    Integer currentIndex = getCurrentIndex(playerId);
    int index = 'A' + currentIndex;

    // Check if current character is in the name
    Integer finalCurrentIndex = currentIndex;
    boolean found =
        event
            .getPlayer()
            .getNameLegacy()
            .toLowerCase(Locale.ROOT)
            .chars()
            .anyMatch(c -> c == index);

    if (!found) return;
    Integer newIndex = ++currentIndex;

    alphabetProgress.put(playerId, newIndex);

    // When they reach the index count required i.e 25 = 'Z'
    storeObjectiveData(player.getBukkit(), getStringForStore(newIndex));
    if (newIndex >= indexCountRequired) {
      reward(player.getBukkit());
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

  @Override
  public Integer getDataFromString(@Nullable String string) {
    if (string == null) return -1;
    return Integer.parseInt(string);
  }

  @Override
  public String getStringForStore(Integer integer) {
    return integer.toString();
  }
}
