package tc.oc.bingo.util;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import java.time.Duration;
import java.util.Random;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.bingo.card.RewardType;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.pgm.api.player.MatchPlayer;

public class Messages {

  private static final String[] FACES = {
    "\\(◕ ◡ ◕\\)",
    "(っ◕‿◕)っ ",
    "(✿◠‿◠)",
    "\\ (•◡•) /",
    "(ಠ⌣ಠ)",
    "^•^*",
    "(• ω •)",
    "╰(*´︶`*)╯",
    "（＾◡＾）♡",
    "(◔ᴥ◔)",
  };

  public static String getManyString(Integer count) {
    if (count > 10) {
      return "Many, many, many players";
    } else if (count > 1) {
      return "Several players";
    } else if (count == 1) {
      return "Someone";
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

  public static Component getRewardTypeBroadcast(MatchPlayer player, RewardType rewardType) {
    return text(getHappyFace())
        .append(player.getName())
        .append(text(" has completed a " + rewardType.getName(), NamedTextColor.GRAY));
  }

  public static Component getBingoPrefix() {
    return text("")
        .append(text("[", NamedTextColor.GRAY))
        .append(text("Bingo", NamedTextColor.GOLD, TextDecoration.BOLD))
        .append(text("] ", NamedTextColor.GRAY));
  }

  public static String getHappyFace() {
    Random random = new Random();
    int index = random.nextInt(FACES.length);
    return FACES[index];
  }

  public static Component getFirstCompletion() {
    return text(Messages.getHappyFace() + " ", NamedTextColor.WHITE)
        .append(text("This goal has been completed for the first time!", NamedTextColor.GRAY));
  }

  public static Component goalCompleted(Component completer, ObjectiveItem objectiveItem) {
    TextComponent objectiveName =
        objectiveItem.shouldShowName()
            ? text(objectiveItem.getName(), NamedTextColor.AQUA)
            : text("NiceTry", NamedTextColor.AQUA, TextDecoration.OBFUSCATED);

    return Messages.getBingoPrefix()
        .append(completer)
        .append(text(" completed the goal", NamedTextColor.GRAY))
        .append(space())
        .append(
            objectiveName
                .hoverEvent(
                    showText(
                        text("Click to see your progress using ", NamedTextColor.GRAY)
                            .append(
                                text("/bingo", NamedTextColor.YELLOW, TextDecoration.UNDERLINED))))
                .clickEvent(runCommand("/bingo " + objectiveItem.getIndex())));
  }
}
