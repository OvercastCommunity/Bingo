package tc.oc.bingo.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;

@Log
public class ConfigHandler {

  private final List<ConfigSetting<?>> configs = new ArrayList<>();

  private <T> ConfigSetting<T> register(ConfigSetting<T> setting) {
    configs.add(setting);
    return setting;
  }

  public void reload(ConfigurationSection section) {
    Set<String> unused = new HashSet<>(section.getKeys(false));
    List<String> undefined = new ArrayList<>();
    configs.forEach(
        config -> {
          config.read(section);
          String key = config.getKey();
          if (key != null && !unused.remove(key)) undefined.add(key);
        });

    if (!unused.isEmpty() || !undefined.isEmpty()) {
      log.warning(
          String.format(
              "Config misuses in section '%s': Unused keys %s, Undefined keys: %s",
              section.getName(), unused, undefined));
    }
  }

  public interface Extensions {

    ConfigHandler getConfig();

    default Supplier<Integer> useConfig(String key, int defaultValue) {
      return getConfig().register(new ConfigSetting<>(key, defaultValue, ConfigReader.INT_READER));
    }

    default Supplier<Boolean> useConfig(String key, boolean defaultValue) {
      return getConfig().register(new ConfigSetting<>(key, defaultValue, ConfigReader.BOOL_READER));
    }

    default Supplier<Double> useConfig(String key, double defaultValue) {
      return getConfig()
          .register(new ConfigSetting<>(key, defaultValue, ConfigReader.DOUBLE_READER));
    }

    default <T> Supplier<T> useConfig(String key, T def, ConfigReader<T> reader) {
      return getConfig().register(new ConfigSetting<>(key, def, reader));
    }

    default <T> Supplier<T> useComputedConfig(Supplier<T> value) {
      return getConfig().register(new ConfigSetting<>(null, value.get(), (f, k, d) -> value.get()));
    }
  }
}
