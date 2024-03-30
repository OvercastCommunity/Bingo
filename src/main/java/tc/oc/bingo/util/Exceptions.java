package tc.oc.bingo.util;

import java.util.concurrent.CompletableFuture;
import lombok.extern.java.Log;

@Log
public class Exceptions {

  public static <T> CompletableFuture<T> handle(CompletableFuture<T> completableFuture) {
    completableFuture.whenComplete(
        (t, throwable) -> {
          if (throwable != null) {
            log.warning(throwable.getMessage());
            throwable.printStackTrace();
          }
        });

    return completableFuture;
  }
}
