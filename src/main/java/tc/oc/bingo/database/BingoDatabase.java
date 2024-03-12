package tc.oc.bingo.database;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BingoDatabase {

  CompletableFuture<BingoCard> getCard();

  CompletableFuture<BingoPlayerCard> getCard(UUID playerId);

  void updateObjective(UUID playerId, int amount);
}
