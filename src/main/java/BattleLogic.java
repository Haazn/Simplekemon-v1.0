import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** All turn rules and gauntlet progression; no Swing or SQL dependencies. */
public final class BattleLogic {
    //The UI uses these states to decide which actions and buttons are available.
    public enum Status { FIGHTING, STAGE_WON, WON, LOST }

    private final String runId = UUID.randomUUID().toString();
    private final List<Arceus.PokemonData> playerTeam;
    private final List<List<Arceus.PokemonData>> gauntlet;
    private final Random random;
    private int stage, playerIndex, enemyIndex;
    private Status status = Status.FIGHTING;

    public BattleLogic(List<Arceus.PokemonData> playerTeam,
                       List<List<Arceus.PokemonData>> gauntlet) {
        //Production runs use random enemy move selection; tests inject a Random.
        this(playerTeam, gauntlet, new Random());
    }

    //Verifies playerTeam and Gauntlet validity
    BattleLogic(List<Arceus.PokemonData> playerTeam,
                List<List<Arceus.PokemonData>> gauntlet, Random random) {
        Objects.requireNonNull(playerTeam, "Player team");
        Objects.requireNonNull(gauntlet, "Gauntlet");
        if (playerTeam.size() != 6 || gauntlet.size() != 4) {
            throw new IllegalArgumentException("Choose six Pokémon and four enemy teams.");
        }
        Set<Arceus.PokemonData> identities =
                Collections.newSetFromMap(new IdentityHashMap<>()); //Set that tracks object references
        this.playerTeam = validateTeam(playerTeam, identities, 6, 6);
        List<List<Arceus.PokemonData>> stages = new ArrayList<>();
        for (List<Arceus.PokemonData> team : gauntlet) { //Loops through every enemy team and validates them
            stages.add(validateTeam(team, identities, 5, 6)); // Adds team to stage after validation
        }
        this.gauntlet = List.copyOf(stages); //Puts a copy of stage to gauntlet so it becomes unchangeable
        this.random = Objects.requireNonNull(random); //Checks if random is not null before storing
    }

    private static List<Arceus.PokemonData> validateTeam(
            List<Arceus.PokemonData> team, Set<Arceus.PokemonData> identities, int minSize, int maxSize) {
        // Identity-based validation prevents two slots from sharing one mutable object.
        if (team == null || team.size() < minSize || team.size() > maxSize) {
            throw new IllegalArgumentException(
                    "Team size must be between " + minSize + " and " + maxSize + " Pokémon.");
        }
        for (Arceus.PokemonData p : team) {
            if (p == null || !identities.add(p)) { //!identities.add(p) checks whether p is not addable(returns false) or already present in identities(also returns false)
                throw new IllegalArgumentException("Every team slot needs a separate Pokémon object.");
            }
            if (p.hp <= 0 || p.currenthp != p.hp || p.speed < 0) {
                throw new IllegalArgumentException("Start each run with fresh, healthy Pokémon.");
            }
        }
        return List.copyOf(team);
    }
    
    //Getters
    public String getRunId() { return runId; }
    public Status getStatus() { return status; }
    public int getStageNumber() { return stage + 1; }
    public int getStageCount() { return gauntlet.size(); }
    public List<Arceus.PokemonData> getPlayerTeam() { return playerTeam; }
    public Arceus.PokemonData getPlayerActive() { return playerTeam.get(playerIndex); }
    public Arceus.PokemonData getEnemyActive() { return gauntlet.get(stage).get(enemyIndex); }

    public String attack(int moveNumber) {
        //This is the main battle entry point called by the two move buttons.
        requireFight();
        if (moveNumber != 1 && moveNumber != 2) {
            throw new IllegalArgumentException("Choose move 1 or 2.");
        }
        Arceus.PokemonData player = getPlayerActive();
        Arceus.PokemonData enemy = getEnemyActive();
        int enemyMove = random.nextInt(2) + 1; //Enemy randomly chooses move 1 or 2 
        StringBuilder log = new StringBuilder();
        if (player.speed == enemy.speed) { //If speed tie, then random 50/50
            int firstMove = random.nextInt(2);
            if (firstMove == 0) {
                hit(player, enemy, moveNumber, log);
                if (enemy.currenthp > 0) hit(enemy, player, enemyMove, log);
            }
            else {
                hit(enemy, player, enemyMove, log);
                if (player.currenthp > 0) hit(player, enemy, moveNumber, log);
            }
        }
        else if(player.speed > enemy.speed) {
            hit(player, enemy, moveNumber, log);
            if (enemy.currenthp > 0) hit(enemy, player, enemyMove, log);
        }
        else {
        hit(enemy, player, enemyMove, log);
        if (player.currenthp > 0) hit(player, enemy, moveNumber, log);
        }
        resolveFainting(log);
        return log.toString();
    }

    //Voluntary switching costs a turn, so the opponent attacks the newcomer
    public String switchPokemon(int index) {
        requireFight();
        if (index < 0 || index >= playerTeam.size() || index == playerIndex
                || playerTeam.get(index).currenthp <= 0) {
            throw new IllegalArgumentException("Choose a different Pokémon that has not fainted.");
        }
        playerIndex = index;
        StringBuilder log = new StringBuilder("Go, " + getPlayerActive().name + "!\n");
        hit(getEnemyActive(), getPlayerActive(), random.nextInt(2) + 1, log);
        resolveFainting(log);
        return log.toString();
    }

    private void hit(Arceus.PokemonData from, Arceus.PokemonData to,
            int move, StringBuilder log) {
        //Damage is calculated here so the UI only displays results and does not own rules.
        String moveName = move == 1 ? from.move1Name : from.move2Name; //Checks if move is 1 or 2
        int basePower = Math.max(1, move == 1 ? from.move1Power : from.move2Power); //Picks move power of selected move, math.max ensures it is at least one 
        String moveType = Arceus.getMoveType(moveName, from.type); //Gets moveType of the attack
        double effectiveness = Arceus.typeEffectiveness(moveType, to.type); //Determines effectiveness by passing the moveType and enemy type to the typeEffectiveness method 

        int rawDamage;
        if (effectiveness == 0.0) {
            rawDamage = 0;
        } else {
            //Defense scaling: 100 / (100 + def) keeps damage responsive and prevents walls from taking 0
            double defenseFactor = 100.0 / (100.0 + Math.max(0, to.def));
            rawDamage = Math.max(1, (int) Math.round(basePower * defenseFactor * effectiveness));
        }

        int actualDamage = Math.min(to.currenthp, rawDamage);
        to.currenthp = Math.max(0, to.currenthp - actualDamage);

        log.append(from.name).append(" used ").append(moveName);
        if (effectiveness == 0.0) {
            log.append(" — it had no effect!\n");
        } else {
            log.append(" — ").append(actualDamage).append(" damage");
            if (effectiveness >= 2.0) {
                log.append(" (Super effective!)");
            } else if (effectiveness < 1.0) {
                log.append(" (Not very effective...)");
            }
            log.append(".\n");
        }
    }

    private void resolveFainting(StringBuilder log) {
        //Replace fainted members, then decide whether the stage, run, or player is finished.
        if (getPlayerActive().currenthp == 0) {
            log.append(getPlayerActive().name).append(" fainted!\n");
            int next = firstAlive(playerTeam);
            if (next < 0) {
                status = Status.LOST;
                log.append("Your team was defeated.\n");
                return;
            }
            playerIndex = next;
            log.append("Go, ").append(getPlayerActive().name).append("!\n");
        }
        if (getEnemyActive().currenthp == 0) {
            log.append(getEnemyActive().name).append(" fainted!\n");
            int next = firstAlive(gauntlet.get(stage));
            if (next < 0) {
                status = stage + 1 == gauntlet.size() ? Status.WON : Status.STAGE_WON;
                log.append(status == Status.WON ? "You cleared all four teams!\n"
                        : "Stage cleared. Continue to the next opponent.\n");
                return;
            }
            enemyIndex = next;
            log.append("Opponent sent out ").append(getEnemyActive().name).append("!\n");
        }
    }

    private static int firstAlive(List<Arceus.PokemonData> team) {
        for (int i = 0; i < team.size(); i++) if (team.get(i).currenthp > 0) return i;
        return -1;
    }

    public String nextStage(boolean healPlayer) {
        //Only a completed stage may advance to the next fixed enemy team.
        if (status != Status.STAGE_WON) {
            throw new IllegalStateException("Defeat the current team before advancing.");
        }
        if (healPlayer) for (Arceus.PokemonData p : playerTeam) p.currenthp = p.hp;
        playerIndex = firstAlive(playerTeam);
        stage++;
        enemyIndex = 0;
        status = Status.FIGHTING;
        return "Stage " + getStageNumber() + " begins. "
                + (healPlayer ? "Your team was healed. " : "")
                + "Opponent sent out " + getEnemyActive().name + ".\n";
    }

    private void requireFight() {
        if (status != Status.FIGHTING) throw new IllegalStateException("Battle is not active.");
    }
}
