package tc.oc.bingo.card;

import static net.kyori.adventure.text.Component.text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.bingo.util.Messages;
import tc.oc.occ.dispense.events.currency.CurrencyType;
import tc.oc.occ.dispense.events.currency.PlayerEarnCurrencyEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class RewardManager implements Listener {

  private final Bingo bingo;

  public RewardManager(Bingo bingo) {
    this.bingo = bingo;
    Bukkit.getServer().getPluginManager().registerEvents(this, Bingo.get());
  }

  @EventHandler
  public void onRaindropEarn(PlayerEarnCurrencyEvent event) {
    if (!Config.get().isDebug()) return;
    event
        .getPlayer()
        .sendMessage(event.getCustomAmount() + " " + event.getReason() + " " + event.getReason());
  }

  public ProgressItem shouldReward(Player player, String objectiveSlug) {
    BingoPlayerCard bingoPlayerCard = bingo.getCards().get(player.getUniqueId());
    if (bingoPlayerCard == null) return null;

    Map<String, ProgressItem> progressList = bingoPlayerCard.getProgressList();

    ProgressItem progressItem =
        progressList.computeIfAbsent(
            objectiveSlug, slug -> new ProgressItem(player.getUniqueId(), slug, false, null, ""));

    boolean completed = progressItem.isCompleted();
    if (completed) return null;

    progressItem.setComplete();

    return progressItem;
  }

  public void rewardPlayers(String objectiveSlug, List<Player> players) {

    List<ProgressItem> filteredCardItems =
        players.stream()
            .map(player -> shouldReward(player, objectiveSlug))
            .collect(Collectors.toList());

    if (filteredCardItems.isEmpty()) return;

    List<UUID> uuids =
        filteredCardItems.stream().map(ProgressItem::getPlayerUUID).collect(Collectors.toList());

    ObjectiveItem objectiveItem =
        bingo.getBingoCard().getObjectives().stream()
            .filter(o -> o.getSlug().equals(objectiveSlug))
            .findFirst()
            .orElse(null);

    Match match =
        players.stream()
            .map(player -> PGM.get().getMatchManager().getMatch(player))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

    bingo
        .getBingoDatabase()
        .rewardPlayers(uuids, objectiveSlug)
        .thenAccept(
            position -> {
              filteredCardItems.forEach(progressItem -> progressItem.setPlacedPosition(position));
              if (objectiveItem != null && match != null && position == 1) {
                objectiveItem.setComplete(null);
                match.sendMessage(Messages.getFirstCompletion());
              }
            });

    if (match == null) return;
    if (objectiveItem == null) return;

    match.sendMessage(
        Messages.goalCompleted(text(Messages.getManyString(uuids.size())), objectiveItem));

    // TODO: reward players with raindrops
  }

  public void rewardPlayer(String objectiveSlug, Player player) {
    BingoPlayerCard bingoPlayerCard = bingo.getCards().get(player.getUniqueId());
    if (bingoPlayerCard == null) return;

    Map<String, ProgressItem> progressList = bingoPlayerCard.getProgressList();

    ProgressItem progressItem =
        progressList.computeIfAbsent(
            objectiveSlug, slug -> new ProgressItem(player.getUniqueId(), slug, false, null, ""));

    if (progressItem.isCompleted()) return;

    progressItem.setComplete();

    ObjectiveItem objectiveItem =
        bingo.getBingoCard().getObjectives().stream()
            .filter(o -> o.getSlug().equals(objectiveSlug))
            .findFirst()
            .orElse(null);

    Match match = PGM.get().getMatchManager().getMatch(player);

    bingo
        .getBingoDatabase()
        .rewardPlayer(player.getUniqueId(), objectiveSlug)
        .thenAccept(
            placedPosition -> {
              progressItem.setPlacedPosition(placedPosition);
              if (objectiveItem != null && match != null && placedPosition == 1) {
                objectiveItem.setComplete(player.getUniqueId());
                match.sendMessage(Messages.getFirstCompletion());
              }
            });

    if (objectiveItem == null) return;

    if (match == null) return;
    MatchPlayer matchPlayer = match.getPlayer(player);
    if (matchPlayer == null) return;

    match.sendMessage(Messages.goalCompleted(matchPlayer.getName(), objectiveItem));

    RewardType rewardType =
        issueRaindropRewards(player, bingo.getBingoCard(), bingoPlayerCard, progressItem);

    if (rewardType.isBroadcast()) {
      match.sendMessage(Messages.getRewardTypeBroadcast(matchPlayer, rewardType));
    }
  }

  public void storeObjectiveData(UUID uuid, String objectiveSlug, String dataAsString) {
    bingo.getBingoDatabase().storePlayerProgress(uuid, objectiveSlug, dataAsString);
  }

  public RewardType issueRaindropRewards(
      Player player, BingoCard bingoCard, BingoPlayerCard playerCard, ProgressItem completedItem) {
    Reward reward = getCompletionType(bingoCard, playerCard, completedItem);
    int rewardAmount = getRewardAmount(reward.type);

    if (rewardAmount == 0) return null;

    rewardAmount = rewardAmount * reward.amount;

    if (rewardAmount != 0) {
      Bukkit.getPluginManager()
          .callEvent(
              new PlayerEarnCurrencyEvent(
                  player,
                  CurrencyType.CUSTOM,
                  true,
                  rewardAmount,
                  "Bingo Goal " + reward.type.getName()));

      return reward.type;
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

  public Reward getCompletionType(
      BingoCard bingoCard, BingoPlayerCard playerCard, ProgressItem completedItem) {
    List<ObjectiveItem> bingoItems = bingoCard.getObjectives();
    Map<String, ProgressItem> playerItems = playerCard.getProgressList();

    // Map to store the completion status of each item in the player's card
    int completedIndex = -1;
    Map<Integer, Boolean> completionMap = new HashMap<>();
    for (ObjectiveItem item : bingoItems) {
      ProgressItem progressItem = playerItems.get(item.getSlug());
      if (item.getSlug().equals(completedItem.getObjectiveSlug())) {
        completedIndex = item.getIndex();
      }
      completionMap.put(item.getIndex(), progressItem != null && progressItem.isCompleted());
    }

    int completedX = completedIndex / 5;
    int completedY = completedIndex % 5;

    // Check for horizontal line containing the completed item
    boolean horizontalLine = true;
    for (int j = 0; j < 5; j++) {
      if (!completionMap.getOrDefault(completedX * 5 + j, false)) {
        horizontalLine = false;
        break;
      }
    }

    // Check for vertical line containing the completed item
    boolean verticalLine = true;
    for (int i = 0; i < 5; i++) {
      if (!completionMap.getOrDefault(i * 5 + completedY, false)) {
        verticalLine = false;
        break;
      }
    }

    // TODO: check logic
    // Check for diagonal lines containing the completed item
    boolean diagonal1Line = true;
    boolean diagonal2Line = true;
    for (int i = 0; i < 5; i++) {
      if (!completionMap.getOrDefault(i * 5 + i, false)) {
        diagonal1Line = false;
      }
      if (!completionMap.getOrDefault(i * 5 + (4 - i), false)) {
        diagonal2Line = false;
      }
    }

    int lines = 0;

    if (horizontalLine) lines++;
    if (verticalLine) lines++;
    if (diagonal1Line) lines++;
    if (diagonal2Line) lines++;

    if (lines > 2) {
      boolean fullHouse = true;
      for (Boolean completionStatus : completionMap.values()) {
        if (!completionStatus) {
          fullHouse = false;
          break;
        }
      }

      if (fullHouse) {
        return new Reward(RewardType.CARD);
      }
    }

    if (lines != 0) {
      return new Reward(RewardType.LINE, lines);
    }

    return new Reward(RewardType.SINGLE);
  }

  public static class Reward {

    private final RewardType type;
    private final int amount;

    public Reward(RewardType type) {
      this.type = type;
      this.amount = 0;
    }

    public Reward(RewardType type, int amount) {
      this.type = type;
      this.amount = amount;
    }
  }
}
