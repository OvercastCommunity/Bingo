package tc.oc.bingo.database;

import java.util.Map;

public class BingoPlayerCard {

  Map<String, ProgressItem> progressList;

  public BingoPlayerCard(Map<String, ProgressItem> progressList) {
    this.progressList = progressList;
  }

  public Map<String, ProgressItem> getProgressList() {
    return progressList;
  }

  public void setProgressList(Map<String, ProgressItem> progressList) {
    this.progressList = progressList;
  }
}
