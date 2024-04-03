package tc.oc.bingo.util;

import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.occ.dispense.events.currency.CurrencyType;
import tc.oc.occ.dispense.events.currency.PlayerEarnCurrencyEvent;

@Log
public class Raindrops {

  private static final Handler HANDLER;

  static {
    Handler tmp;
    try {
      tmp = new DispenseHandler();
    } catch (NoClassDefFoundError ignore) {
      log.warning("[Bingo] Could not find class Dispense. Disabling rewards...");
      tmp = new NoOpHandler();
    }
    HANDLER = tmp;
  }

  public static void reward(Player player, int amount, String reason) {
    HANDLER.reward(player, amount, reason);
  }

  private interface Handler {
    default void reward(Player player, int amount, String reason) {}
  }

  private static class DispenseHandler implements Handler {
    @Override
    public void reward(Player player, int amount, String reason) {
      Bukkit.getPluginManager()
          .callEvent(
              new PlayerEarnCurrencyEvent(player, CurrencyType.CUSTOM, true, amount, reason));
    }
  }

  private static class NoOpHandler implements Handler {}
}
