package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;

@Tracker("masked-killer")
public class MaskedKillerObjective extends ObjectiveTracker {

  private final Supplier<Material> HELMET_ITEM = useConfig("mask-material", Material.PUMPKIN);
  private final Supplier<Integer> REQUIRED_KILLS = useConfig("required-kills", 5);

  private final Map<UUID, Integer> killsWithHelmet = useState(Scope.MATCH);

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDeath(MatchPlayerDeathEvent event) {
    if (!event.isChallengeKill() || event.getKiller() == null) return;

    MatchPlayer player = getPlayer(event.getKiller());
    if (player == null) return;

    if (!isWearingHelmet(player.getBukkit())) return;

    UUID killerId = player.getId();
    int kills = killsWithHelmet.getOrDefault(killerId, 0) + 1;
    killsWithHelmet.put(killerId, kills);

    if (kills >= REQUIRED_KILLS.get()) {
      reward(player.getBukkit());
    }
  }

  private boolean isWearingHelmet(Player player) {
    ItemStack helmet = player.getInventory().getHelmet();
    return helmet != null && helmet.getType() == HELMET_ITEM.get();
  }
}
