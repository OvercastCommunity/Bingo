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

    PlayerState state = states.getOrDefault(player.getUniqueId(), new PlayerState());

    long now = System.currentTimeMillis();
    long timeSincePunch = now - state.lastPunch;

    // Reset punch count back to 0 if the time between punches is too long
    if (timeSincePunch > MAX_ACTION_SECONDS.get() * 1000L) {
      state.punchCount = 0;
    }

    // Detect player hitting air again after doing above then reward them
    state.punchCount++;
    if (state.punchCount >= 3 && now - state.lastYell < MAX_ACTION_SECONDS.get() * 1000L) {
      reward(player);
    }

    state.lastPunch = now;
    states.put(player.getUniqueId(), state);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMessageSent(ChannelMessageEvent<?> event) {
    MatchPlayer player = event.getSender();
    if (player == null) return;

    String message = event.getMessage();

    PlayerState state = states.getOrDefault(player.getId(), new PlayerState());
    if (state.punchCount < 3) return;

    // Allowed characters = A-Z (upper case) and !
    if (message.matches("[^A-Z !]")) return;

    long now = System.currentTimeMillis();
    state.lastYell = now;
    state.lastPunch = now;
    state.punchCount = 0;
    states.put(player.getId(), state);
  }

  static class PlayerState {
    int punchCount = 0;
    long lastPunch = 0L;
    long lastYell = 0L;
  }
}
