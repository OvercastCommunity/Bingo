package tc.oc.bingo.objectives;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;

@Tracker("all-armour")
public class AllArmourObjective extends ObjectiveTracker.Stateful<Set<Character>> {

  private final Supplier<Integer> SETS_REQUIRED = useConfig("sets-required", 4);

  private final ArmourSet[] VALID_SETS = {
    new ArmourSet(
        'l',
        Material.LEATHER_HELMET,
        Material.LEATHER_CHESTPLATE,
        Material.LEATHER_LEGGINGS,
        Material.LEATHER_BOOTS),
    new ArmourSet(
        'g',
        Material.GOLD_HELMET,
        Material.GOLD_CHESTPLATE,
        Material.GOLD_LEGGINGS,
        Material.GOLD_BOOTS),
    new ArmourSet(
        'c',
        Material.CHAINMAIL_HELMET,
        Material.CHAINMAIL_CHESTPLATE,
        Material.CHAINMAIL_LEGGINGS,
        Material.CHAINMAIL_BOOTS),
    new ArmourSet(
        'i',
        Material.IRON_HELMET,
        Material.IRON_CHESTPLATE,
        Material.IRON_LEGGINGS,
        Material.IRON_BOOTS),
    new ArmourSet(
        'd',
        Material.DIAMOND_HELMET,
        Material.DIAMOND_CHESTPLATE,
        Material.DIAMOND_LEGGINGS,
        Material.DIAMOND_BOOTS)
  };

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onKitApply(ParticipantKitApplyEvent event) {
    checkArmourStatus(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerItemTransfer(PlayerItemTransferEvent event) {
    checkArmourStatus(getPlayer(event.getActor()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerItemTransfer(InventoryClickEvent event) {
    checkArmourStatus(getPlayer(event.getActor()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerItemTransfer(PlayerInteractEvent event) {
    if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
        || event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
      checkArmourStatus(getPlayer(event.getActor()));
    }
  }

  private void checkArmourStatus(MatchPlayer player) {
    if (player == null || !player.isParticipating()) return;
    PlayerInventory inventory = player.getInventory();
    if (inventory == null) return;

    if (inventory.getHelmet() == null
        || inventory.getChestplate() == null
        || inventory.getLeggings() == null
        || inventory.getBoots() == null) {
      return;
    }

    Set<Character> sets = getObjectiveData(player.getId());

    for (ArmourSet set : VALID_SETS) {
      if (sets.contains(set.code)) continue;
      if (set.isFullSet(inventory)) {

        int size =
            updateObjectiveData(
                    player.getId(),
                    s -> {
                      s.add(set.code);
                      return s;
                    })
                .size();

        if (size >= SETS_REQUIRED.get()) {
          reward(player.getBukkit());
        }
        break;
      }
    }
  }

  @Override
  public @NotNull Set<Character> initial() {
    return new HashSet<>();
  }

  @Override
  public @NotNull Set<Character> deserialize(@NotNull String string) {
    if (string.isEmpty()) return initial();
    return Arrays.stream(string.split(",")).map(s -> s.charAt(0)).collect(Collectors.toSet());
  }

  @Override
  public @NotNull String serialize(@NotNull Set<Character> data) {
    return String.join(",", Iterables.transform(data, Object::toString));
  }

  @Override
  public double progress(Set<Character> data) {
    return (double) data.size() / SETS_REQUIRED.get();
  }

  static class ArmourSet {
    char code;
    private final Material helmet;
    private final Material chestplate;
    private final Material leggings;
    private final Material boots;

    public ArmourSet(
        char code, Material helmet, Material chestplate, Material leggings, Material boots) {
      this.code = code;
      this.helmet = helmet;
      this.chestplate = chestplate;
      this.leggings = leggings;
      this.boots = boots;
    }

    public boolean isFullSet(PlayerInventory inv) {
      return inv.getHelmet().getType() == helmet
          && inv.getChestplate().getType() == chestplate
          && inv.getLeggings().getType() == leggings
          && inv.getBoots().getType() == boots;
    }
  }
}
