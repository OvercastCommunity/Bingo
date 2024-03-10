package tc.oc.bingo;

import co.aikar.commands.BukkitCommandManager;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.objectives.Objective;
import tc.oc.bingo.objectives.ObjectiveTracker;
import tc.oc.bingo.objectives.Tracker;
import tc.oc.bingo.util.Reflections;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Bingo extends JavaPlugin {

  private static Bingo INSTANCE;

  private BukkitCommandManager commands;
  private List<ObjectiveTracker> trackers;

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

    trackers = Reflections.findClasses(Objective.class.getPackage().getName(), ObjectiveTracker.class, Tracker.class)
            .stream()
            .map(this::buildTracker)
            .collect(Collectors.toList());

    FileConfiguration config = getConfig();
    trackers.forEach(tracker ->
            tracker.setConfig(config.getConfigurationSection(tracker.getObjective().getSlug())));

    PluginManager plMan = getServer().getPluginManager();
    getTrackersOfType(Listener.class).forEach(listener -> plMan.registerEvents(listener, this));
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

}
