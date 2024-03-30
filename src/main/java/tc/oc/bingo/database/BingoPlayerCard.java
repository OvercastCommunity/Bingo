package tc.oc.bingo.database;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class BingoPlayerCard {
  private final UUID playerUUID;
  private final Map<String, ProgressItem> progressMap;

  public ProgressItem getProgress(String objectiveSlug) {
    return progressMap.computeIfAbsent(
        objectiveSlug, slug -> new ProgressItem(this, slug, false, null, null));
  }
}
