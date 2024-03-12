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
import tc.oc.bingo.Bingo;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.database.ProgressItem;

public class BingoCardMenu implements InventoryProvider {

  public static final SmartInventory INVENTORY =
      SmartInventory.builder()
          .id("bingoCard")
          .provider(new BingoCardMenu())
          .size(6, 9)
          .title("Bingo Objectives")
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

  private ItemStack makeIconFor(ObjectiveItem objectiveItem, BingoPlayerCard playerCard) {

    ProgressItem progressItem = playerCard.getProgressList().get(objectiveItem.getSlug());

    short itemDamage = (short) (progressItem.isCompleted() ? 10 : 8);

    ItemStack itemStack = new ItemStack(Material.INK_SACK, 1, itemDamage);
    ItemMeta itemMeta = itemStack.getItemMeta();
    String objectiveItemName =
        objectiveItem.getHintLevel() == -1 || objectiveItem.getHintLevel() >= 1
            ? objectiveItem.getName()
            : "?????";
    itemMeta.setDisplayName(
        "" + ChatColor.RESET + ChatColor.BOLD + ChatColor.AQUA + objectiveItemName);

    String[] displayedHints;
    String[] splitHints = objectiveItem.getDescription().split(";");
    boolean shouldDisplayHints = objectiveItem.getHintLevel() > 0;
    if (shouldDisplayHints) {
      displayedHints = Arrays.copyOfRange(splitHints, 0, objectiveItem.getHintLevel());
    } else {
      displayedHints = splitHints;
    }

    List<String> loreList = new ArrayList<>();
    for (String displayedHint : displayedHints) {
      loreList.add(ChatColor.GRAY + displayedHint);
      loreList.add("");
    }

    // Add a "Clue revealed in 5 hours." line if the hint level is less than the total number of
    // splitHints
    // The 5 hours should come from the objectiveItem.getNextClueUnlock() which is a LocalDateTime
    LocalDateTime nextClueUnlock = objectiveItem.getNextClueUnlock();
    if (shouldDisplayHints
        && nextClueUnlock != null
        && objectiveItem.getHintLevel() < splitHints.length) {
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

    // Add lore lines depending on the conditions
    if (objectiveItem.getDiscoveryUUID() != null) {
      loreList.add(
          ChatColor.GRAY
              + "Discovered by: "
              + ChatColor.GOLD
              + objectiveItem.getDiscoveryUUID()); // Assuming playerName is accessible
    }

    if (progressItem.getPlacedPosition() != null) {
      loreList.add(
          ChatColor.GRAY
              + "Your position: "
              + ChatColor.GOLD
              + "#"
              + progressItem.getPlacedPosition());
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
