package tc.oc.bingo.util;

import java.time.Duration;
import javax.annotation.Nullable;

public class Messages {

  public static String getManyString(Integer count) {
    if (count > 10) {
      return "Many, many, many players";
    } else if (count > 1) {
      return "Many, many players";
    } else if (count == 1) {
      return "Many players";
    } else {
      return "No one";
    }
  }

  public static @Nullable String getDurationRemaining(Duration remaining) {
    if (remaining.isNegative()) return null;

    long hours = remaining.toHours();
    if (hours >= 1) {
      if (hours == 1) {
        return hours + " hour";
      }
      return hours + " hours";
    }

    long minutes = remaining.toMinutes();
    if (minutes >= 1) {
      if (minutes == 1) {
        return minutes + " minute";
      }
      return minutes + " minutes";
    }

    return "a moment";
  }
}
