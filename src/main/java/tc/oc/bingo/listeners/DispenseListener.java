package tc.oc.bingo.listeners;

import java.util.EnumSet;
import java.util.Set;
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
public class DispenseListener implements Listener {

  public static class Factory {
    public static void create(Bingo bingo) {
      try {
        DispenseListener dispenseListener = new DispenseListener();
        Bukkit.getPluginManager().registerEvents(dispenseListener, bingo);
      } catch (ClassNotFoundException|NoClassDefFoundError ignored) {
        log.warning("[Bingo] Could not find Dispense. Dispense listener disabled.");
      }
    }
  }

  //  /**
  //   * Factory method to create a DispenseListener.
  //   */
  //  public static void create(Bingo bingo) {
  //    DispenseListener dispenseListener = new DispenseListener();
  //    Bukkit.getPluginManager().registerEvents(dispenseListener, bingo);
  //  }

  private final Set<CurrencyType> IGNORE_REASONS =
      EnumSet.of(
          CurrencyType.SCORE,
          CurrencyType.WIN,
          CurrencyType.PARTICIPATE,
          CurrencyType.MAP_VOTE,
          CurrencyType.SPORTSMANSHIP,
          CurrencyType.SPORTSMANSHIP_LOSER,
          CurrencyType.SPORTSMANSHIP_OBS);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerEarnCurrency(PlayerEarnCurrencyEvent event) {
    boolean postMatch = IGNORE_REASONS.contains(event.getType());
    Player player = event.getPlayer();
    Integer amount = event.getCustomAmount();

    Bukkit.getPluginManager().callEvent(new PlayerRaindropEvent(player, amount, postMatch));
  }

  public static class PlayerRaindropEvent extends Event {

    private final Player player;
    private final Integer amount;
    private final boolean postMatch;

    private static final HandlerList handlers = new HandlerList();

    public PlayerRaindropEvent(Player player, Integer amount, boolean postMatch) {
      this.player = player;
      this.amount = amount;
      this.postMatch = postMatch;
    }

    public Player getPlayer() {
      return player;
    }

    public Integer getAmount() {
      return amount;
    }

    public boolean isPostMatch() {
      return postMatch;
    }
  }
}
