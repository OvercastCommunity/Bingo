package tc.oc.bingo.card;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.bingo.database.BingoPlayerCard;

public class BingoCardOpenEvent extends Event {

  private final Player player;
  private final BingoPlayerCard bingoPlayerCard;

  private static final HandlerList handlers = new HandlerList();

  public BingoCardOpenEvent(Player player, BingoPlayerCard bingoPlayerCard) {
    this.player = player;
    this.bingoPlayerCard = bingoPlayerCard;
  }

  public BingoPlayerCard getBingoPlayerCard() {
    return bingoPlayerCard;
  }

  public Player getPlayer() {
    return player;
  }
}
