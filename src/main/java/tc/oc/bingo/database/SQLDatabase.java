package tc.oc.bingo.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import tc.oc.bingo.config.Config;
import tc.oc.bingo.util.ExceptionHandlingExecutorService;
import tc.oc.occ.database.ConnectionPool;

public class SQLDatabase implements BingoDatabase {

  private static final String BINGO_OBJECTIVES = "bingo_objectives";
  private static final String BINGO_PROGRESS = "bingo_progress";

  public static final String CREATE_OBJECTIVE_TABLE_SQL =
      "CREATE TABLE IF NOT EXISTS "
          + BINGO_OBJECTIVES
          + " ("
          + "slug VARCHAR(255),"
          + "name VARCHAR(255),"
          + "season_id INT,"
          + "description TEXT,"
          + "idx INT,"
          + "hint_level INT,"
          + "next_clue_unlock DATETIME,"
          + "locked_until DATETIME,"
          + "discovery_uuid VARCHAR(255),"
          + "discovery_time DATETIME, "
          + "PRIMARY KEY (slug, season_id)"
          + ")";

  public static final String CREATE_PROGRESS_TABLE_SQL =
      "CREATE TABLE IF NOT EXISTS "
          + BINGO_PROGRESS
          + " ("
          + "player_uuid VARCHAR(255),"
          + "objective_slug VARCHAR(255),"
          + "season_id INT,"
          + "completed BOOLEAN DEFAULT FALSE,"
          + "completed_at DATETIME,"
          + "placed_position INT,"
          + "data TEXT,"
          + "PRIMARY KEY (player_uuid, objective_slug, season_id)"
          + ")";

  private static final String SELECT_CARD_SQL =
      "SELECT * FROM " + BINGO_OBJECTIVES + " WHERE idx != -1 AND season_id = ?";

  private static final String SELECT_PROGRESS_SQL =
      "SELECT * FROM " + BINGO_PROGRESS + " WHERE player_uuid = ? AND season_id = ?";

  private static final String COUNT_COMPLETED_SQL =
      "SELECT COUNT(*) FROM "
          + BINGO_PROGRESS
          + " WHERE objective_slug = ? AND completed AND season_id = ?";

  private static final String UPSERT_COMPLETED_SQL =
      "INSERT INTO "
          + BINGO_PROGRESS
          + " (player_uuid, objective_slug, season_id, completed, completed_at, placed_position) "
          + "VALUES (?, ?, ?, ?, ?, ?) "
          + "ON DUPLICATE KEY UPDATE completed = VALUES(completed), completed_at = VALUES(completed_at), placed_position = VALUES(placed_position)";

  private static final String UPSERT_PROGRESS_SQL =
      "INSERT INTO "
          + BINGO_PROGRESS
          + " (player_uuid, objective_slug, season_id, data) "
          + "VALUES (?, ?, ?, ?) "
          + "ON DUPLICATE KEY UPDATE data = VALUES(data)";

  private static final String UPDATE_OBJECTIVE_DISCOVERY_SQL =
      "UPDATE "
          + BINGO_OBJECTIVES
          + " SET discovery_uuid = ?, discovery_time = ? WHERE slug = ? and season_id = ?";

  private static final ExceptionHandlingExecutorService EXECUTOR =
      new ExceptionHandlingExecutorService(Executors.newFixedThreadPool(5));

  private final ConnectionPool pool;

  public SQLDatabase(ConnectionPool pool) {
    this.pool = pool;
    createTables();
  }

  private Connection getConnection() throws SQLException {
    return Objects.requireNonNull(pool.getPool()).getConnection();
  }

  private void createTables() {
    CompletableFuture.runAsync(this::createTablesImpl, EXECUTOR);
  }

  @SneakyThrows
  public void createTablesImpl() {
    @Cleanup Connection conn = getConnection();

    @Cleanup PreparedStatement objectiveTable = conn.prepareStatement(CREATE_OBJECTIVE_TABLE_SQL);
    objectiveTable.execute();
    @Cleanup PreparedStatement progressTable = conn.prepareStatement(CREATE_PROGRESS_TABLE_SQL);
    progressTable.execute();
  }

  @Override
  public CompletableFuture<BingoCard> getCard() {
    return CompletableFuture.supplyAsync(this::getBingoCardImpl, EXECUTOR);
  }

  @SneakyThrows
  private BingoCard getBingoCardImpl() {
    @Cleanup Connection connection = getConnection();
    @Cleanup PreparedStatement stm = connection.prepareStatement(SELECT_CARD_SQL);
    stm.setInt(1, Config.get().getSeasonId());
    @Cleanup ResultSet resultSet = stm.executeQuery();

    List<ObjectiveItem> objectives = new ArrayList<>(25);
    while (resultSet.next()) {
      ObjectiveItem objective =
          new ObjectiveItem(
              resultSet.getString("slug"),
              resultSet.getString("name"),
              resultSet.getString("description"),
              resultSet.getInt("idx"),
              resultSet.getInt("hint_level"),
              parseTimestamp(resultSet.getTimestamp("next_clue_unlock")),
              parseTimestamp(resultSet.getTimestamp("locked_until")),
              parseUuid(resultSet.getString("discovery_uuid")),
              parseTimestamp(resultSet.getTimestamp("discovery_time")));
      objectives.add(objective);
    }

    return new BingoCard(objectives);
  }

  @Override
  public CompletableFuture<BingoPlayerCard> getPlayerCard(UUID playerId) {
    return CompletableFuture.supplyAsync(() -> getCardImpl(playerId), EXECUTOR);
  }

  @SneakyThrows
  private BingoPlayerCard getCardImpl(UUID playerId) {
    @Cleanup Connection connection = getConnection();
    @Cleanup PreparedStatement stm = connection.prepareStatement(SELECT_PROGRESS_SQL);
    stm.setString(1, playerId.toString());
    stm.setInt(2, Config.get().getSeasonId());

    @Cleanup ResultSet resultSet = stm.executeQuery();

    Map<String, ProgressItem> progresses = new HashMap<>();
    BingoPlayerCard card = new BingoPlayerCard(playerId, progresses);
    while (resultSet.next()) {
      ProgressItem item =
          new ProgressItem(
              card,
              resultSet.getString("objective_slug"),
              resultSet.getBoolean("completed"),
              resultSet.getInt("placed_position"),
              resultSet.getString("data"));
      progresses.put(item.getObjectiveSlug(), item);
    }
    return card;
  }

  @Override
  public CompletableFuture<Integer> rewardPlayers(List<UUID> players, String objectiveSlug) {
    return CompletableFuture.supplyAsync(() -> rewardPlayersImpl(players, objectiveSlug), EXECUTOR);
  }

  @SneakyThrows
  private Integer rewardPlayersImpl(List<UUID> players, String objectiveSlug) {
    @Cleanup Connection conn = getConnection();

    int nextPos = getCompletionCountImpl(conn, objectiveSlug) + 1;

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    storePlayerCompletionsImpl(conn, players, objectiveSlug, nextPos, timestamp);

    if (nextPos == 1) {
      UUID playerUUID = players.size() == 1 ? players.get(0) : null;
      storeGoalDiscovererImpl(conn, playerUUID, objectiveSlug, timestamp);
    }

    return nextPos;
  }

  @SneakyThrows
  private int getCompletionCountImpl(Connection conn, String objectiveSlug) {
    @Cleanup PreparedStatement stmt = conn.prepareStatement(COUNT_COMPLETED_SQL);
    stmt.setString(1, objectiveSlug);
    stmt.setInt(2, Config.get().getSeasonId());

    @Cleanup ResultSet resultSet = stmt.executeQuery();
    int numCompleted = 0;
    if (resultSet.next()) {
      numCompleted = resultSet.getInt(1);
    }

    return numCompleted;
  }

  @Override
  public CompletableFuture<Void> storePlayerProgress(
      UUID playerId, String objectiveSlug, String dataAsString) {
    return CompletableFuture.runAsync(
        () -> storePlayerProgressImpl(playerId, objectiveSlug, dataAsString), EXECUTOR);
  }

  @SneakyThrows
  private void storePlayerProgressImpl(UUID playerId, String objectiveSlug, String dataAsString) {
    @Cleanup Connection conn = getConnection();
    @Cleanup PreparedStatement stmt = conn.prepareStatement(UPSERT_PROGRESS_SQL);
    stmt.setString(1, playerId.toString());
    stmt.setString(2, objectiveSlug);
    stmt.setInt(3, Config.get().getSeasonId());
    stmt.setString(4, dataAsString);
    stmt.executeUpdate();
  }

  @SneakyThrows
  private void storePlayerCompletionsImpl(
      Connection conn,
      List<UUID> uuids,
      String objectiveSlug,
      Integer position,
      Timestamp timestamp) {
    @Cleanup PreparedStatement stmt = conn.prepareStatement(UPSERT_COMPLETED_SQL);

    boolean isBatch = uuids.size() > 1;

    for (UUID uuid : uuids) {
      stmt.setString(1, uuid.toString());
      stmt.setString(2, objectiveSlug);
      stmt.setInt(3, Config.get().getSeasonId());
      stmt.setBoolean(4, true);
      stmt.setTimestamp(5, timestamp);
      stmt.setInt(6, position);

      if (isBatch) stmt.addBatch();
    }

    if (isBatch) stmt.executeBatch();
    else stmt.executeUpdate();
  }

  @SneakyThrows
  private void storeGoalDiscovererImpl(
      Connection conn, @Nullable UUID uuid, String objectiveSlug, Timestamp timestamp) {
    @Cleanup PreparedStatement stmt = conn.prepareStatement(UPDATE_OBJECTIVE_DISCOVERY_SQL);

    stmt.setString(1, (uuid != null) ? uuid.toString() : null);
    stmt.setTimestamp(2, timestamp);
    stmt.setString(3, objectiveSlug);
    stmt.setInt(4, Config.get().getSeasonId());
    stmt.executeUpdate();
  }

  private static LocalDateTime parseTimestamp(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  private static UUID parseUuid(String string) {
    return string == null ? null : UUID.fromString(string);
  }
}
