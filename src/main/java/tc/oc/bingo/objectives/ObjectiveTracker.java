package tc.oc.bingo.objectives;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Data;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

@Data
public class ObjectiveTracker implements Listener {

  private final String objectiveSlug;

  public ObjectiveTracker() {
    this.objectiveSlug = getClass().getDeclaredAnnotation(Tracker.class).value();
  }

  public void setConfig(ConfigurationSection config) {}

  public @Nullable String getDataAsString(ProgressItem progressItem) {
    return null;
  }

  public @Nullable ProgressItem getProgress(UUID playerId) {
    BingoPlayerCard bingoPlayerCard = Bingo.get().getCards().get(playerId);
    if (bingoPlayerCard == null) return null;

    ProgressItem progressItem = bingoPlayerCard.getProgressList().get(objectiveSlug);
    if (progressItem == null) return null;

    return progressItem;
  }

  public void storeObjectiveData(Player player, String dataAsString) {
    Bingo.get().storeObjectiveData(player.getUniqueId(), getObjectiveSlug(), dataAsString);
  }

  public void reward(List<Player> players) {
    Bingo.get().rewardPlayers(objectiveSlug, players);
  }

  public void reward(Player player) {
    Bingo.get().rewardPlayer(objectiveSlug, player);
  }

  public @Nullable Match getMatch(World world) {
    return PGM.get().getMatchManager().getMatch(world);
  }
}
