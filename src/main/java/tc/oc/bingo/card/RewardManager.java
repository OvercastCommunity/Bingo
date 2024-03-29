package tc.oc.bingo.card;

import static net.kyori.adventure.text.Component.text;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    List<ProgressCombo> filteredCardItems =
        players.stream()
            .map(player -> processReward(player, objectiveSlug))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (filteredCardItems.isEmpty()) return;

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

    reward(filteredCardItems, objectiveItem, match);
  }

  public void rewardPlayer(String objectiveSlug, Player player) {

    // check permission checks to complete objectives

    ProgressCombo progressCombo = processReward(player, objectiveSlug);

    if (progressCombo == null) return;

    ObjectiveItem objectiveItem =
        bingo.getBingoCard().getObjectives().stream()
            .filter(o -> o.getSlug().equals(objectiveSlug))
            .findFirst()
            .orElse(null);

    Match match = PGM.get().getMatchManager().getMatch(player);

    reward(Collections.singletonList(progressCombo), objectiveItem, match);
  }

  private ProgressCombo processReward(Player player, String objectiveSlug) {
    BingoPlayerCard bingoPlayerCard = bingo.getCards().get(player.getUniqueId());
    if (bingoPlayerCard == null) return null;

    Map<String, ProgressItem> progressList = bingoPlayerCard.getProgressList();

    ProgressItem progressItem =
        progressList.computeIfAbsent(
            objectiveSlug, slug -> new ProgressItem(player.getUniqueId(), slug, false, null, ""));

    if (progressItem.isCompleted()) return null;

    progressItem.setComplete();

    return ProgressCombo.of(bingoPlayerCard, progressItem);
  }

  private void reward(
      List<ProgressCombo> filteredCardItems, ObjectiveItem objectiveItem, Match match) {
    if (objectiveItem == null || match == null) return;

    int position =
        bingo
            .getBingoDatabase()
            .rewardPlayers(
                filteredCardItems.stream()
                    .map(ProgressCombo::getPlayerUUID)
                    .collect(Collectors.toList()),
                objectiveItem.getSlug())
            .join(); // todo: join?

    filteredCardItems.forEach(
        rewardedCombo -> rewardedCombo.progressItem.setPlacedPosition(position));

    // When completed by a single player get their name
    UUID playerUUID =
        filteredCardItems.size() == 1 ? filteredCardItems.get(0).getPlayerUUID() : null;
    MatchPlayer discoveryPlayer = (playerUUID != null) ? match.getPlayer(playerUUID) : null;

    Component component =
        (discoveryPlayer != null)
            ? discoveryPlayer.getName()
            : text(Messages.getManyString(filteredCardItems.size()));

    match.sendMessage(Messages.goalCompleted(component, objectiveItem));

    if (position == 1) {
      objectiveItem.setComplete(playerUUID);
      match.sendMessage(Messages.getFirstCompletion());
    }

    filteredCardItems.forEach(
        rewardedCombo -> {
          // TODO: get as match players earlier?
          MatchPlayer matchPlayer =
              PGM.get().getMatchManager().getPlayer(rewardedCombo.getPlayerUUID());
          if (matchPlayer == null) return;

          Player player = matchPlayer.getBukkit();
          if (player == null) return;

          RewardType rewardType =
              issueRaindropRewards(
                  player,
                  bingo.getBingoCard(),
                  rewardedCombo.playerCard,
                  rewardedCombo.progressItem);

          LocationUtils.spawnFirework(
              player.getLocation(), FIREWORK_EFFECT, ROCKET_POWER);

          if (rewardType.isBroadcast()) {
            match.sendMessage(Messages.getRewardTypeBroadcast(matchPlayer, rewardType));
          }
        });
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
      String rewardExtra = reward.amount > 1 ? " x" + reward.amount : "";
      Bukkit.getPluginManager()
          .callEvent(
              new PlayerEarnCurrencyEvent(
                  player,
                  CurrencyType.CUSTOM,
                  true,
                  rewardAmount,
                  "Bingo Goal " + reward.type.getName() + rewardExtra));

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

    // TODO: check logic with gridWidth added
    int gridWidth = Config.get().getGridWidth();
    int completedX = completedIndex / gridWidth;
    int completedY = completedIndex % gridWidth;

    // Check for horizontal line containing the completed item
    boolean horizontalLine = true;
    for (int j = 0; j < gridWidth; j++) {
      if (!completionMap.getOrDefault(completedX * gridWidth + j, false)) {
        horizontalLine = false;
        break;
      }
    }

    // Check for vertical line containing the completed item
    boolean verticalLine = true;
    for (int i = 0; i < gridWidth; i++) {
      if (!completionMap.getOrDefault(i * gridWidth + completedY, false)) {
        verticalLine = false;
        break;
      }
    }

    // TODO: check logic
    // Check for diagonal lines containing the completed item
    boolean diagonal1Line = true;
    boolean diagonal2Line = true;
    for (int i = 0; i < gridWidth; i++) {
      if (!completionMap.getOrDefault(i * gridWidth + i, false)) {
        diagonal1Line = false;
      }
      if (!completionMap.getOrDefault(i * gridWidth + ((gridWidth - 1) - i), false)) {
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

    if (lines > 0) {
      return new Reward(RewardType.LINE, lines);
    }

    return new Reward(RewardType.SINGLE);
  }

  public static class ProgressCombo {

    public BingoPlayerCard playerCard;
    public ProgressItem progressItem;

    public ProgressCombo(BingoPlayerCard playerCard, ProgressItem progressItem) {
      this.playerCard = playerCard;
      this.progressItem = progressItem;
    }

    public UUID getPlayerUUID() {
      return progressItem.getPlayerUUID();
    }

    public static ProgressCombo of(BingoPlayerCard playerCard, ProgressItem progressItem) {
      return new ProgressCombo(playerCard, progressItem);
    }
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
  }
}
