package tc.oc.bingo;

import co.aikar.commands.BukkitCommandManager;
import fr.minuskube.inv.InventoryManager;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.bingo.commands.CardCommand;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoDatabase;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.SQLDatabase;
import tc.oc.bingo.objectives.Objective;
import tc.oc.bingo.objectives.ObjectiveTracker;
import tc.oc.bingo.objectives.Tracker;
import tc.oc.bingo.util.Reflections;

public class Bingo extends JavaPlugin {

  private static Bingo INSTANCE;

  private BukkitCommandManager commands;
  private List<ObjectiveTracker> trackers;
  private BingoDatabase database;
  private InventoryManager inventoryManager;

  private BingoCard bingoCard = null;
  private HashMap<UUID, BingoPlayerCard> cards = new HashMap<UUID, BingoPlayerCard>();

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
                Objective.class.getPackage().getName(), ObjectiveTracker.class, Tracker.class)
            .stream()
            .map(this::buildTracker)
            .collect(Collectors.toList());

    FileConfiguration config = getConfig();
    trackers.forEach(
        tracker ->
            tracker.setConfig(config.getConfigurationSection(tracker.getObjective().getSlug())));

    PluginManager plMan = getServer().getPluginManager();
    getTrackersOfType(Listener.class).forEach(listener -> plMan.registerEvents(listener, this));

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
    Tracker tracker = trackerCls.getDeclaredAnnotation(Tracker.class);
    // TODO: load definitions from a resource file
    Objective obj = new Objective(tracker.value(), tracker.value(), tracker.value());
    return trackerCls.getConstructor(Objective.class).newInstance(obj);
  }

  public boolean isBingoCardLoaded(UUID playerId) {
    return cards.containsKey(playerId);
  }

  public CompletableFuture<BingoPlayerCard> loadBingoCard(UUID playerId) {
    BingoPlayerCard bingoPlayerCard = cards.get(playerId);
    if (bingoPlayerCard != null) return CompletableFuture.completedFuture(bingoPlayerCard);

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

  public BingoCard getBingoCard() {
    return bingoCard;
  }

  public HashMap<UUID, BingoPlayerCard> getCards() {
    return cards;
  }

  public BingoDatabase getBingoDatabase() {
    return database;
  }

  public InventoryManager getInventoryManager() {
    return inventoryManager;
  }
}
