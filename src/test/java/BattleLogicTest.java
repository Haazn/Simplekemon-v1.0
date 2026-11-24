import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import java.io.File;

/** Dependency-free regression checks; run main(), not a Swing window. */
public final class BattleLogicTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        // Count every assertion so the final line gives defense-ready evidence.
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Arceus.PokemonData pokemon(int hp, int attack, int speed) {
        // Fixed stats make battle behavior deterministic and easy to explain.
        return new Arceus.PokemonData(1, "Test", "Normal", hp, attack, speed,
                "Weak move", "Strong move", 1.0);
    }

    private static List<Arceus.PokemonData> team(int hp, int attack, int speed) {
        // Build the six-member player team required by BattleLogic.
        List<Arceus.PokemonData> team = new ArrayList<>();
        for (int i = 0; i < 6; i++) team.add(pokemon(hp, attack, speed));
        return team;
    }

    private static List<List<Arceus.PokemonData>> enemies(int hp, int attack, int speed) {
        // Build four test stages without relying on the real Elite Four data.
        List<List<Arceus.PokemonData>> teams = new ArrayList<>();
        for (int i = 0; i < 4; i++) teams.add(team(hp, attack, speed));
        return teams;
    }

    public static void main(String[] args) throws Exception {
        // Speed tie and knockout ordering: the player attacks first and a fainted target cannot retaliate.
        List<Arceus.PokemonData> players = team(100, 200, 100);
        BattleLogic battle = new BattleLogic(players, enemies(50, 20, 100), new Random(1));
        battle.attack(2);
        check(players.get(0).currenthp == 100, "Player wins speed tie; fainted enemy cannot retaliate");
        check(battle.getEnemyActive().currenthp == 50, "Next enemy enters with full HP");

        players = team(50, 20, 10);
        battle = new BattleLogic(players, enemies(500, 500, 100), new Random(1));
        battle.attack(1);
        check(players.get(0).currenthp == 0, "Overkill clamps HP to zero");
        check(battle.getEnemyActive().currenthp == 500, "Fainted player cannot retaliate");
        check(battle.getPlayerActive() == players.get(1), "Next healthy Pokémon enters automatically");
        for (int i = 0; i < 5; i++) battle.attack(1);
        check(battle.getStatus() == BattleLogic.Status.LOST, "Six fainted players end the run");
        try { battle.attack(1); throw new AssertionError("Attack after loss accepted"); }
        catch (IllegalStateException expected) { checks++; }

        players = team(500, 50, 100);
        // Switching preserves the outgoing HP but consumes the player's turn.
        battle = new BattleLogic(players, enemies(500, 20, 10), new Random(1));
        players.get(0).currenthp = 123;
        battle.switchPokemon(1);
        check(players.get(0).currenthp == 123, "Switching preserves the outgoing Pokémon's HP");
        check(players.get(1).currenthp < 500, "Switching costs a turn");
        players.get(2).currenthp = 0;
        try { battle.switchPokemon(2); throw new AssertionError("Fainted switch accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
        try { battle.attack(3); throw new AssertionError("Invalid move accepted"); }
        catch (IllegalArgumentException expected) { checks++; }

        players = team(100, 500, 500);
        // Four stages, automatic healing, and the final WON state.
        battle = new BattleLogic(players, enemies(20, 20, 1), new Random(1));
        for (int stage = 0; stage < 4; stage++) {
            for (int i = 0; i < 6; i++) battle.attack(2);
            if (stage < 3) {
                check(battle.getStatus() == BattleLogic.Status.STAGE_WON, "Only complete stages advance");
                players.get(0).currenthp = 12;
                players.get(3).currenthp = 0;
                battle.nextStage(true);
                check(players.get(0).currenthp == 100 && players.get(3).currenthp == 100,
                        "Between-stage healing restores damaged and fainted Pokémon");
            }
        }
        check(battle.getStatus() == BattleLogic.Status.WON, "All 24 opponents produce exactly one completed run");
        try { battle.nextStage(true); throw new AssertionError("Fifth stage accepted"); }
        catch (IllegalStateException expected) { checks++; }

        players = team(100, 500, 500);
        // The optional no-heal mode carries HP into the next stage.
        battle = new BattleLogic(players, enemies(20, 20, 1), new Random(1));
        for (int i = 0; i < 6; i++) battle.attack(2);
        players.get(0).currenthp = 37;
        battle.nextStage(false);
        check(players.get(0).currenthp == 37, "Optional no-heal mode carries HP across stages");

        List<List<Arceus.PokemonData>> first = ArceusEnemy.createGauntlet();
        // Fresh-run isolation: changing one gauntlet must not damage another.
        List<List<Arceus.PokemonData>> second = ArceusEnemy.createGauntlet();
        new BattleLogic(Arceus.create().subList(0, 6), first);
        first.get(0).get(0).currenthp = 0;
        check(second.get(0).get(0).currenthp == second.get(0).get(0).hp,
                "New run does not reuse damaged enemy objects");
        check(first.get(0).get(0) != first.get(1).get(4),
                "Same species in different stages has independent HP");

        List<Arceus.PokemonData> invalid = team(100, 100, 100);
        // Shared object identity is rejected because each slot needs its own mutable state.
        invalid.set(1, invalid.get(0));
        try { new BattleLogic(invalid, enemies(50, 20, 10)); throw new AssertionError("Shared object accepted"); }
        catch (IllegalArgumentException expected) { checks++; }

        for (int i = 1; i <= 12; i++) {
            // Verify the selection-screen sprites can be decoded from the project root.
            check(ImageIO.read(new File("images/char" + i + ".png")) != null,
                    "Sprite " + i + " loads");
        }
        System.out.println("PASS: " + checks + " battle and image checks.");
    }
}
