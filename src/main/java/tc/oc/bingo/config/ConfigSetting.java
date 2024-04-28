package tc.oc.bingo.config;

import java.util.function.Supplier;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;

@Data
class ConfigSetting<T> implements Supplier<T> {
  protected final String key;
  private final T defaultValue;
  private final ConfigReader<T> reader;
  private T value;

  ConfigSetting(String key, T defaultValue, ConfigReader<T> reader) {
    this.key = key;
    this.value = this.defaultValue = defaultValue;
    this.reader = reader;
  }

  public void read(ConfigurationSection section) {
    value = reader.read(section, key, defaultValue);
  }

  @Override
  public T get() {
    return value;
  }
}
