package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.InventoryUtils;

@Tracker("entity-jockey")
public class EntityJockeyObjective extends ObjectiveTracker {

  private final Supplier<EntityType> ENTITY_TYPE = useConfig("entity-type", EntityType.CHICKEN);

  @EventHandler
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    Player player = event.getPlayer();
    MatchPlayer matchPlayer = getPlayer(player);
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    if (event.getRightClicked().getType() != ENTITY_TYPE.get()) return;

    ItemStack itemInHand = player.getInventory().getItemInHand();
    if (itemInHand == null || itemInHand.getType() != Material.SADDLE) return;

    event.getRightClicked().setPassenger(player);
    InventoryUtils.consumeItem(event);

    reward(player);
  }
}
