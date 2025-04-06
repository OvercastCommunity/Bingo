package tc.oc.bingo.objectives;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;

@Tracker("cloud-yell")
public class CloudYellObjective extends ObjectiveTracker {

  // Maximum number of seconds between each of the steps
  private final Supplier<Integer> MAX_ACTION_SECONDS = useConfig("max-action-seconds", 5);

  private final Map<UUID, PlayerState> states = useState(Scope.LIFE);

  @EventHandler(priority = EventPriority.MONITOR)
  public final void onPunch(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;

    // Detect player punching the air (upwards) (three times).
    if (event.getAction() != Action.LEFT_CLICK_AIR) return;
    if (player.getLocation().getPitch() > -80F) return;

    PlayerState state = states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());

    long now = System.currentTimeMillis();
    long timeSincePunch = now - state.lastPunch;
    long timeSinceYell = now - state.lastYell;

    // Reset punch & yell counts back to 0 if the time is too long
    if (timeSincePunch > MAX_ACTION_SECONDS.get() * 1000L) state.punchCount = 0;
    if (timeSinceYell > MAX_ACTION_SECONDS.get() * 1000L) state.lastYell = 0;

    // Detect player hitting air again after doing above then reward them
    state.punchCount++;
    state.lastPunch = now;
    if (state.punchCount >= 3 && state.lastYell != 0) {
      reward(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMessageSent(ChannelMessageEvent<?> event) {
    MatchPlayer player = event.getSender();
    if (player == null) return;

    String message = event.getMessage();

    PlayerState state = states.computeIfAbsent(player.getId(), k -> new PlayerState());

    // Allowed characters = A-Z (upper case) and !
    if (state.punchCount >= 3 && message.matches("[A-Z !]{5,}!")) {
      state.lastYell = state.lastPunch = System.currentTimeMillis();
      state.punchCount = 0;
    }
  }

  static class PlayerState {
    int punchCount = 0;
    long lastPunch = 0L;
    long lastYell = 0L;
  }
}
