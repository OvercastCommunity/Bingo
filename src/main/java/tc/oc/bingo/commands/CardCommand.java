package tc.oc.bingo.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.menu.BingoCardMenu;

public class CardCommand extends BaseCommand {

  @CommandAlias("bingoinfo")
  @CommandPermission("bingo.card")
  public void bingoCard(CommandSender sender) {
    if (sender instanceof Player) {
      Player player = (Player) sender;

      if (!Bingo.get().isBingoCardLoaded(player.getUniqueId())) {
        sender.sendMessage("Your Bingo Card has not yet loaded, please try again.");
        Bingo.get().loadBingoCard(player.getUniqueId());
      }

      Bingo.get()
          .loadBingoCard(player.getUniqueId())
          .whenComplete(
              (bingoPlayerCard, throwable) -> {
                BingoCardMenu.INVENTORY.open(player);
              });
    }
  }

  @CommandAlias("bingoshow")
  @CommandPermission("bingo.card")
  public void bingoShow(CommandSender sender) {
    Bingo.get().loadBingoCard();
  }
}
