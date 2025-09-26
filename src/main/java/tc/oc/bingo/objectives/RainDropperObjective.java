package tc.oc.bingo.objectives;

import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.bingo.modules.ItemRemoveCanceller;
import tc.oc.bingo.util.Raindrops;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("rain-dropper")
public class RainDropperObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> REQUIRED_DROPS = useConfig("required-drops", 50);
  private final Supplier<Double> DROP_CHANCE = useConfig("drop-chance", 0.1d);

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerEarnCurrency(Raindrops.PlayerRaindropEvent event) {
    // Skip these as all players will be out of the match
    if (event.isPostMatch()) return;

    MatchPlayer matchPlayer = getPlayer(event.getPlayer());
    if (matchPlayer == null || !matchPlayer.isParticipating()) return;

    if (Math.random() <= DROP_CHANCE.get()) {

      // Drop a real fake raindrop
      Location location = event.getPlayer().getLocation().add(0, 0.1, 0);

      ItemStack raindrop = new ItemStack(Material.GHAST_TEAR);
      ItemMeta itemMeta = raindrop.getItemMeta();
      itemMeta.setDisplayName("§b§lRaindrop");
      raindrop.setItemMeta(itemMeta);
      ItemRemoveCanceller.applyCustomMeta(raindrop);

      Item item = location.getWorld().dropItemNaturally(location, raindrop);
      item.setPickupDelay(60); // 1 second pickup delay

      trackProgress(event.getPlayer());
    }
  }

  @Override
  protected int maxValue() {
    return REQUIRED_DROPS.get();
  }
}
