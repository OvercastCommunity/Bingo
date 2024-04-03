package tc.oc.bingo.util;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import lombok.extern.java.Log;

@Log
public class Exceptions {

  public static <T> CompletableFuture<T> handle(CompletableFuture<T> completableFuture) {
    completableFuture.whenComplete(
        (t, throwable) -> {
          if (throwable != null)
            log.log(Level.WARNING, "[Bingo] Unhandled exception occurred: ", throwable);
        });

    return completableFuture;
  }
}
