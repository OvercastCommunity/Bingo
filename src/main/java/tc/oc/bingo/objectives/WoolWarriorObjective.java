package tc.oc.bingo.objectives;

import java.util.BitSet;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.bingo.util.InventoryUtil;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.WoolMatchModule;

@Tracker("wool-warrior")
public class WoolWarriorObjective extends ObjectiveTracker {

  private WoolMatchModule woolMatchModule;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    woolMatchModule = event.getMatch().getModule(WoolMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(final MatchPlayerDeathEvent event) {
    if (woolMatchModule == null || event.getKiller() == null) return;

    MatchPlayer matchPlayer = event.getKiller().getPlayer().orElse(null);
    if (matchPlayer == null) return;

    Collection<MonumentWool> monumentWools =
        woolMatchModule.getWools().get((Team) matchPlayer.getCompetitor());

    if (monumentWools.isEmpty()) return;

    BitSet woolColours =
        monumentWools.stream()
            .filter(monumentWool -> !monumentWool.isCompleted())
            .map(monumentWool -> monumentWool.getDyeColor().getWoolData())
            .collect(() -> new BitSet(16), BitSet::set, BitSet::or);

    if (woolColours.isEmpty()) return;

    PlayerInventory inventory = matchPlayer.getInventory();
    if (inventory == null) return;

    if (InventoryUtil.containsAny(
        inventory,
        it -> it.getType() == Material.WOOL && woolColours.get(it.getData().getData()))) {
      reward(matchPlayer.getBukkit());
    }
  }
}
