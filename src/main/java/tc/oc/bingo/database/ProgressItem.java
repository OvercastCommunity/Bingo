package tc.oc.bingo.database;

import java.util.UUID;
import lombok.Data;
import lombok.ToString;
import lombok.extern.java.Log;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.objectives.ObjectiveTracker;

@Log
@Data
public class ProgressItem {

  @ToString.Exclude private final BingoPlayerCard card;

  private String objectiveSlug;
  private boolean completed;
  private Integer placedPosition;
  private String data;

  public ProgressItem(
      BingoPlayerCard card,
      String objectiveSlug,
      boolean completed,
      Integer placedPosition,
      String data) {
    this.card = card;
    this.objectiveSlug = objectiveSlug;
    this.completed = completed;
    this.placedPosition = placedPosition;
    this.data = data;
  }

  public UUID getPlayerUUID() {
    return card.getPlayerUUID();
  }

  public void setComplete() {
    this.completed = true;
  }

  public Double getCompletion() {
    ObjectiveTracker tracker = Bingo.get().getTrackers().get(objectiveSlug);
    if (tracker == null) log.warning("No tracker found for " + objectiveSlug);
    return tracker == null ? null : tracker.getProgress(card.getPlayerUUID());
  }
}
