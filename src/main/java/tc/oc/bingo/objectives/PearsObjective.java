package tc.oc.bingo.objectives;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
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
import tc.oc.pgm.api.PGM;

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
    pearWearers.forEach(this::checkPairs);
  }

  private void checkPairs(UUID uuid) {
    Player player = Bukkit.getPlayer(uuid);
    if (player == null || !player.isOnline()) {
      pearWearers.remove(uuid);
      return;
    }

    checkPairs(player);
  }

  private void checkPairs(Player player) {
    if (!passesVibeCheck(player)) return;

    Set<Player> rewardingPlayers =
        player.getNearbyEntities(PAIR_RANGE.get(), PAIR_RANGE.get(), PAIR_RANGE.get()).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .filter(neighbor -> !neighbor.getUniqueId().equals(player.getUniqueId()))
            .filter(this::passesVibeCheck)
            .collect(Collectors.toSet());

    if (rewardingPlayers.isEmpty()) return;

    reward(rewardingPlayers);
  }

  public boolean passesVibeCheck(Player player) {
    if (!pearWearers.contains(player.getUniqueId())) return false;

    boolean isWearingPear = isPearItem(player.getInventory().getHelmet());
    if (!isWearingPear) pearWearers.remove(player.getUniqueId());

    return isWearingPear;
  }

  public boolean isPearItem(ItemStack itemStack) {
    return itemStack != null
        && (itemStack.getType() == Material.SKULL || itemStack.getType() == Material.SKULL_ITEM)
        && CustomItemModule.isCustomItem(itemStack, PEAR_ITEM);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWearSkull(InventoryClickEvent event) {
    if (notParticipating(event.getWhoClicked())) return;
    if (event.getRawSlot() != 5) return;

    if (!event.getSlotType().equals(InventoryType.SlotType.ARMOR)) return;
    InventoryAction action = event.getAction();
    if (!(action.equals(InventoryAction.PLACE_ALL)
        || action.equals(InventoryAction.SWAP_WITH_CURSOR))) return;

    // Detect player putting on a custom skull
    if (isPearItem(event.getCursor())) {
      pearWearers.add(event.getWhoClicked().getUniqueId());
      PGM.get().getExecutor().schedule(() -> checkPairs(event.getActor()), 1, TimeUnit.SECONDS);
    }
  }
}
