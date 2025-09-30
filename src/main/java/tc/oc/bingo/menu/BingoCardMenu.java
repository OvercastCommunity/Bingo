package tc.oc.bingo.menu;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.Style.*;
import static org.bukkit.ChatColor.*;
import static tc.oc.pgm.util.named.NameStyle.PLAIN;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TextTranslations.translateLegacy;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.bingo.util.IconUtils;
import tc.oc.bingo.util.Messages;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class BingoCardMenu implements InventoryProvider {

  private final BingoPlayerCard playerCard;
  private final Integer objectiveIndex;

  public BingoCardMenu(BingoPlayerCard bingoPlayerCard, @Nullable Integer objectiveIndex) {
    this.playerCard = bingoPlayerCard;
    this.objectiveIndex = objectiveIndex;
  }

  public static SmartInventory getInventory(
      BingoPlayerCard bingoPlayerCard, @Nullable Integer objectiveIndex) {
    return SmartInventory.builder()
        .provider(new BingoCardMenu(bingoPlayerCard, objectiveIndex))
        .manager(Bingo.get().getInventoryManager())
        .size(5, 9)
        .title("Bingo Card")
        .build();
  }

  public static SmartInventory get(BingoPlayerCard bingoCard) {
    return BingoCardMenu.getInventory(bingoCard, null);
  }

  public static SmartInventory get(BingoPlayerCard bingoCard, @Nullable Integer objectiveIndex) {
    return BingoCardMenu.getInventory(bingoCard, objectiveIndex);
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    int xOffset = 2;

    BingoCard bingoCard = Bingo.get().getBingoCard();
    if (bingoCard == null || playerCard == null) return;

    Integer requestedObjectiveIndex = this.objectiveIndex;

    contents.set(0, 0, ClickableItem.empty(getInfoItem()));

    contents.set(
        4,
        0,
        ClickableItem.of(
            new ItemBuilder()
                .material(Material.ARROW)
                .name(
                    player,
                    text("«", NamedTextColor.GOLD)
                        .append(text(" Battlepass", NamedTextColor.YELLOW)))
                .build(),
            event -> player.performCommand("battlepass")));

    bingoCard
        .getObjectives()
        .forEach(
            objectiveItem -> {
              if (objectiveItem == null) return;
              contents.set(
                  objectiveItem.getY(),
                  objectiveItem.getX() + xOffset,
                  ClickableItem.of(
                      makeIconFor(player, objectiveItem, playerCard, requestedObjectiveIndex),
                      inventoryClickEvent ->
                          Bukkit.getPluginManager()
                              .callEvent(
                                  new ObjectiveClickEvent(
                                      player, objectiveItem, inventoryClickEvent))));
            });
  }

  private ItemStack getInfoItem() {
    ItemStack itemStack = new ItemStack(Material.REDSTONE_TORCH_ON, 1);
    ItemMeta itemMeta = itemStack.getItemMeta();

    itemMeta.setDisplayName(YELLOW + "What is " + GOLD + BOLD + "Bingo" + RESET + YELLOW + "?");

    itemMeta.setLore(
        Arrays.asList(
            GRAY + "Complete mystery objectives on",
            GRAY + "the Bingo Card to earn " + AQUA + "raindrops" + GRAY + ".",
            GRAY + "",
            GRAY + "Get bonuses for lines and a full",
            GRAY + "house. " + DARK_PURPLE + "Clues" + GRAY + " will be revealed over",
            GRAY + "time to help you out.",
            GRAY + "",
            GRAY + "Join the Discord to share your",
            GOLD + "#bingo" + GRAY + " discoveries: " + BLUE + "oc.tc/discord"));

    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  private ItemStack makeIconFor(
      Player viewer,
      ObjectiveItem objective,
      BingoPlayerCard playerCard,
      @Nullable Integer requestedIdx) {
    @Nullable ProgressItem progressItem = playerCard.getProgressMap().get(objective.getSlug());

    List<String> loreList = new ArrayList<>();

    if (!objective.hasUnlocked()) {
      addUnlock(objective.getLockedUntil(), loreList);
      return createIconItem(objective.getIndex(), false, false, true, "Objective Locked", loreList);
    }

    boolean completed = progressItem != null && progressItem.isCompleted();
    if (completed) {
      loreList.add(GREEN + "Objective Complete" + DARK_GREEN + " ✔");
    }

    HintDisplay hints = new HintDisplay(objective);

    // Add all visible hints
    addSpaced(loreList, hints.revealed);

    // Add "Clue revealed in X"
    if (hints.hasHidden) addNextClueUnlock(objective.getNextClueUnlock(), loreList);

    // Add "You placed #x", or "Progress: X%" (for stateful objectives)
    addProgress(playerCard.getPlayerUUID(), objective, progressItem, loreList);

    // Add "Discovered by: X"
    if (objective.getDiscoveryTime() != null) {
      UUID uuid = objective.getDiscoveryUUID();
      String by;
      if (uuid == null) by = "" + GOLD + ITALIC + "Several Players";
      else by = translateLegacy(player(uuid, PLAIN).style(style(NamedTextColor.GOLD)), viewer);
      addSpaced(loreList, GRAY + "Discovered by: " + by);
    }

    boolean showCoordinate = hints.unlocked <= 0 || Config.get().isShowObjectiveCoords();

    String name =
        (hints.unlocked > 0 ? objective.getName() : MAGIC + "NiceTry")
            + (showCoordinate ? AQUA + " (" + objective.getGridPosition() + ")" : "");

    return createIconItem(
        objective.getIndex(),
        completed,
        Objects.equals(requestedIdx, objective.getIndex()),
        false,
        name,
        loreList);
  }

  private static @NotNull ItemStack createIconItem(
      int idx,
      boolean completed,
      boolean highlight,
      boolean locked,
      String name,
      List<String> lore) {
    @SuppressWarnings("deprecation")
    ItemStack itemStack = IconUtils.getItemStack(idx, completed, locked, highlight);
    ItemMeta itemMeta = itemStack.getItemMeta();

    if (highlight) {
      itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
      itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    itemMeta.setDisplayName("" + AQUA + BOLD + name);
    itemMeta.setLore(lore);

    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  private static void addProgress(
      UUID uuid, ObjectiveItem objective, ProgressItem progress, List<String> loreList) {
    if (progress != null && progress.isCompleted()) {
      Integer placed = progress.getPlacedPosition();
      if (placed != null) addSpaced(loreList, GRAY + "Your position: " + GOLD + "#" + placed);
      return;
    }

    Double cmp = objective.getCompletion(uuid);
    if (cmp == null) return;

    String pct =
        cmp <= 0 ? "0%" : cmp >= 1 ? "~99%" : "~" + (int) ((Math.floor(cmp * 10) * 10) + 5) + "%";
    addSpaced(loreList, GRAY + "Progress: " + GOLD + pct);
  }

  private static void addUnlockMessage(
      LocalDateTime unlockAt, List<String> loreList, String messagePrefix) {
    if (unlockAt == null) return;
    String nextIn = Messages.getDurationRemaining(Duration.between(LocalDateTime.now(), unlockAt));
    if (nextIn != null)
      addSpaced(loreList, "" + ITALIC + DARK_PURPLE + messagePrefix + " " + nextIn + ".");
  }

  private static void addNextClueUnlock(LocalDateTime unlockAt, List<String> loreList) {
    addUnlockMessage(unlockAt, loreList, "Clue revealed in");
  }

  private static void addUnlock(LocalDateTime unlockAt, List<String> loreList) {
    addUnlockMessage(unlockAt, loreList, "Unlocked in");
  }

  private static void addSpaced(List<String> lore, String... txt) {
    if (lore.isEmpty() || !lore.get(lore.size() - 1).isEmpty()) lore.add("");
    lore.addAll(Arrays.asList(txt));
  }

  @Override
  public void update(Player player, InventoryContents inventoryContents) {}

  private static class HintDisplay {
    private final int unlocked;
    private final String[] revealed;
    private final boolean hasHidden;

    public HintDisplay(ObjectiveItem objective) {
      unlocked = objective.getHintLevel() + (objective.hasNextCluePassed() ? 1 : 0);

      String[] splitHints = objective.getDescription().split(";");
      if (unlocked > 1) {
        revealed = Arrays.copyOfRange(splitHints, 0, Math.min(unlocked - 1, splitHints.length));
        for (int i = 0; i < revealed.length; i++) {
          revealed[i] = GRAY + revealed[i];
        }
      } else {
        revealed = new String[] {GRAY + "" + ITALIC + "No hints revealed yet."};
      }
      this.hasHidden = unlocked <= splitHints.length;
    }
  }
}
