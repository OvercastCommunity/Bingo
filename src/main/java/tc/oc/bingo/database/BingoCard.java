package tc.oc.bingo.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.java.Log;

@Data
@Log
public class BingoCard {
  private static final int SIZE = 5 * 5;

  private final List<ObjectiveItem> objectives;

  @Getter(AccessLevel.NONE)
  private final Map<String, ObjectiveItem> bySlug;

  public BingoCard(List<ObjectiveItem> objectives) {
    ObjectiveItem[] arr = new ObjectiveItem[SIZE];
    this.objectives = Arrays.asList(arr);
    this.bySlug =
        Collections.unmodifiableMap(
            objectives.stream()
                .collect(Collectors.toMap(ObjectiveItem::getSlug, Function.identity())));

    for (ObjectiveItem objective : objectives) {
      int idx = objective.getIndex();
      if (idx < 0 || idx >= SIZE) {
        log.warning(
            "[Bingo] Objective "
                + objective.getSlug()
                + " has invalid index "
                + objective.getIndex());
        continue;
      }
      if (objectives.get(idx) != null) {
        log.warning(
            "[Bingo] Objectives "
                + arr[idx].getSlug()
                + " and "
                + objective.getSlug()
                + " clash with index "
                + idx);
        continue;
      }
      arr[idx] = objective;
    }
    for (int i = 0; i < SIZE; i++) {
      if (arr[i] == null) log.warning("[Bingo] Index " + i + " has no associated objective");
    }
  }

  public ObjectiveItem getObjectiveBySlug(String slug) {
    return bySlug.get(slug);
  }

  public ObjectiveItem getObjectiveByIndex(int index) {
    return objectives.get(index);
  }
}
