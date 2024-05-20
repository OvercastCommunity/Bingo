package tc.oc.bingo;

import co.aikar.commands.BukkitCommandManager;
import fr.minuskube.inv.InventoryManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.bingo.card.CardRefresher;
import tc.oc.bingo.card.RewardManager;
import tc.oc.bingo.commands.CardCommand;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoDatabase;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.listeners.PlayerJoinListener;
import tc.oc.bingo.objectives.ObjectiveTracker;
import tc.oc.bingo.objectives.Tracker;
import tc.oc.bingo.util.Exceptions;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.bingo.util.Reflections;
import tc.oc.pgm.api.PGM;

@Getter
public class Bingo extends JavaPlugin {

  private static Bingo INSTANCE;

  private BukkitCommandManager commands;
  private final List<ObjectiveTracker> trackers = new ArrayList<>(25);

  @Getter(AccessLevel.NONE)
  private BingoDatabase database;

  private final Map<UUID, BingoPlayerCard> cards = new HashMap<>();
  private InventoryManager inventoryManager;
  private BingoCard bingoCard = null;
  private RewardManager rewards;
  private CardRefresher cardRefresher;

  public Bingo() {
    INSTANCE = this;
  }

  public static Bingo get() {
    return INSTANCE;
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    Config.load(getConfig());

    this.database = BingoDatabase.build(Config.get().getDatabase());

    this.cardRefresher = new CardRefresher();

    Bukkit.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    this.rewards = new RewardManager(this);

    // Set up the command manager and register all commands
    this.commands = new BukkitCommandManager(this);
    commands.registerCommand(new CardCommand());

    inventoryManager = new InventoryManager(Bingo.get());
    inventoryManager.init();
  }

  public void loadTrackerConfigs(FileConfiguration config) {
    trackers.forEach(
        tracker -> {
          ConfigurationSection section = config.getConfigurationSection(tracker.getObjectiveSlug());
          if (section != null) tracker.setConfig(section);
        });
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

  public CompletableFuture<BingoPlayerCard> getPlayerCard(UUID playerId) {
    BingoPlayerCard bingoPlayerCard = cards.get(playerId);
    if (bingoPlayerCard != null) return CompletableFuture.completedFuture(bingoPlayerCard);

    return loadPlayerCard(playerId);
  }

  public CompletableFuture<BingoPlayerCard> loadPlayerCard(UUID playerId) {
    return database
        .getPlayerCard(playerId)
        .thenApply(
            (bingoCard) -> {
              cards.put(playerId, bingoCard);
              return bingoCard;
            });
  }

  public CompletableFuture<BingoCard> loadBingoCard() {
    return Exceptions.handle(
        database
            .getCard()
            .thenApplyAsync(
                (bingoCard) -> {
                  resyncTrackers(bingoCard);
                  return this.bingoCard = bingoCard;
                },
                PGM.get().getExecutor()));
  }

  public BingoDatabase getBingoDatabase() {
    return database;
  }

  private void resyncTrackers(BingoCard card) {
    Set<String> objectivesToCreate =
        card.getObjectives().stream()
            .filter(Objects::nonNull)
            .map(ObjectiveItem::getSlug)
            .collect(Collectors.toSet());

    Map<String, ObjectiveTracker> objectivesToRemove =
        trackers.stream()
            .collect(Collectors.toMap(ObjectiveTracker::getObjectiveSlug, Function.identity()));

    objectivesToCreate.removeIf(string -> objectivesToRemove.remove(string) != null);

    objectivesToRemove
        .values()
        .forEach(
            tracker -> {
              tracker.disable();
              trackers.remove(tracker);
            });

    List<ObjectiveTracker> newTrackers = new ArrayList<>();
    if (!objectivesToCreate.isEmpty()) {
      // Find trackers that are not present in the current list and create
      newTrackers =
          Reflections.findClasses(
                  ObjectiveTracker.class.getPackage().getName(),
                  ObjectiveTracker.class,
                  Tracker.class)
              .stream()
              .filter(
                  trackerClass -> {
                    Tracker annotation = trackerClass.getAnnotation(Tracker.class);
                    return annotation != null && objectivesToCreate.contains(annotation.value());
                  })
              .map(this::buildTracker)
              .collect(Collectors.toList());
    }

    newTrackers.forEach(
        tracker -> {
          ConfigurationSection section =
              getConfig().getConfigurationSection(tracker.getObjectiveSlug());
          if (section != null) tracker.setConfig(section);
        });

    newTrackers.forEach(ManagedListener::enable);

    trackers.addAll(newTrackers);
  }
}
