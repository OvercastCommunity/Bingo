package tc.oc.bingo.menu;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.pgm.api.PGM;

public class BingoCardMenu implements InventoryProvider {

  public static final SmartInventory INVENTORY =
      SmartInventory.builder()
          .id("bingoCard")
          .provider(new BingoCardMenu())
          .size(5, 9)
          .title("Bingo Card")
          .manager(Bingo.get().getInventoryManager())
          .build();

  public Bingo bingo;

  public BingoCardMenu() {
    bingo = Bingo.get();
  }

  @Override
  public void init(Player player, InventoryContents contents) {

    int xOffset = 2;

    BingoCard bingoCard = Bingo.get().getBingoCard();
    BingoPlayerCard playerCard = bingo.getCards().getOrDefault(player.getUniqueId(), null);
    if (playerCard == null) return;

    contents.set(0, 0, ClickableItem.empty(getInfoItem()));

    bingoCard
        .getObjectives()
        .forEach(
            objectiveItem -> {
              contents.set(
                  objectiveItem.getY(),
                  objectiveItem.getX() + xOffset,
                  ClickableItem.of(makeIconFor(objectiveItem, playerCard), event -> {}));
            });
  }

  private ItemStack getInfoItem() {
    ItemStack itemStack = new ItemStack(Material.REDSTONE_TORCH_ON, 1);
    ItemMeta itemMeta = itemStack.getItemMeta();

    itemMeta.setDisplayName(
        ChatColor.YELLOW
            + "What is "
            + ChatColor.GOLD
            + ChatColor.BOLD
            + "Bingo"
            + ChatColor.RESET
            + ChatColor.YELLOW
            + "?");

    itemMeta.setLore(
        Arrays.asList(
            ChatColor.GRAY + "Complete mystery objectives on",
            ChatColor.GRAY
                + "the Bingo Card to earn "
                + ChatColor.AQUA
                + "raindrops"
                + ChatColor.GRAY
                + ".",
            ChatColor.GRAY + "",
            ChatColor.GRAY + "Get bonuses for a full-house and",
            ChatColor.GRAY
                + "lines. "
                + ChatColor.DARK_PURPLE
                + "Clues"
                + ChatColor.GRAY
                + " will be revealed over",
            ChatColor.GRAY + "time to help you out.",
            ChatColor.GRAY + "",
            ChatColor.GRAY + "Join the Discord to share your",
            ChatColor.GOLD
                + "#bingo"
                + ChatColor.GRAY
                + " discoveries: "
                + ChatColor.BLUE
                + "oc.tc/discord"));

    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  private ItemStack makeIconFor(ObjectiveItem objectiveItem, BingoPlayerCard playerCard) {

    ProgressItem progressItem = playerCard.getProgressList().get(objectiveItem.getSlug());

    // TODO: use ChatColor.RESET + "" + ChatColor.GREEN + "✓" somewhere to show completed

    boolean completed = progressItem != null && progressItem.isCompleted();
    short itemDamage = (short) (completed ? 10 : 8);

    ItemStack itemStack = new ItemStack(Material.INK_SACK, 1, itemDamage);
    ItemMeta itemMeta = itemStack.getItemMeta();
    String objectiveItemName =
        objectiveItem.getHintLevel() > 0 ? objectiveItem.getName() : ChatColor.MAGIC + "NiceTry";

    itemMeta.setDisplayName("" + ChatColor.AQUA + ChatColor.BOLD + objectiveItemName);

    String[] displayedHints;
    String[] splitHints = objectiveItem.getDescription().split(";");
    if (objectiveItem.getHintLevel() > 1) {
      displayedHints = Arrays.copyOfRange(splitHints, 0, objectiveItem.getHintLevel() - 1);
    } else {
      displayedHints =
          new String[] {ChatColor.GRAY + "" + ChatColor.ITALIC + "No hints revealed yet."};
    }

    List<String> loreList = new ArrayList<>();

    if (completed) {
      loreList.add(ChatColor.GREEN + "Objective Complete" + ChatColor.DARK_GREEN + " ✔");
    }

    for (String displayedHint : displayedHints) {
      loreList.add(ChatColor.GRAY + displayedHint);
      loreList.add("");
    }

    // Add a "Clue revealed in 5 hours." line if the hint level is less than the total number of
    // splitHints
    // The 5 hours should come from the objectiveItem.getNextClueUnlock() which is a LocalDateTime
    LocalDateTime nextClueUnlock = objectiveItem.getNextClueUnlock();
    if (nextClueUnlock != null && objectiveItem.getHintLevel() <= splitHints.length) {
      LocalDateTime now = LocalDateTime.now();
      long hoursUntilUnlock = Duration.between(now, nextClueUnlock).toHours();
      loreList.add(
          ChatColor.ITALIC
              + ""
              + ChatColor.DARK_PURPLE
              + "Clue revealed in "
              + hoursUntilUnlock
              + " hours.");
      loreList.add("");
    }

    if (progressItem != null && progressItem.getPlacedPosition() != null) {
      loreList.add(
          ChatColor.GRAY
              + "Your position: "
              + ChatColor.GOLD
              + "#"
              + progressItem.getPlacedPosition());
    }

    // Add lore lines depending on the conditions

    if (objectiveItem.getDiscoveryUUID() != null) {
      @Nullable
      String username =
          PGM.get().getDatastore().getUsername(objectiveItem.getDiscoveryUUID()).getNameLegacy();
      if (username != null) {
        loreList.add(ChatColor.GRAY + "Discovered by: " + ChatColor.GOLD + username);
      }
    }

    itemMeta.setLore(loreList);

    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  @Override
  public void update(Player player, InventoryContents inventoryContents) {
    return;
  }
}
