package tc.oc.bingo.database;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.config.Config;

@Data
public class ObjectiveItem {

  private final String slug;
  private final String name;
  private final String description;
  private final int index;
  private final int hintLevel;
  private final @Nullable LocalDateTime nextClueUnlock;
  private final @Nullable LocalDateTime lockedUntil;
  private @Nullable UUID discoveryUUID;
  private @Nullable LocalDateTime discoveryTime;

  public ObjectiveItem(
      String slug,
      String name,
      String description,
      int index,
      int hintLevel,
      @Nullable LocalDateTime nextClueUnlock,
      @Nullable LocalDateTime lockedUntil,
      @Nullable UUID discoveryUUID,
      @Nullable LocalDateTime discoveryTime) {
    this.slug = slug;
    this.name = name;
    this.description = description;
    this.index = index;
    this.hintLevel = hintLevel;
    this.nextClueUnlock = nextClueUnlock;
    this.lockedUntil = lockedUntil;
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

  public boolean hasUnlocked() {
    if (this.lockedUntil == null) return true;

    LocalDateTime now = LocalDateTime.now();
    return now.isAfter(this.lockedUntil);
  }

  public void setComplete(@Nullable UUID discoveryUUID) {
    this.discoveryUUID = discoveryUUID;
    this.discoveryTime = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
  }

  public int getX() {
    return index % Config.get().getGridWidth();
  }

  public int getY() {
    return index / Config.get().getGridWidth();
  }

  public String getGridPosition() {
    return (char) ('A' + getY()) + Integer.toString(getX() + 1);
  }

  public @Nullable LocalDateTime getNextClueUnlock() {
    return nextClueUnlock;
  }

  public @Nullable LocalDateTime getLockedUntil() {
    return lockedUntil;
  }

  public @Nullable LocalDateTime getDiscoveryTime() {
    return discoveryTime;
  }
}
