package tc.oc.bingo.database;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.ToString;
import tc.oc.bingo.config.Config;

@Data
@ToString
public class ObjectiveItem {

  private String slug;
  private String name;
  private String description;
  private int index;
  private String clue;
  private int hintLevel;
  private @Nullable LocalDateTime nextClueUnlock;
  private UUID discoveryUUID;
  private @Nullable LocalDateTime discoveryTime;

  public ObjectiveItem(
      String slug,
      String name,
      String description,
      int index,
      String clue,
      int hintLevel,
      @Nullable LocalDateTime nextClueUnlock,
      UUID discoveryUUID,
      @Nullable LocalDateTime discoveryTime) {
    this.slug = slug;
    this.name = name;
    this.description = description;
    this.index = index;
    this.clue = clue;
    this.hintLevel = hintLevel;
    this.nextClueUnlock = nextClueUnlock;
    this.discoveryUUID = discoveryUUID;
    this.discoveryTime = discoveryTime;
  }

  public boolean shouldShowName() {
    return this.getHintLevel() > 0 || hasNextCluePassed();
  }

  public boolean hasNextCluePassed() {
    if (this.nextClueUnlock == null) return false;

    LocalDateTime now = LocalDateTime.now();
    return now.isAfter(this.nextClueUnlock);
  }

  public int getX() {
    return index % Config.get().getGridWidth();
  }

  public int getY() {
    return index / Config.get().getGridWidth();
  }

  public @Nullable LocalDateTime getNextClueUnlock() {
    return nextClueUnlock;
  }

  public @Nullable LocalDateTime getDiscoveryTime() {
    return discoveryTime;
  }
}
