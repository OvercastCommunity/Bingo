package tc.oc.bingo.objectives;

import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.menu.ObjectiveClickEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("objective-click")
public class ObjectiveClickedObjective extends ObjectiveTracker {

  private final Supplier<Material> MATERIAL_REQUIRED = useConfig("material-name", (Material) null);
  private final Supplier<Integer> MATERIAL_DATA = useConfig("material-data", -1);

  @EventHandler
  public void onObjectiveClick(ObjectiveClickEvent event) {
    Player player = event.getPlayer();
    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    // Check that clicked objective is the one we're tracking
    ObjectiveItem objectiveItem = event.getObjectiveItem();
    if (objectiveItem == null) return;
    if (!Objects.equals(objectiveItem.getSlug(), getObjectiveSlug())) return;

    // Check clicked material with stored material
    ItemStack currentItem = event.getInventoryClickEvent().getCursor();
    if (currentItem == null) return;

    var material = MATERIAL_REQUIRED.get();
    var data = MATERIAL_DATA.get();

    // Check that item and data is correct
    if (material == null || !currentItem.getType().equals(material)) return;
    if (data != -1 && data != currentItem.getData().getData()) return;

    reward(event.getPlayer());

    // Take item from the player
    event.getInventoryClickEvent().getView().setCursor(null);

    // Force card fresh with a command (easier this way)
    Bukkit.dispatchCommand(player, "bingo " + objectiveItem.getIndex());
  }
}
