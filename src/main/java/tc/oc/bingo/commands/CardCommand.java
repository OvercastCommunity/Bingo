package tc.oc.bingo.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.menu.BingoCardMenu;

@CommandAlias("bingo")
public class CardCommand extends BaseCommand {

  @Default
  @CommandPermission("bingo.card")
  public void bingoCard(CommandSender sender) {
    if (sender instanceof Player) {
      Player player = (Player) sender;

      if (!Bingo.get().isBingoCardLoaded(player.getUniqueId())) {
        sender.sendMessage("Your Bingo Card has not yet loaded, please try again.");
        Bingo.get().loadBingoCard(player.getUniqueId());
      }

      Bingo.get()
          // TODO: dont do this
          .loadBingoCard(player.getUniqueId())
          .whenComplete(
              (bingoPlayerCard, throwable) -> {
                BingoCardMenu.INVENTORY.open(player);
              });
    }
  }

  @Subcommand("resync")
  @CommandPermission("bingo.reload")
  public void bingoResync(CommandSender sender) {
    sender.sendMessage("Fetching updated Bingo card data.");
    Bingo.get().loadBingoCard();
  }

  @Subcommand("reload")
  @CommandPermission("bingo.reload")
  public void bingoReload(CommandSender sender) {
    sender.sendMessage("Reloading Bingo config file.");
    Bingo.get().reloadConfig();
    Config.create(Bingo.get().getConfig());
  }
}
