package tc.oc.bingo;

import co.aikar.commands.BukkitCommandManager;
import fr.minuskube.inv.InventoryManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.bingo.card.CardRefresher;
import tc.oc.bingo.card.RewardManager;
import tc.oc.bingo.commands.CardCommand;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoDatabase;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.SQLDatabase;
import tc.oc.bingo.listeners.PlayerJoinListener;
import tc.oc.bingo.objectives.ObjectiveTracker;
import tc.oc.bingo.objectives.Tracker;
import tc.oc.bingo.util.Reflections;

@Getter
public class Bingo extends JavaPlugin {

  private static Bingo INSTANCE;

  private BukkitCommandManager commands;
  private List<ObjectiveTracker> trackers;

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

    loadTrackerConfigs(getConfig());

    PluginManager plMan = Bukkit.getServer().getPluginManager();
    getTrackersOfType(Listener.class).forEach(listener -> plMan.registerEvents(listener, this));

    plMan.registerEvents(new PlayerJoinListener(this), this);
    this.rewards = new RewardManager(this);

    // Set up the command manager and register all commands
    this.commands = new BukkitCommandManager(this);
    commands.registerCommand(new CardCommand());

    inventoryManager = new InventoryManager(Bingo.get());
    inventoryManager.init();

    this.cardRefresher = new CardRefresher();
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

  public CompletableFuture<BingoPlayerCard> getBingoCard(UUID playerId) {
    BingoPlayerCard bingoPlayerCard = cards.get(playerId);
    if (bingoPlayerCard != null) return CompletableFuture.completedFuture(bingoPlayerCard);

    return database.getCard(playerId);
  }

  public CompletableFuture<BingoPlayerCard> loadBingoCard(UUID playerId) {
    return this.getBingoCard(playerId)
        .whenComplete((bingoCard, throwable) -> cards.put(playerId, bingoCard));
  }

  public CompletableFuture<BingoCard> loadBingoCard() {
    return database
        .getCard()
        .whenComplete(
            (bingoCard, throwable) -> {
              if (throwable != null) return;
              this.bingoCard = bingoCard;
            });
  }

  public BingoDatabase getBingoDatabase() {
    return database;
  }
}
