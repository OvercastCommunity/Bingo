package tc.oc.bingo.objectives;

import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class ObjectiveTracker implements Listener {

  public Objective objective;

  public boolean completed;

  public ObjectiveTracker(Objective objective) {
    this.objective = objective;
  }

  public boolean isComplete() {
    return completed;
  }

  public boolean reward(Player player) {
    Bukkit.broadcastMessage(player + " completed " + this.objective.name);
    return true;
  }

  public @Nullable Match getMatch(World world) {
    return PGM.get().getMatchManager().getMatch(world);
  }
}
