package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

@Tracker("stay-hydrated")
public class HydrationObjective extends ObjectiveTracker.StatefulInt {

  private final Supplier<Integer> MAX_SECONDS = useConfig("max-seconds", 1800);
  private final Supplier<Integer> MIN_SECONDS = useConfig("min-seconds", 300);

  private final Supplier<Integer> REQUIRED_DRINKS = useConfig("required-drinks", 6);
  private final Supplier<Boolean> INCLUDE_POTIONS = useConfig("accept-potions", false);

  private final Map<UUID, Long> lastDrink = useState(Scope.SESSION);

  // TODO: also change to a record?
  // when a player consumes a water bottle log the millisecond of when this was completed.
  // Check this against the stored last drink value,
  // if the number of seconds is between the min and max since the last time then increment
  // required-drinks by 1
  // If above the max reset drinks to 1
  // If below the min ignore the event

  // When the player reaches the required-drinks then reward them

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onDrink(PlayerItemConsumeEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;

    ItemStack item = event.getItem();
    if (item.getType() != Material.POTION) return;
    if (!INCLUDE_POTIONS.get() && item.getDurability() != 0) return;

    long now = System.currentTimeMillis();
    long drinkGap = now - lastDrink.getOrDefault(player.getUniqueId(), 0L);

    if (drinkGap < MIN_SECONDS.get() * 1000L) return;
    lastDrink.put(player.getUniqueId(), now);

    if (drinkGap > MAX_SECONDS.get() * 1000L) {
      storeObjectiveData(player.getUniqueId(), 1);
      return;
    }

    trackProgress(player);
  }

  @Override
  protected int maxValue() {
    return REQUIRED_DRINKS.get();
  }
}
