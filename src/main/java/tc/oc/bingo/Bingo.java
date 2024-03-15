package tc.oc.bingo;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import co.aikar.commands.BukkitCommandManager;
import fr.minuskube.inv.InventoryManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.bingo.commands.CardCommand;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoDatabase;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.bingo.database.SQLDatabase;
import tc.oc.bingo.listeners.PlayerJoinListener;
import tc.oc.bingo.objectives.ObjectiveTracker;
import tc.oc.bingo.objectives.Tracker;
import tc.oc.bingo.util.Messages;
import tc.oc.bingo.util.Reflections;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class Bingo extends JavaPlugin {

  private static Bingo INSTANCE;

  private BukkitCommandManager commands;
  private List<ObjectiveTracker> trackers;
  private BingoDatabase database;
  @Getter private InventoryManager inventoryManager;
  @Getter private BingoCard bingoCard = null;
  @Getter private HashMap<UUID, BingoPlayerCard> cards = new HashMap<UUID, BingoPlayerCard>();

  public Bingo() {
    INSTANCE = this;
  }

  public static Bingo get() {
    return INSTANCE;
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    Config.create(getConfig());

    database = new SQLDatabase();

    trackers =
        Reflections.findClasses(
                ObjectiveTracker.class.getPackage().getName(),
                ObjectiveTracker.class,
                Tracker.class)
            .stream()
            .map(this::buildTracker)
            .collect(Collectors.toList());

    FileConfiguration config = getConfig();
    trackers.forEach(
        tracker -> tracker.setConfig(config.getConfigurationSection(tracker.getObjectiveSlug())));

    PluginManager plMan = getServer().getPluginManager();
    getTrackersOfType(Listener.class).forEach(listener -> plMan.registerEvents(listener, this));

    plMan.registerEvents(new PlayerJoinListener(this), this);

    // Set up the command manager and register all commands
    this.commands = new BukkitCommandManager(this);
    commands.registerCommand(new CardCommand());

    inventoryManager = new InventoryManager(Bingo.get());
    inventoryManager.init();

    loadBingoCard();
  }

  private <T> Stream<T> getTrackersOfType(Class<T> type) {
    return trackers.stream().filter(type::isInstance).map(type::cast);
  }

  @SneakyThrows
  private <T extends ObjectiveTracker> T buildTracker(Class<T> trackerCls) {
    return trackerCls.getConstructor().newInstance();
  }

  public boolean isBingoCardLoaded(UUID playerId) {
    return cards.containsKey(playerId);
  }

  public CompletableFuture<BingoPlayerCard> loadBingoCard(UUID playerId) {
    BingoPlayerCard bingoPlayerCard = cards.get(playerId);
    // TODO: fix false
    if (false && bingoPlayerCard != null) return CompletableFuture.completedFuture(bingoPlayerCard);

    return database
        .getCard(playerId)
        .whenComplete((bingoCard, throwable) -> cards.put(playerId, bingoCard));
  }

  public CompletableFuture<BingoCard> loadBingoCard() {
    return database
        .getCard()
        .whenComplete(
            (bingoCard, throwable) -> {
              this.bingoCard = bingoCard;
              System.out.println(bingoCard.toString());
            });
  }

  public BingoDatabase getBingoDatabase() {
    return database;
  }

  public ProgressItem shouldReward(Player player, String objectiveSlug) {
    BingoPlayerCard bingoPlayerCard = cards.get(player.getUniqueId());
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
        bingoCard.getObjectives().stream()
            .filter(o -> o.getSlug().equals(objectiveSlug))
            .findFirst()
            .orElse(null);

    Match match =
        players.stream()
            .map(player -> PGM.get().getMatchManager().getMatch(player))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

    database
        .rewardPlayers(uuids, objectiveSlug)
        .thenAccept(
            position -> {
              filteredCardItems.forEach(progressItem -> progressItem.setPlacedPosition(position));
              if (objectiveItem != null && match != null && position == 1) {
                // TODO: Broadcast first discovery
                objectiveItem.setComplete(null);
                match.sendMessage(
                    text("(っ◕‿◕)っ ", NamedTextColor.WHITE)
                        .append(
                            text(
                                "This goal has been completed for the first time!",
                                NamedTextColor.GRAY)));
              }
            });

    if (match == null) return;
    if (objectiveItem == null) return;

    TextComponent.Builder bingo =
        text()
            .append(text("[", NamedTextColor.GRAY))
            .append(text("Bingo", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text("] ", NamedTextColor.GRAY))
            .append(text(Messages.getManyString(uuids.size())))
            .append(text(" completed the goal", NamedTextColor.GRAY));

    if (objectiveItem.shouldShowName()) {
      bingo.append(text(" " + objectiveItem.getName(), NamedTextColor.AQUA));
    }

    // TODO: custom message when first, probs called from somewhere else :shrug:
    //  or the act of rewarding populates that field on the above

    match.sendMessage(bingo);
  }

  public void rewardPlayer(String objectiveSlug, Player player) {
    BingoPlayerCard bingoPlayerCard = cards.get(player.getUniqueId());
    if (bingoPlayerCard == null) return;

    Map<String, ProgressItem> progressList = bingoPlayerCard.getProgressList();

    ProgressItem progressItem =
        progressList.computeIfAbsent(
            objectiveSlug, slug -> new ProgressItem(player.getUniqueId(), slug, false, null, ""));

    if (progressItem.isCompleted()) return;

    progressItem.setComplete();

    ObjectiveItem objectiveItem =
        bingoCard.getObjectives().stream()
            .filter(o -> o.getSlug().equals(objectiveSlug))
            .findFirst()
            .orElse(null);

    Match match = PGM.get().getMatchManager().getMatch(player);

    database
        .rewardPlayer(player.getUniqueId(), objectiveSlug)
        .thenAccept(
            placedPosition -> {
              progressItem.setPlacedPosition(placedPosition);
              if (objectiveItem != null && match != null && placedPosition == 1) {
                objectiveItem.setComplete(player.getUniqueId());
                // TODO: Broadcast first discovery
                match.sendMessage(
                    text("(っ◕‿◕)っ ", NamedTextColor.WHITE)
                        .append(
                            text(
                                "This goal has been completed for the first time!",
                                NamedTextColor.GRAY)));
              }
            });

    // TODO: if first set the player as the discovery
    if (objectiveItem == null) return;

    if (match == null) return;
    MatchPlayer matchPlayer = match.getPlayer(player);
    if (matchPlayer == null) return;

    TextComponent.Builder bingo =
        text()
            .append(text("[", NamedTextColor.GRAY))
            .append(text("Bingo", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text("] ", NamedTextColor.GRAY))
            .append(matchPlayer.getName())
            .append(text(" completed the goal", NamedTextColor.GRAY));

    if (objectiveItem.shouldShowName()) {
      TextComponent name =
          text(" " + objectiveItem.getName(), NamedTextColor.AQUA)
              .hoverEvent(
                  showText(
                      text("Click to run ", NamedTextColor.GRAY)
                          .append(
                              text("/bingo", NamedTextColor.YELLOW, TextDecoration.UNDERLINED))))
              .clickEvent(runCommand("/bingo"));

      bingo.append(name);
    }

    // TODO: custom message when first, probs called from somewhere else :shrug:
    //  or the act of rewarding populates that field on the above

    match.sendMessage(bingo);
  }

  public void storeObjectiveData(UUID uuid, String objectiveSlug, String dataAsString) {
    database.storePlayerProgress(uuid, objectiveSlug, dataAsString);
  }
}
