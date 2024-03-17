package tc.oc.bingo.card;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.bingo.Bingo;

public class CardRefresher {

  public static final int REFRESH_INTERVAL_SECONDS = 60;
  private final BukkitTask bukkitTask;

  public CardRefresher() {
    bukkitTask =
        new BukkitRunnable() {
          @Override
          public void run() {
            Bingo.get().loadBingoCard();
          }
        }.runTaskTimerAsynchronously(Bingo.get(), 0, REFRESH_INTERVAL_SECONDS * 20);
  }
}
