package tc.oc.bingo.objectives;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;

@Tracker("all-armour")
public class AllArmourObjective
    extends ObjectiveTracker.Stateful<AllArmourObjective.ArmourProgress> {

  private final Supplier<Integer> COUNT_REQUIRED = useConfig("count-required", 4);

  private final Supplier<Boolean> PARTIAL_SETS = useConfig("partial-sets", false);
  private final Supplier<Boolean> COUNT_UNIQUE = useConfig("count-unique", false);

  private static final Set<EquipmentSlot> ARMOR_SLOTS =
      EnumSet.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

  private final ArmourSet[] VALID_SETS = {
    new ArmourSet(
        'l',
        Set.of(
            Material.LEATHER_HELMET,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS,
            Material.LEATHER_BOOTS)),
    new ArmourSet(
        'g',
        Set.of(
            Material.GOLD_HELMET,
            Material.GOLD_CHESTPLATE,
            Material.GOLD_LEGGINGS,
            Material.GOLD_BOOTS)),
    new ArmourSet(
        'c',
        Set.of(
            Material.CHAINMAIL_HELMET,
            Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_LEGGINGS,
            Material.CHAINMAIL_BOOTS)),
    new ArmourSet(
        'i',
        Set.of(
            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS)),
    new ArmourSet(
        'd',
        Set.of(
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS))
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
  public void onInventoryClick(InventoryClickEvent event) {
    checkArmourStatus(getPlayer(event.getActor()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() == Action.RIGHT_CLICK_AIR
        || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      checkArmourStatus(getPlayer(event.getActor()));
    }
  }

  private void checkArmourStatus(MatchPlayer player) {
    if (player == null || !player.isParticipating()) return;

    PlayerInventory inv = player.getInventory();
    if (inv == null) return;

    ArmourProgress armourProgress =
        updateObjectiveData(
            player.getId(),
            progress -> {
              for (ArmourSet set : VALID_SETS) {
                EnumSet<EquipmentSlot> worn = set.getWornPieces(inv);
                if (worn.isEmpty()) continue;

                // Skip if not all pieces are worn at same time and is required
                if (!PARTIAL_SETS.get() && !worn.containsAll(ARMOR_SLOTS)) continue;

                Set<EquipmentSlot> current =
                    progress.piecesBySet.computeIfAbsent(
                        set.code, k -> EnumSet.noneOf(EquipmentSlot.class));
                current.addAll(worn);
              }
              return progress;
            });

    // Check if reward threshold met
    long completed = getCompletionAmount(armourProgress);

    if (completed >= COUNT_REQUIRED.get()) {
      reward(player.getBukkit());
    }
  }

  private int getCompletionAmount(ArmourProgress armourProgress) {
    return armourProgress.piecesBySet.values().stream()
        .mapToInt(
            slots -> {
              // If counting unique pieces
              if (COUNT_UNIQUE.get()) return slots.size();
              // If full sets are required
              if (slots.containsAll(ARMOR_SLOTS)) return 1;
              return 0;
            })
        .sum();
  }

  @Override
  public @NotNull ArmourProgress initial() {
    return new ArmourProgress();
  }

  @Override
  public @NotNull ArmourProgress deserialize(@NotNull String string) {
    // Stored as "l:HEAD,CHEST,LEGS,FEET;g:CHEST,FEET;..." etc
    ArmourProgress progress = new ArmourProgress();
    if (string.isEmpty()) return progress;

    for (String entry : string.split(";")) {
      String[] parts = entry.split(":");
      if (parts.length != 2) continue;

      char code = parts[0].charAt(0);
      Set<EquipmentSlot> slots =
          Arrays.stream(parts[1].split(","))
              .map(EquipmentSlot::valueOf)
              .collect(Collectors.toCollection(() -> EnumSet.noneOf(EquipmentSlot.class)));

      progress.piecesBySet.put(code, slots);
    }
    return progress;
  }

  @Override
  public @NotNull String serialize(@NotNull ArmourProgress data) {
    return data.piecesBySet.entrySet().stream()
        .map(
            e ->
                e.getKey()
                    + ":"
                    + e.getValue().stream().map(Enum::name).collect(Collectors.joining(",")))
        .collect(Collectors.joining(";"));
  }

  @Override
  public double progress(ArmourProgress data) {
    long completed = getCompletionAmount(data);
    return (double) completed / COUNT_REQUIRED.get();
  }

  public static class ArmourProgress {
    public final Map<Character, Set<EquipmentSlot>> piecesBySet = new HashMap<>();
  }

  static class ArmourSet {
    char code;
    private final Set<Material> items;

    public ArmourSet(char code, Set<Material> items) {
      this.code = code;
      this.items = items;
    }

    public EnumSet<EquipmentSlot> getWornPieces(PlayerInventory inv) {
      EnumSet<EquipmentSlot> worn = EnumSet.noneOf(EquipmentSlot.class);

      ARMOR_SLOTS.forEach(
          slot -> {
            ItemStack item = inv.getItem(slot);
            if (item != null && items.contains(item.getType())) {
              worn.add(slot);
            }
          });

      return worn;
    }
  }
}
