package tc.oc.bingo.card;

import static net.kyori.adventure.text.Component.text;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.bingo.util.Exceptions;
import tc.oc.bingo.util.LocationUtils;
import tc.oc.bingo.util.Messages;
import tc.oc.occ.dispense.events.currency.CurrencyType;
import tc.oc.occ.dispense.events.currency.PlayerEarnCurrencyEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class RewardManager implements Listener {

  private static final FireworkEffect FIREWORK_EFFECT =
      FireworkEffect.builder()
          .with(FireworkEffect.Type.BURST)
          .withFlicker()
          .withColor(Color.LIME)
          .withFade(Color.BLACK)
          .build();

  private static final int ROCKET_POWER = 0;

  private final Bingo bingo;

  public RewardManager(Bingo bingo) {
    this.bingo = bingo;
    Bukkit.getServer().getPluginManager().registerEvents(this, bingo);
  }

  @EventHandler
  public void onRaindropEarn(PlayerEarnCurrencyEvent event) {
    if (!Config.get().isDebug()) return;
    event
        .getPlayer()
        .sendMessage(event.getCustomAmount() + " " + event.getReason() + " " + event.getReason());
  }

  public void rewardPlayers(String objectiveSlug, List<Player> players) {
    List<ProgressItem> filteredCardItems =
        players.stream()
            .map(player -> tryComplete(player, objectiveSlug))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (filteredCardItems.isEmpty()) return;

    ObjectiveItem objectiveItem = bingo.getBingoCard().getObjectiveBySlug(objectiveSlug);

    Match match =
        players.stream()
            .map(player -> PGM.get().getMatchManager().getMatch(player))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

    reward(filteredCardItems, objectiveItem, match);
  }

  private ProgressItem tryComplete(Player player, String objectiveSlug) {
    BingoPlayerCard card = bingo.getCards().get(player.getUniqueId());
    if (card == null) return null;

    ProgressItem progressItem = card.getProgress(objectiveSlug);
    if (progressItem.isCompleted()) return null;
    progressItem.setComplete();
    return progressItem;
  }

  private void reward(List<ProgressItem> toReward, ObjectiveItem objective, Match match) {
    if (objective == null || match == null) return;

    List<UUID> players =
        toReward.stream().map(ProgressItem::getPlayerUUID).collect(Collectors.toList());

    Exceptions.handle(
        bingo
            .getBingoDatabase()
            .rewardPlayers(players, objective.getSlug())
            .thenAcceptAsync(
                (position) -> postReward(toReward, objective, match, position),
                PGM.get().getExecutor()));
  }

  private void postReward(
      List<ProgressItem> rewarded, ObjectiveItem objectiveItem, Match match, Integer position) {
    rewarded.forEach(item -> item.setPlacedPosition(position));

    // When completed by a single player get their name
    UUID playerUUID = rewarded.size() == 1 ? rewarded.get(0).getPlayerUUID() : null;
    MatchPlayer discoveryPlayer = (playerUUID != null) ? match.getPlayer(playerUUID) : null;

    Component component =
        (discoveryPlayer != null)
            ? discoveryPlayer.getName()
            : text(Messages.getManyString(rewarded.size()));

    match.sendMessage(Messages.goalCompleted(component, objectiveItem));

    if (position == 1) {
      objectiveItem.setComplete(playerUUID);
      match.sendMessage(Messages.getFirstCompletion());
    }

    rewarded.forEach(
        rewardedItem -> {
          Player player = Bukkit.getPlayer(rewardedItem.getPlayerUUID());
          if (player == null) return;

          Reward reward = issueRaindropRewards(player, bingo.getBingoCard(), rewardedItem);

          LocationUtils.spawnFirework(player.getLocation(), FIREWORK_EFFECT, ROCKET_POWER);

          if (reward.getType().isBroadcast()) {
            match.sendMessage(Messages.getRewardTypeBroadcast(player, reward));
          }
        });
  }

  public void storeObjectiveData(UUID uuid, String objectiveSlug, String dataAsString) {
    Exceptions.handle(
        bingo.getBingoDatabase().storePlayerProgress(uuid, objectiveSlug, dataAsString));
  }

  public Reward issueRaindropRewards(
      Player player, BingoCard bingoCard, ProgressItem completedItem) {
    Reward reward = getCompletionType(bingoCard, completedItem);
    int rewardAmount = getRewardAmount(reward.type);

    if (rewardAmount == 0) return null;

    rewardAmount = rewardAmount * reward.amount;

    if (rewardAmount != 0) {
      String rewardExtra = reward.amount > 1 ? " x" + reward.amount : "";
      Bukkit.getPluginManager()
          .callEvent(
              new PlayerEarnCurrencyEvent(
                  player,
                  CurrencyType.CUSTOM,
                  true,
                  rewardAmount,
                  "Bingo Goal " + reward.type.getName() + rewardExtra));

      return reward;
    }
    return null;
  }

  private int getRewardAmount(RewardType type) {
    switch (type) {
      case SINGLE:
        return Config.get().getRewardSingle();
      case LINE:
        return Config.get().getRewardLine();
      case CARD:
        return Config.get().getRewardCard();
      default:
        return 0;
    }
  }

  public Reward getCompletionType(BingoCard bingoCard, ProgressItem completedItem) {
    List<ObjectiveItem> bingoItems = bingoCard.getObjectives();
    Map<String, ProgressItem> playerItems = completedItem.getCard().getProgressMap();

    // Map to store the completion status of each item in the player's card
    int completedIndex = -1;
    Set<Integer> completed = new HashSet<>(25);
    for (ObjectiveItem item : bingoItems) {
      ProgressItem progressItem = playerItems.get(item.getSlug());
      if (item.getSlug().equals(completedItem.getObjectiveSlug())) {
        completedIndex = item.getIndex();
      }
      if (progressItem != null && progressItem.isCompleted()) completed.add(item.getIndex());
    }

    // TODO: check logic with gridWidth added
    int gridWidth = Config.get().getGridWidth();
    int completedX = completedIndex / gridWidth;
    int completedY = completedIndex % gridWidth;

    // Check for horizontal line containing the completed item
    boolean horizontalLine = true;
    for (int j = 0; j < gridWidth; j++) {
      if (!completed.contains(completedX * gridWidth + j)) {
        horizontalLine = false;
        break;
      }
    }

    // Check for vertical line containing the completed item
    boolean verticalLine = true;
    for (int i = 0; i < gridWidth; i++) {
      if (!completed.contains(i * gridWidth + completedY)) {
        verticalLine = false;
        break;
      }
    }

    // TODO: check logic
    // Check for diagonal lines containing the completed item
    boolean diagonal1Line = completedX == completedY;
    boolean diagonal2Line = completedX + completedY == gridWidth - 1;

    if (diagonal1Line || diagonal2Line) {
      for (int i = 0; i < gridWidth; i++) {
        if (!completed.contains(i * gridWidth + i)) {
          diagonal1Line = false;
        }
        if (!completed.contains(i * gridWidth + ((gridWidth - 1) - i))) {
          diagonal2Line = false;
        }
      }
    }

    int lines = 0;

    if (horizontalLine) lines++;
    if (verticalLine) lines++;
    if (diagonal1Line) lines++;
    if (diagonal2Line) lines++;

    if (lines >= 2) {
      boolean fullHouse = completed.size() == bingoItems.size();

      if (fullHouse) {
        return new Reward(RewardType.CARD);
      }
    }

    if (lines > 0) {
      return new Reward(RewardType.LINE, lines);
    }

    return new Reward(RewardType.SINGLE);
  }

  public static class Reward {

    private final RewardType type;
    private final int amount;

    public Reward(RewardType type) {
      this.type = type;
      this.amount = 1;
    }

    public Reward(RewardType type, int amount) {
      this.type = type;
      this.amount = amount;
    }

    public RewardType getType() {
      return type;
    }

    public int getAmount() {
      return amount;
    }
  }
}
