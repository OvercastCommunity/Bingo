package tc.oc.bingo.database;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BingoDatabase {

  CompletableFuture<BingoCard> getCard();

  CompletableFuture<BingoPlayerCard> getPlayerCard(UUID playerId);

  CompletableFuture<Integer> rewardPlayers(List<UUID> players, String objectiveSlug);

  CompletableFuture<Void> storePlayerProgress(
      UUID playerId, String objectiveSlug, String dataAsString);
}
