package tc.oc.bingo.util;

import java.util.EnumSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.bingo.Bingo;
import tc.oc.occ.dispense.events.currency.CurrencyType;
import tc.oc.occ.dispense.events.currency.PlayerEarnCurrencyEvent;

@Log
@UtilityClass
public class Raindrops {
  private static final Handler HANDLER;

  static {
    Handler tmp;
    try {
      tmp = new DispenseHandler();
    } catch (NoClassDefFoundError ignore) {
      log.warning("[Bingo] Could not find class Dispense. Disabling rewards...");
      tmp = new Handler() {};
    }
    HANDLER = tmp;
  }

  public void setupListener(Bingo bingo) {
    if (HANDLER instanceof Listener l) Bukkit.getPluginManager().registerEvents(l, bingo);
  }

  public static void reward(Player player, int amount, String reason) {
    HANDLER.reward(player, amount, reason);
  }

  private interface Handler {
    default void reward(Player player, int amount, String reason) {}
  }

  private static class DispenseHandler implements Handler, Listener {
    private final Set<CurrencyType> IGNORE_REASONS =
        EnumSet.of(
            CurrencyType.SCORE,
            CurrencyType.WIN,
            CurrencyType.PARTICIPATE,
            CurrencyType.MAP_VOTE,
            CurrencyType.SPORTSMANSHIP,
            CurrencyType.SPORTSMANSHIP_LOSER,
            CurrencyType.SPORTSMANSHIP_OBS);

    @Override
    public void reward(Player player, int amount, String reason) {
      Bukkit.getPluginManager()
          .callEvent(
              new PlayerEarnCurrencyEvent(player, CurrencyType.CUSTOM, true, amount, reason));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerEarnCurrency(PlayerEarnCurrencyEvent event) {
      boolean postMatch = IGNORE_REASONS.contains(event.getType());
      Player player = event.getPlayer();
      Integer amount = event.getCustomAmount();

      Bukkit.getPluginManager().callEvent(new PlayerRaindropEvent(player, amount, postMatch));
    }
  }

  @Getter
  @AllArgsConstructor
  public static class PlayerRaindropEvent extends Event {
    private final Player player;
    private final Integer amount;
    private final boolean postMatch;

    private static final HandlerList handlers = new HandlerList();
  }
}
