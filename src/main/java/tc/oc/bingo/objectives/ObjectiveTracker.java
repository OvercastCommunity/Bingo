package tc.oc.bingo.objectives;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

import javax.annotation.Nullable;

@Data
public class ObjectiveTracker implements Listener {
  private final Objective objective;

  public void setConfig(ConfigurationSection config) {
  }

  public boolean reward(Player player) {
    Bukkit.broadcastMessage(player + " completed " + this.objective.name);
    return true;
  }

  public @Nullable Match getMatch(World world) {
    return PGM.get().getMatchManager().getMatch(world);
  }
}
