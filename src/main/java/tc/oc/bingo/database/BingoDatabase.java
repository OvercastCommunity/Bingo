package tc.oc.bingo.database;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import tc.oc.bingo.config.Config;
import tc.oc.occ.database.Database;

public interface BingoDatabase {

  CompletableFuture<BingoCard> getCard();

  CompletableFuture<BingoPlayerCard> getPlayerCard(UUID playerId);

  CompletableFuture<Integer> rewardPlayers(List<UUID> players, String objectiveSlug);

  CompletableFuture<Void> storePlayerProgress(
      UUID playerId, String objectiveSlug, String dataAsString);

  static BingoDatabase build(Config.Database config) {
    switch (config.getType().toLowerCase(Locale.ROOT)) {
      case "mock":
        return new MockDatabase();
      case "primary":
        return new SQLDatabase(Database.get().getConnectionPool());
      case "secondary":
        return new SQLDatabase(Database.get().getSecondaryPool());
      default:
        throw new IllegalArgumentException("Unsupported database type: " + config.getType());
    }
  }
}
