package tc.oc.bingo.util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.bingo.Bingo;
import tc.oc.bingo.modules.BingoModule;
import tc.oc.bingo.modules.DependsOn;
import tc.oc.pgm.api.PGM;

public interface ManagedListener extends Listener {

  default void setupDependencies() {}

  default void teardownDependencies() {}

  default void enable() {
    Bukkit.getServer().getPluginManager().registerEvents(this, Bingo.get());
    children().forEach(ManagedListener::enable);
    var dependants = getClass().getAnnotation(DependsOn.class);
    if (dependants != null) {
      var bingo = Bingo.get();
      for (Class<? extends BingoModule> cls : dependants.value()) {
        bingo.getModules().get(cls).addDependant(this);
      }
    }
    setupDependencies();
  }

  default void disable() {
    HandlerList.unregisterAll(this);
    children().forEach(ManagedListener::disable);

    var dependants = getClass().getAnnotation(DependsOn.class);
    if (dependants != null) {
      var bingo = Bingo.get();
      for (Class<? extends BingoModule> cls : dependants.value()) {
        bingo.getModules().get(cls).removeDependant(this);
      }
    }
    teardownDependencies();
  }

  default Stream<ManagedListener> children() {
    return Stream.empty();
  }

  class Ticker implements ManagedListener {

    private final Runnable task;
    private final int delay;
    private final int period;
    private final TimeUnit unit;

    private Future<?> future;

    public Ticker(final Runnable task, int delay, int period, TimeUnit unit) {
      this.task = task;
      this.delay = delay;
      this.period = period;
      this.unit = unit;
    }

    @Override
    public void enable() {
      disable();
      future = PGM.get().getExecutor().scheduleAtFixedRate(task, delay, period, unit);
    }

    @Override
    public void disable() {
      if (future != null) {
        future.cancel(false);
        future = null;
      }
    }
  }
}
