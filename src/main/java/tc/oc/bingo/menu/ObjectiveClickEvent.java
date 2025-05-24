package tc.oc.bingo.menu;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.database.ObjectiveItem;

@Getter
public class ObjectiveClickEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  private final Player player;
  private final @Nullable ObjectiveItem objectiveItem;
  private final InventoryClickEvent inventoryClickEvent;

  public ObjectiveClickEvent(
      Player player,
      @Nullable ObjectiveItem objectiveItem,
      InventoryClickEvent inventoryClickEvent) {
    this.player = player;
    this.objectiveItem = objectiveItem;
    this.inventoryClickEvent = inventoryClickEvent;
  }
}
