package tc.oc.bingo.database;

import java.util.ArrayList;
import java.util.List;

public class BingoCard {

  private List<ObjectiveItem> objectives = new ArrayList<>();

  public BingoCard(List<ObjectiveItem> objectives) {
    this.objectives = objectives;
  }

  public List<ObjectiveItem> getObjectives() {
    return objectives;
  }

  @Override
  public String toString() {
    return "BingoCard{" + "objectives=" + objectives + '}';
  }
}
