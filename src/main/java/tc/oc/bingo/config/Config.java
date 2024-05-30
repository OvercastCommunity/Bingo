package tc.oc.bingo.config;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.bingo.database.ObjectiveItem;

@Getter
public class Config {
  private static Config INSTANCE;

  // Settings
  private int gridWidth;
  private int seasonId;

  // Rewards
  private int rewardSingle;
  private int rewardLine;
  private int rewardCard;

  // Database settings
  private final Database database = new Database();

  @Getter
  public static class Database {
    private String type;
    private List<ObjectiveItem> mockData;

    public void init(ConfigurationSection config) {
      this.type = config.getString("type");
      if (type.equalsIgnoreCase("mock")) {
        this.mockData =
            config.getMapList("mock-data").stream()
                .map(
                    map ->
                        new ObjectiveItem(
                            (String) map.get("slug"),
                            (String) map.get("name"),
                            (String) map.get("description"),
                            (int) map.get("idx"),
                            99,
                            null,
                            null,
                            null))
                .collect(Collectors.toList());
      } else {
        mockData = null;
      }
    }
  }

  private Config(Configuration config) {
    init(config);
  }

  public static void load(Configuration config) {
    INSTANCE = new Config(config);
  }

  public static Config get() {
    if (INSTANCE == null) throw new IllegalStateException("Config not loaded");
    return INSTANCE;
  }

  public void init(Configuration config) {
    this.gridWidth = config.getInt("grid-width", 5);

    this.seasonId = config.getInt("season_id", 1);

    this.rewardSingle = config.getInt("rewards.single", 100);
    this.rewardLine = config.getInt("rewards.line", 250);
    this.rewardCard = config.getInt("rewards.card", 5000);

    this.database.init(config.getConfigurationSection("database"));
  }
}
