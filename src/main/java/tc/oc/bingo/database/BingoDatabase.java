package tc.oc.bingo.database;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BingoDatabase {

  CompletableFuture<BingoCard> getCard();

  CompletableFuture<BingoPlayerCard> getCard(UUID playerId);

  CompletableFuture<Integer> rewardPlayer(
      UUID player, String objectiveSlug, ProgressItem progressItem);

  CompletableFuture<Integer> rewardPlayers(List<UUID> players, String objectiveSlug);

  void storePlayerProgress(UUID playerId, String objectiveSlug, Object object);

  CompletableFuture storePlayerCompletion(UUID player, String objectiveSlug, Integer position);

  CompletableFuture storePlayerCompletion(List<UUID> uuids, String objectiveSlug, Integer position);

  CompletableFuture<Void> storeGoalDiscoverer(UUID uuid, String objectiveSlug);
}
