package tc.oc.bingo;

import co.aikar.commands.BukkitCommandManager;
import fr.minuskube.inv.InventoryManager;
import java.util.HashMap;
import java.util.List;
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
import tc.oc.bingo.modules.BingoModule;
import tc.oc.bingo.modules.CarePackageModule;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.modules.FreezerModule;
import tc.oc.bingo.modules.ItemRemoveCanceller;
import tc.oc.bingo.objectives.ObjectiveTracker;
import tc.oc.bingo.objectives.Tracker;
import tc.oc.bingo.util.Exceptions;
import tc.oc.bingo.util.Reflections;
import tc.oc.bingo.util.StringUtils;
import tc.oc.pgm.api.PGM;

@Log
@Getter
public class Bingo extends JavaPlugin {

  private static Bingo INSTANCE;

  // Plugin state
  private final Map<String, ObjectiveTracker> trackers = new HashMap<>(25);
  private final Map<Class<? extends BingoModule>, BingoModule> modules = new HashMap<>();
  private final Map<UUID, BingoPlayerCard> cards = new HashMap<>();
  private BingoCard bingoCard = null;

  // Plugin classes and utils
  @Getter(AccessLevel.NONE)
  private BingoDatabase database;

  private PlayerJoinListener playerJoinListener;
  private RewardManager rewards;
  private BukkitCommandManager commands;
  private InventoryManager inventoryManager;
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

    this.playerJoinListener = new PlayerJoinListener(this);
    List.of(
            CarePackageModule.INSTANCE,
            CustomItemModule.INSTANCE,
            FreezerModule.INSTANCE,
            ItemRemoveCanceller.INSTANCE)
        .forEach(m -> modules.put(m.getClass(), m));

    this.rewards = new RewardManager(this);

    // Set up the command manager and register all commands
    this.commands = new BukkitCommandManager(this);
    this.commands.registerCommand(new CardCommand());

    this.inventoryManager = new InventoryManager(this);
    this.inventoryManager.init();

    this.cardRefresher = new CardRefresher();

    reloadTrackerConfigs();
  }

  public void reloadTrackerConfigs() {
    trackers.values().forEach(t -> t.reloadConfig(getConfig()));
    modules.values().forEach(m -> m.reloadConfig(getConfig()));
  }

  @SneakyThrows
  private <T extends ObjectiveTracker> T buildTracker(Class<T> trackerCls, String slug) {
    T tracker = trackerCls.getConstructor().newInstance();
    tracker.setObjectiveSlug(slug);
    return tracker;
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
            .filter(ObjectiveItem::hasUnlocked)
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
      // Build a map of tracker classes keyed by their annotation value
      Map<String, Class<? extends ObjectiveTracker>> trackerClassMap = new HashMap<>();

      // Find tracker classes not present in current map, and create them
      Reflections.findClasses(
              ObjectiveTracker.class.getPackage().getName(), ObjectiveTracker.class, Tracker.class)
          .forEach(
              trackerClass -> {
                Tracker annotation = trackerClass.getAnnotation(Tracker.class);
                String slug = annotation != null ? annotation.value() : null;
                if (slug != null) {
                  trackerClassMap.put(slug, trackerClass);
                }
              });

      objectivesToCreate.forEach(
          fullSlug -> {
            StringUtils.SplitSlug slug = StringUtils.splitSlug(fullSlug);

            Class<? extends ObjectiveTracker> trackerClass = trackerClassMap.get(slug.tracker());
            if (trackerClass == null) return;

            ObjectiveTracker tracker = buildTracker(trackerClass, fullSlug);
            tracker.reloadConfig(getConfig());
            tracker.enable();
            trackers.put(fullSlug, tracker);
          });
      modules.values().forEach(m -> m.reloadConfig(getConfig()));
    }
  }
}
