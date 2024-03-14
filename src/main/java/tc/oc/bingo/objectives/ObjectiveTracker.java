package tc.oc.bingo.objectives;

import javax.annotation.Nullable;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.bingo.Bingo;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

@Data
public class ObjectiveTracker implements Listener {

  private final String objectiveSlug;

  public ObjectiveTracker() {
    this.objectiveSlug = getClass().getDeclaredAnnotation(Tracker.class).value();
  }

  public void setConfig(ConfigurationSection config) {}

  public boolean progress(Player player, Object data) {
    // TODO: do
    return true;
  }

  public boolean reward(Player player) {

    Bingo.get().rewardPlayer(objectiveSlug, player);

    Bukkit.broadcastMessage(player + " completed " + objectiveSlug);
    return true;
  }

  public @Nullable Match getMatch(World world) {
    return PGM.get().getMatchManager().getMatch(world);
  }
}
