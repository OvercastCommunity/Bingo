package tc.oc.bingo.database;

import java.util.UUID;
import lombok.Data;
import lombok.ToString;
import lombok.extern.java.Log;

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
}
