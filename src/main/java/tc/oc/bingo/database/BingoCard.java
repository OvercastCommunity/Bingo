package tc.oc.bingo.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.config.Config;

@Data
@Log
public class BingoCard {
  private static final int SIZE = Config.get().getGridWidth() * Config.get().getGridWidth();

  private final List<@Nullable ObjectiveItem> objectives;

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
        log.warning(String.format("Objective %s has invalid idx %d", objective.getSlug(), idx));
        continue;
      }
      if (arr[idx] != null) {
        log.warning(
            String.format(
                "Objectives %s & %s clash @ idx %s", arr[idx].getSlug(), objective.getSlug(), idx));
        continue;
      }
      arr[idx] = objective;
    }
    String unused =
        IntStream.range(0, SIZE)
            .filter(i -> arr[i] == null)
            .mapToObj(String::valueOf)
            .collect(Collectors.joining(","));
    if (!unused.isEmpty())
      log.warning("[Bingo] Index(es) " + unused + " have no associated objective");
  }

  public @Nullable ObjectiveItem getObjectiveBySlug(String slug) {
    return bySlug.get(slug);
  }

  public @Nullable ObjectiveItem getObjectiveByIndex(int index) {
    return objectives.get(index);
  }
}
