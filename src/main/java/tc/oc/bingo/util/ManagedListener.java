package tc.oc.bingo.util;

import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.bingo.Bingo;

public interface ManagedListener extends Listener {

  default void enable() {
    Bukkit.getServer().getPluginManager().registerEvents(this, Bingo.get());
    children().forEach(ManagedListener::enable);
  }

  default void disable() {
    HandlerList.unregisterAll(this);
    children().forEach(ManagedListener::disable);
  }

  default Stream<ManagedListener> children() {
    return Stream.empty();
  }
}
