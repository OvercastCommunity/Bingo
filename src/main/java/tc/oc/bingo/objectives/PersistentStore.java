package tc.oc.bingo.objectives;

public abstract interface PersistentStore<T> {

  public abstract T getDataFromString(String input);

  public abstract String getStringForStore(T data);
}
