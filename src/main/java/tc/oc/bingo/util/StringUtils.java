package tc.oc.bingo.util;

import org.jetbrains.annotations.Nullable;

public class StringUtils {

  public record SplitSlug(String tracker, @Nullable String variant) {}

  public static SplitSlug splitSlug(String slug) {
    int colonIndex = slug.indexOf(':');

    // Assume no variant when colon is absent
    if (colonIndex == -1) return new SplitSlug(slug, null);

    String tracker = slug.substring(0, colonIndex);
    String variant = slug.substring(colonIndex + 1);

    return new SplitSlug(tracker, variant.isEmpty() ? null : variant);
  }
}
