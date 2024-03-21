package tc.oc.bingo.config;

import lombok.Getter;
import org.bukkit.configuration.Configuration;

@Getter
public class Config {

  private static Config config;
  private boolean enabled;
  @Getter private boolean debug;

  // Settings
  private int gridWidth;

  // Rewards
  private int rewardSingle;
  private int rewardLine;
  private int rewardCard;

  public Config(Configuration config) {
    load(config);
  }

  public static void create(Configuration config) {
    Config.config = new Config(config);
  }

  public static Config get() {
    return config;
  }

  public void load(Configuration config) {
    this.enabled = config.getBoolean("enabled", true);
    this.debug = config.getBoolean("debug", false);

    this.gridWidth = config.getInt("grid-width", 5);

    this.rewardSingle = config.getInt("rewards.single", 100);
    this.rewardLine = config.getInt("rewards.line", 250);
    this.rewardCard = config.getInt("rewards.card", 5000);
  }
}
