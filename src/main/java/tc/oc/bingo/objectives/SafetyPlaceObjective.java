package tc.oc.bingo.objectives;

import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.WoolMatchModule;

@Tracker("safety-place")
public class SafetyPlaceObjective extends ObjectiveTracker {

  private WoolMatchModule woolMatchModule;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    woolMatchModule = event.getMatch().getModule(WoolMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(ParticipantBlockTransformEvent event) {
    if (woolMatchModule == null) return;

    if (!(event.getNewState().getMaterial().equals(Material.WOOL))) return;

    MatchPlayer matchPlayer = event.getPlayer();
    if (matchPlayer == null) return;

    Collection<MonumentWool> monumentWools =
        woolMatchModule.getWools().get((Team) matchPlayer.getCompetitor());

    if (monumentWools.isEmpty()) return;

    for (MonumentWool monumentWool : monumentWools) {
      if (!monumentWool.isCompleted()
          && monumentWool.getDefinition().isObjectiveWool(event.getNewState())) {
        reward(matchPlayer.getBukkit());
        break;
      }
    }
  }
}
