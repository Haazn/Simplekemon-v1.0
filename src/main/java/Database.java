import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDateTime;

//JDBC persistence. A committed gauntlet_wins row is one win, with six team rows
public final class Database {
    //These records are read-only data passed between JDBC code and the Hall of Fame UI
    public record TeamMember(int slot, int pokemonId, String name) {}
    public record HallOfFameEntry(String runId, LocalDateTime wonAt, List<TeamMember> team) {
        public HallOfFameEntry { team = List.copyOf(team); }
    }

    //Page victories first, then join all six slots so a page never splits a team
    public List<HallOfFameEntry> getHallOfFame(int offset, int limit) throws SQLException {
        //Page victories first, then join their members so one victory is never split.
        if (offset < 0 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Use a nonnegative offset and a limit from 1 to 100.");
        }
        String sql = "SELECT w.run_id, w.won_at, t.slot_number, t.pokemon_id, t.pokemon_name "
                + "FROM (SELECT run_id, won_at FROM gauntlet_wins "
                + "ORDER BY won_at DESC, run_id DESC LIMIT ? OFFSET ?) w "
                + "LEFT JOIN winning_team t ON t.run_id = w.run_id "
                + "ORDER BY w.won_at DESC, w.run_id DESC, t.slot_number";
        Map<String, List<TeamMember>> teams = new LinkedHashMap<>();
        Map<String, LocalDateTime> dates = new LinkedHashMap<>();
        try (Connection connection = connect();
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, limit);
            query.setInt(2, offset);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    String run = rows.getString("run_id");
                    teams.computeIfAbsent(run, key -> new ArrayList<>());
                    dates.putIfAbsent(run, rows.getTimestamp("won_at").toLocalDateTime());
                    if (rows.getObject("slot_number") != null) {
                        teams.get(run).add(new TeamMember(rows.getInt("slot_number"),
                                rows.getInt("pokemon_id"), rows.getString("pokemon_name")));
                    }
                }
            }
        }
        List<HallOfFameEntry> result = new ArrayList<>();
        teams.forEach((run, members) -> result.add(new HallOfFameEntry(run, dates.get(run), members)));
        return List.copyOf(result);
    }

    private Connection connect() throws SQLException {
        //Settings for MySQL server/account.
        //The class that both victory saving and HallOfFameUI actually use.
        String url = "jdbc:mysql://localhost:3306/pokemon_gauntlet";
        String username = "pokemon_app";
        String password = "pikamon";

        Properties settings = new Properties();
        settings.setProperty("user", username);
        settings.setProperty("password", password);
        settings.setProperty("connectTimeout", "5000");
        settings.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(url, settings);
    }

    //For database connectivity testing
    public static void main(String[] args) {
        try {
            Database database = new Database();
            long wins = database.getWinCount();
            List<HallOfFameEntry> history = database.getHallOfFame(0, 1);
            System.out.println("MySQL connection, win count and Hall of Fame query succeeded.");
            System.out.println("Recorded wins: " + wins);
            System.out.println(history.isEmpty() ? "No victories yet (this is normal for a fresh database)."
                    : "Latest saved team contains " + history.get(0).team().size() + " Pokémon.");
        } catch (SQLException e) {
            System.err.println("Database connection/query failed: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState() + "; MySQL error code: " + e.getErrorCode());
            System.err.println("Check connect() in Database.java, the running MySQL server, "
                    + "schema.sql, and the MySQL Connector/J runtime dependency.");
            System.exit(1);
        }
    }

    //The same run UUID can be retried without creating another win
    public long recordVictory(BattleLogic battle) throws SQLException {
        //A victory and its six team members are one transaction.
        if (battle.getStatus() != BattleLogic.Status.WON) {
            throw new IllegalStateException("Only a completed gauntlet victory can be recorded.");
        }
        String runId = battle.getRunId();
        List<Arceus.PokemonData> team = battle.getPlayerTeam();
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement win = connection.prepareStatement(
                        "INSERT INTO gauntlet_wins (run_id) VALUES (?)")) {
                    win.setString(1, runId);
                    win.executeUpdate();
                }
                try (PreparedStatement member = connection.prepareStatement(
                        "INSERT INTO winning_team "
                                + "(run_id, slot_number, pokemon_id, pokemon_name) VALUES (?, ?, ?, ?)")) {
                    for (int i = 0; i < team.size(); i++) {
                        //Selection order becomes slot_number and is shown in the Hall of Fame.
                        member.setString(1, runId);
                        member.setInt(2, i + 1);
                        member.setInt(3, team.get(i).id);
                        member.setString(4, team.get(i).name);
                        member.addBatch();
                    }
                    member.executeBatch();
                }
                connection.commit();
            } catch (SQLIntegrityConstraintViolationException e) {
                connection.rollback();
                //MySQL duplicate key: a retry may follow a successful but unacknowledged commit.
                if (e.getErrorCode() != 1062 || !isRecorded(connection, runId)) throw e;
            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
        return getWinCount();
    }

    private boolean isRecorded(Connection connection, String runId) throws SQLException {
        //Used after a duplicate-key error to distinguish a safe retry from a real failure.
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT run_id FROM gauntlet_wins WHERE run_id = ?")) {
            query.setString(1, runId);
            try (ResultSet rows = query.executeQuery()) { return rows.next(); }
        }
    }

    public long getWinCount() throws SQLException {
        //The view is represented by a count query here so the displayed total stays current.
        try (Connection connection = connect();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COUNT(*) FROM gauntlet_wins");
             ResultSet rows = query.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }
}
