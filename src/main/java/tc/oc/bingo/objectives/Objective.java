package tc.oc.bingo.objectives;

import lombok.Data;
import tc.oc.pgm.api.player.MatchPlayer;

@Data
public class Objective {
  public final String slug;
  public final String name;
  public final String description;

  public void reward(MatchPlayer player) {}
}
