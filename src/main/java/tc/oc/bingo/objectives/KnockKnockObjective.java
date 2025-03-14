package tc.oc.bingo.objectives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;

@Tracker("knock-knock")
public class KnockKnockObjective extends ObjectiveTracker.Stateful<Integer> {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(ParticipantBlockTransformEvent event) { // maybe this event
    MatchPlayer player = getPlayer(event.getActor());
    if (player == null) return;

    // Check if player opened or closed a door or trap door caused by player increment

    if (event.getBlock().getType().name().contains("TRAPDOOR")) {
      System.out.println("Trapdoor opened?");
    }
  }

  @Override
  public @NotNull Integer initial() {
    return 0;
  }

  @Override
  public @NotNull Integer deserialize(@NotNull String string) {
    return 0;
  }

  @Override
  public @NotNull String serialize(@NotNull Integer data) {
    return "";
  }

  @Override
  public double progress(Integer data) {
    return 0;
  }
}
