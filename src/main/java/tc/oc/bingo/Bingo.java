package tc.oc.bingo;

import co.aikar.commands.BukkitCommandManager;
import fr.minuskube.inv.InventoryManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
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
import tc.oc.bingo.util.Reflections;
import tc.oc.pgm.api.PGM;

@Log
@Getter
public class Bingo extends JavaPlugin {

  private static Bingo INSTANCE;

  private BukkitCommandManager commands;
  private final Map<String, ObjectiveTracker> trackers = new HashMap<>(25);

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

  public void reloadTrackerConfigs() {
    trackers.values().forEach(this::reloadTrackerConfig);
  }

  private void reloadTrackerConfig(ObjectiveTracker tracker) {
    ConfigurationSection section = getConfig().getConfigurationSection(tracker.getObjectiveSlug());
    if (section != null) tracker.setConfig(section);
    else if (tracker.hasConfig())
      log.warning("Config key for tracker '" + tracker.getObjectiveSlug() + "' not found");
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

    Map<String, ObjectiveTracker> objectivesToRemove = new HashMap<>(trackers);
    objectivesToCreate.removeIf(string -> objectivesToRemove.remove(string) != null);

    objectivesToRemove.forEach(
        (slug, tracker) -> {
          tracker.disable();
          trackers.remove(slug);
        });

    if (!objectivesToCreate.isEmpty()) {
      // Find tracker classes not present in current map, and create them
      Reflections.findClasses(
              ObjectiveTracker.class.getPackage().getName(), ObjectiveTracker.class, Tracker.class)
          .forEach(
              trackerClass -> {
                Tracker annotation = trackerClass.getAnnotation(Tracker.class);
                String slug = annotation != null ? annotation.value() : null;
                if (slug == null || !objectivesToCreate.contains(slug)) return;

                ObjectiveTracker tracker = buildTracker(trackerClass);
                reloadTrackerConfig(tracker);
                tracker.enable();
                trackers.put(slug, tracker);
              });
    }
  }
}
