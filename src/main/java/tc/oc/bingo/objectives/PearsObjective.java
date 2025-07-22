package tc.oc.bingo.objectives;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.bingo.util.CustomItem;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("pears-pair")
public class PearsObjective extends ObjectiveTracker {

  private final Supplier<Double> PAIR_RANGE = useConfig("player-range", 10d);

  private static final Supplier<CustomItem> PEAR_ITEM = CustomItem.of("pear");

  private final Set<UUID> pearWearers = Collections.newSetFromMap(useState(Scope.LIFE));

  @Override
  public Stream<ManagedListener> children() {
    return Stream.concat(
        super.children(),
        Stream.of(new ManagedListener.Ticker(this::checkPairs, 0, 10, TimeUnit.SECONDS)));
  }

  private void checkPairs() {
    pearWearers.forEach(
        playerId -> {
          MatchPlayer matchPlayer = getPlayer(playerId);
          if (matchPlayer == null) return;
          checkPairs(matchPlayer.getBukkit());
        });
  }

  private void checkPairs(Player player) {
    if (!isWearingPear(player)) return;

    Set<Player> rewardingPlayers =
        player.getNearbyEntities(PAIR_RANGE.get(), PAIR_RANGE.get(), PAIR_RANGE.get()).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .filter(neighbor -> !neighbor.getUniqueId().equals(player.getUniqueId()))
            .filter(this::isWearingPear)
            .collect(Collectors.toSet());

    reward(rewardingPlayers);
  }

  public boolean isWearingPear(Player player) {
    if (!pearWearers.contains(player.getUniqueId())) return false;
    ItemStack helmet = player.getInventory().getHelmet();

    boolean isWearingPear =
        helmet != null
            && helmet.getType() == Material.SKULL
            && CustomItemModule.isCustomItem(helmet, PEAR_ITEM);

    // TODO: will cause issues with stream?
    if (!isWearingPear) pearWearers.remove(player.getUniqueId());

    return isWearingPear;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWearSkull(InventoryClickEvent event) {
    if (notParticipating(event.getWhoClicked())) return;

    if (!event.getSlotType().equals(InventoryType.SlotType.ARMOR)) return;
    if (!event.getAction().equals(InventoryAction.PLACE_ALL)) return;
    if (event.getRawSlot() != 5) return;

    // Detect player putting on a custom skill
    if (event.getCursor() != null || event.getCursor().getType() != Material.SKULL) {
      if (CustomItemModule.isCustomItem(event.getCursor(), PEAR_ITEM)) {
        pearWearers.add(event.getWhoClicked().getUniqueId());
        checkPairs(event.getActor());
      }
    }
  }
}
