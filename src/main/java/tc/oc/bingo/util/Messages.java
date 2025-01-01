package tc.oc.bingo.util;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import java.time.Duration;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.card.RewardManager;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;

public class Messages {

  private static final String[] FACES = {
    "\\(◕ ◡ ◕\\)",
    "(っ◕‿◕)っ",
    "(✿◠‿◠)",
    "\\ (•◡•) /",
    "(ಠ⌣ಠ)",
    "^•^*",
    "(• ω •)",
    "╰(*´︶`*)╯",
    "｡◕‿‿◕｡",
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

    long days = remaining.toDays();
    long hours = remaining.toHours();
    long minutes = remaining.toMinutes() % 60;

    // Cutoff after 6 hours when only hours are shown
    if (hours >= 6 && hours < 24) {
      return hours + " hours";
    }

    // Include both hours and minutes
    if (hours >= 1) {
      return (hours == 1 ? "1 hour" : hours + " hours")
          + (minutes > 0 ? " " + (minutes == 1 ? "1 minute" : minutes + " minutes") : "");
    }

    // If less than 1 hour, only show minutes
    if (minutes >= 1) {
      return minutes == 1 ? "1 minute" : minutes + " minutes";
    }

    return "a moment";
  }

  public static Component getRewardTypeBroadcast(Player player, RewardManager.Reward reward) {
    TextComponent message =
        text(getHappyFace() + " ")
            .append(PlayerComponent.player(player, NameStyle.VERBOSE))
            .append(text(" has completed a ", NamedTextColor.GRAY))
            .append(text(reward.getType().getName(), reward.getType().getColor()));

    if (reward.getAmount() > 1) {
      message = message.append(text(" x" + reward.getAmount(), NamedTextColor.GOLD));
    }

    return message;
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
            : text("")
                .append(text("NiceTry", NamedTextColor.AQUA, TextDecoration.OBFUSCATED))
                .append(text(" (" + objectiveItem.getGridPosition() + ")", NamedTextColor.AQUA));

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
