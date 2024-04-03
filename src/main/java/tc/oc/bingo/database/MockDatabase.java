package tc.oc.bingo.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import tc.oc.bingo.config.Config;

public class MockDatabase implements BingoDatabase {

  // This will inherently memory leak, but that's acceptable for debug/test purposes
  private final Map<UUID, BingoPlayerCard> cardCache = new HashMap<>();

  @Override
  public CompletableFuture<BingoCard> getCard() {
    return CompletableFuture.completedFuture(
        new BingoCard(Config.get().getDatabase().getMockData()));
  }

  @Override
  public CompletableFuture<BingoPlayerCard> getPlayerCard(UUID playerId) {
    return CompletableFuture.completedFuture(
        cardCache.computeIfAbsent(playerId, id -> new BingoPlayerCard(id, new HashMap<>())));
  }

  @Override
  public CompletableFuture<Integer> rewardPlayers(List<UUID> players, String objectiveSlug) {
    return CompletableFuture.completedFuture(1);
  }

  @Override
  public CompletableFuture<Void> storePlayerProgress(
      UUID playerId, String objectiveSlug, String dataAsString) {
    return getPlayerCard(playerId)
        .thenAccept(bpc -> bpc.getProgress(objectiveSlug).setData(dataAsString));
  }
}
