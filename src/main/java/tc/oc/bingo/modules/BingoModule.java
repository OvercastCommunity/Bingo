package tc.oc.bingo.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Getter;
import tc.oc.bingo.config.ConfigHandler;
import tc.oc.bingo.objectives.Scope;
import tc.oc.bingo.util.ManagedListener;
import tc.oc.bingo.util.StateHandler;

public class BingoModule implements ManagedListener, ConfigHandler.Extensions {
  @Getter protected final ConfigHandler config = new ConfigHandler();

  private final Set<Object> dependants = new HashSet<>();
  private final StateHandler state = new StateHandler();
  private boolean isEnabled = false;

  public BingoModule() {
    // Depend on itself so it will never unload
    if (getClass().getAnnotation(AlwaysOn.class) != null) addDependant(this);
  }

  public void addDependant(Object dependant) {
    dependants.add(dependant);
    if (!isEnabled) {
      enable();
      isEnabled = true;
    }
  }

  public void removeDependant(Object dependant) {
    dependants.remove(dependant);
    if (dependants.isEmpty() && isEnabled) {
      disable();
      isEnabled = false;
    }
  }

  protected final <T> Map<UUID, T> useState(Scope scope) {
    Map<UUID, T> result = new HashMap<>();
    state.registerState(scope, result);
    return result;
  }

  @Override
  public Stream<ManagedListener> children() {
    return Stream.of(state);
  }

  @Override
  public String getConfigSection() {
    var cfg = getClass().getAnnotation(Config.class);
    return cfg == null ? getClass().getSimpleName().toLowerCase(Locale.ROOT) : cfg.value();
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Config {
    String value();
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface AlwaysOn {}
}
