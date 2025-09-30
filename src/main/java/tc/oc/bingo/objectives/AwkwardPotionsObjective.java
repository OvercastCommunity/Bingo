package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.channels.GlobalChannel;
import tc.oc.pgm.util.MatchPlayers;

@Tracker("awkward-potions")
public class AwkwardPotionsObjective extends ObjectiveTracker {

  private final Supplier<String> REQUIRED_TEXT = useConfig("required-text", "uwu");

  private final Supplier<Integer> MAX_CONVERSIONS = useConfig("max-conversions", 3);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMessageSent(ChannelMessageEvent<?> event) {
    MatchPlayer player = event.getSender();
    if (!MatchPlayers.canInteract(player)) return;

    String message = event.getMessage();

    if (message == null || message.isEmpty()) return;
    if (!(event.getChannel() instanceof GlobalChannel)) return;

    if (!message.equalsIgnoreCase(REQUIRED_TEXT.get())) {
      return;
    }

    // Get the player's inventory and check if they have water bottles
    PlayerInventory inventory = player.getBukkit().getInventory();
    ItemStack[] contents = inventory.getContents();

    int changed = 0;
    for (int i = 0; i < contents.length; i++) {
      ItemStack stack = contents[i];
      if (stack == null) continue;

      // Find water bottles and replace with awkward potions
      if (stack.getType() != Material.POTION) continue;
      if (stack.getDurability() == 0) {
        ItemStack awkward = new ItemStack(Material.POTION, stack.getAmount(), (short) 16);
        inventory.setItem(i, awkward);
        changed++;
        if (changed >= MAX_CONVERSIONS.get()) break;
      }
    }

    // If any bottles were replaced, apply and reward the player
    if (changed > 0) {
      reward(player.getBukkit());
    }
  }
}
