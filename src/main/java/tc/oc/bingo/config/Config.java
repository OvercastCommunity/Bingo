package tc.oc.bingo.config;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import java.time.Duration;
import org.bukkit.configuration.Configuration;

public class Config {

  private static Config config;
  private boolean enabled;
  private boolean debug;

  // Selection
  private int minKillstreak;
  private Duration startsAfter;
  private boolean allowDuplicate;

  // Bounty
  private Duration maxDuration;
  private boolean particles;

  // Rewards
  private int killReward;
  private int survivalReward;

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

    this.minKillstreak = config.getInt("selection.min-killstreak", 3);
    this.startsAfter = parseDuration(config.getString("selection.starts-after", "1m"));
    this.allowDuplicate = config.getBoolean("selection.allow-duplicate", false);

    this.maxDuration = parseDuration(config.getString("bounty.max-duration", "2m"));
    this.particles = config.getBoolean("rewards.particles", true);

    this.killReward = config.getInt("rewards.kill-reward", 50);
    this.survivalReward = config.getInt("rewards.survival-reward", 10);
  }

  public boolean getEnabled() {
    return enabled;
  }

  public boolean isDebug() {
    return debug;
  }

  public int getMinKillstreak() {
    return minKillstreak;
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

  public int getKillReward() {
    return killReward;
  }

  public int getSurvivalReward() {
    return survivalReward;
  }
}
