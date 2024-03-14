package tc.oc.bingo.database;

import java.util.UUID;
import lombok.Data;

@Data
public class ProgressItem {

  private UUID playerUUID;
  private String objectiveSlug;
  private boolean completed;
  private Integer placedPosition;
  private String data;

  public ProgressItem(
      UUID playerUUID,
      String objectiveSlug,
      boolean completed,
      Integer placedPosition,
      String data) {
    this.playerUUID = playerUUID;
    this.objectiveSlug = objectiveSlug;
    this.completed = completed;
    this.placedPosition = placedPosition;
    this.data = data;
  }

  public void setComplete() {
    this.completed = true;
  }
}
