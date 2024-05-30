package tc.oc.bingo.config;

import org.bukkit.configuration.ConfigurationSection;

@FunctionalInterface
public interface ConfigReader<T> {
  ConfigReader<Integer> INT_READER = ConfigurationSection::getInt;
  ConfigReader<Double> DOUBLE_READER = ConfigurationSection::getDouble;
  ConfigReader<Boolean> BOOL_READER = ConfigurationSection::getBoolean;
  ConfigReader<String> STRING_READER = ConfigurationSection::getString;

  T read(ConfigurationSection section, String key, T defaultValue);
}
