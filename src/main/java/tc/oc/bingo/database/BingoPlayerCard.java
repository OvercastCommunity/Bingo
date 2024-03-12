package tc.oc.bingo.database;

import java.util.HashMap;

public class BingoPlayerCard {

  HashMap<String, ProgressItem> progressList = new HashMap<>();

  public BingoPlayerCard(HashMap<String, ProgressItem> progressList) {
    this.progressList = progressList;
  }

  public HashMap<String, ProgressItem> getProgressList() {
    return progressList;
  }

  public void setProgressList(HashMap<String, ProgressItem> progressList) {
    this.progressList = progressList;
  }
}
