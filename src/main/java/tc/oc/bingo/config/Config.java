package tc.oc.bingo.config;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import java.time.Duration;
import lombok.Getter;
import org.bukkit.configuration.Configuration;

@Getter
public class Config {

  private static Config config;
  private boolean enabled;
  @Getter private boolean debug;

  // Selection
  private int gridWidth;
  private Duration startsAfter;
  private boolean allowDuplicate;

  // Bounty
  private Duration maxDuration;
  private boolean particles;

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

    this.startsAfter = parseDuration(config.getString("selection.starts-after", "1m"));
    this.allowDuplicate = config.getBoolean("selection.allow-duplicate", false);

    this.maxDuration = parseDuration(config.getString("bounty.max-duration", "2m"));
    this.particles = config.getBoolean("rewards.particles", true);
  }

  public boolean getEnabled() {
    return enabled;
  }

  public int getMinKillstreak() {
    return gridWidth;
  }

  public Duration getStartsAfter() {
    return startsAfter;
  }

  public boolean isAllowDuplicate() {
    return allowDuplicate;
  }

  public Duration getMaxDuration() {
    return maxDuration;
  }

  public boolean isParticles() {
    return particles;
  }
}
