package tc.oc.bingo.menu;

import static net.kyori.adventure.text.Component.text;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.database.BingoCard;
import tc.oc.bingo.database.BingoPlayerCard;
import tc.oc.bingo.database.ObjectiveItem;
import tc.oc.bingo.database.ProgressItem;
import tc.oc.bingo.util.Messages;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TextTranslations;

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
                  ClickableItem.empty(
                      makeIconFor(player, objectiveItem, playerCard, requestedObjectiveIndex)));
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
            ChatColor.GRAY + "Get bonuses for lines and a full",
            ChatColor.GRAY
                + "house. "
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

  private ItemStack makeIconFor(
      Player viewer,
      ObjectiveItem objectiveItem,
      BingoPlayerCard playerCard,
      @Nullable Integer requestedObjectiveIndex) {

    ProgressItem progressItem = playerCard.getProgressMap().get(objectiveItem.getSlug());

    boolean completed = progressItem != null && progressItem.isCompleted();
    short itemDamage = (short) (completed ? 10 : 8);
    boolean hightlight =
        requestedObjectiveIndex != null && (requestedObjectiveIndex == objectiveItem.getIndex());

    ItemStack itemStack = new ItemStack(Material.INK_SACK, 1, itemDamage);
    ItemMeta itemMeta = itemStack.getItemMeta();

    if (hightlight) {
      itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
      itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    int hintLevel = objectiveItem.getHintLevel();

    // When the next clue timestamp passes bump the hint level by 1
    if (objectiveItem.hasNextCluePassed()) {
      hintLevel++;
    }

    String objectiveItemName =
        hintLevel > 0 ? objectiveItem.getName() : ChatColor.MAGIC + "NiceTry";

    itemMeta.setDisplayName("" + ChatColor.AQUA + ChatColor.BOLD + objectiveItemName);

    String[] displayedHints;
    String[] splitHints = objectiveItem.getDescription().split(";");
    if (hintLevel > 1) {
      displayedHints =
          Arrays.copyOfRange(splitHints, 0, Math.min(hintLevel - 1, splitHints.length));
    } else {
      displayedHints =
          new String[] {ChatColor.GRAY + "" + ChatColor.ITALIC + "No hints revealed yet."};
      // TODO: remove extra line that gets added this scenario
    }

    List<String> loreList = new ArrayList<>();

    if (completed) {
      loreList.add(ChatColor.GREEN + "Objective Complete" + ChatColor.DARK_GREEN + " ✔");
      loreList.add("");
    }

    int i = 0;
    for (String displayedHint : displayedHints) {
      if (i != 0) loreList.add("");
      loreList.add(ChatColor.GRAY + displayedHint);
      i++;
    }

    // Add a "Clue revealed in 5 hours." line if the hint level is less than the total number of
    // splitHints
    // The 5 hours should come from the objectiveItem.getNextClueUnlock() which is a LocalDateTime
    LocalDateTime nextClueUnlock = objectiveItem.getNextClueUnlock();
    if (nextClueUnlock != null && hintLevel <= splitHints.length) {
      LocalDateTime now = LocalDateTime.now();

      Duration remaining = Duration.between(now, nextClueUnlock);
      String durationRemaining = Messages.getDurationRemaining(remaining);

      if (durationRemaining != null) {
        loreList.add("");
        loreList.add(
            ChatColor.ITALIC
                + ""
                + ChatColor.DARK_PURPLE
                + "Clue revealed in "
                + durationRemaining
                + ".");
      }
    }

    boolean selfDiscovered =
        progressItem != null
            && progressItem.isCompleted()
            && progressItem.getPlacedPosition() != null;
    if (selfDiscovered) {
      loreList.add("");
      loreList.add(
          ChatColor.GRAY
              + "Your position: "
              + ChatColor.GOLD
              + "#"
              + progressItem.getPlacedPosition());
    }

    // Add lore lines depending on the conditions
    if (objectiveItem.getDiscoveryTime() != null) {

      if (objectiveItem.getDiscoveryUUID() != null) {
        @Nullable
        String username =
            PGM.get().getDatastore().getUsername(objectiveItem.getDiscoveryUUID()).getNameLegacy();

        Component discoveryPlayer =
            PlayerComponent.player(objectiveItem.getDiscoveryUUID(), NameStyle.PLAIN)
                .style(Style.style(NamedTextColor.GOLD));
        String discoveryName = TextTranslations.translateLegacy(discoveryPlayer, viewer);

        if (username != null) {
          if (!selfDiscovered) loreList.add("");
          // TODO: use online players getName
          loreList.add(ChatColor.GRAY + "Discovered by: " + discoveryName);
        }
      } else {
        loreList.add(
            ChatColor.GRAY
                + "Discovered by: "
                + ChatColor.GOLD
                + ChatColor.ITALIC
                + "Several Players");
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
