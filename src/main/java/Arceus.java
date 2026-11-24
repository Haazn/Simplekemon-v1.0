import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
complete data for all Pokemon found in the program.
this lists their stats, available moves, and a simplified IV calculation.
stats were taken from pokemondb.net; minimum HP and base attack, speed, and defense values were chosen.

data supplied will be ussed in battle, and in the battle's UI
 */
class Arceus {
    static final class PokemonData {
        final int id;
        final String name;
        final String type;
        final int hp;
        final int atk;
        final int def;
        final int speed;
        final String move1Name;
        final int move1Power;
        final String move2Name;
        final int move2Power;
        final double IV;
        int currenthp; //will be used for battle, when hp decreases

        //Supplies a random IV multiplier through randomIV()

        //Constructor for player pokemons
        PokemonData(
                int id,
                String name,
                String type,
                int baseHp,
                int baseAtk,
                int baseDef,
                int baseSpd,
                String move1Name,
                String move2Name) {
            this(
                    id,
                    name,
                    type,
                    baseHp,
                    baseAtk,
                    baseDef,
                    baseSpd,
                    move1Name,
                    move2Name,
                    randomIV()
            );
        }

        //Constructor for enemy pokemons
        PokemonData(
                int id,
                String name,
                String type,
                int baseHp,
                int baseAtk,
                int baseSpd,
                String move1Name,
                String move2Name,
                double IV) {
            this(
                    id,
                    name,
                    type,
                    baseHp,
                    baseAtk,
                    defaultBaseDef(type),
                    baseSpd,
                    move1Name,
                    move2Name,
                    IV
            );
        }

        //Full 10 parameter constructor with explicit baseDef and IV multiplier
        //Uses math.round for clear data values and math.max for safety net in case of a low IV roll
        PokemonData(
                int id,
                String name,
                String type,
                int baseHp,
                int baseAtk,
                int baseDef,
                int baseSpd,
                String move1Name,
                String move2Name,
                double IV) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.IV = IV;
            this.hp = (int) Math.round(baseHp * IV);
            this.atk = (int) Math.round(baseAtk * IV);
            this.def = Math.max(1, (int) Math.round(baseDef * IV));
            this.speed = (int) Math.round(baseSpd * IV);
            this.move1Name = move1Name;
            // Move power uses the IV-scaled attack plus a small random bonus from 0 to 9.
            this.move1Power = Math.max(1, this.atk + Pokemon.rng.nextInt(10));
            this.move2Name = move2Name;
            this.move2Power = Math.max(1, this.atk + Pokemon.rng.nextInt(10));
            this.currenthp = this.hp;
        }
    }
    
    //Pokemon defense calculation
    private static int defaultBaseDef(String type) {
        if (type == null) return 75;
        String primary = type.split("/")[0].trim().toLowerCase(Locale.ROOT);
        return switch (primary) {
            case "rock", "ground", "steel" -> 105;
            case "water", "grass", "ice" -> 85;
            case "fighting", "dragon", "poison" -> 80;
            case "normal", "fire", "flying" -> 75;
            case "psychic", "electric", "ghost" -> 65;
            default -> 75;
        };
    }

    //IV Selector
    static double randomIV() {
        int roll = Pokemon.rng.nextInt(100);

        if (roll < 33) {
            return 0.88;
        } else if (roll < 66) {
            return 1.02;
        } else {
            return 1.12;
        }
    }
    
    //Map of moves and their types
    private static final Map<String, String> MOVE_TYPES = Map.ofEntries(
            Map.entry("vine whip", "Grass"),
            Map.entry("razor leaf", "Grass"),
            Map.entry("ember", "Fire"),
            Map.entry("flame burst", "Fire"),
            Map.entry("blaze kick", "Fire"),
            Map.entry("water gun", "Water"),
            Map.entry("aqua tail", "Water"),
            Map.entry("water pulse", "Water"),
            Map.entry("hydro pump", "Water"),
            Map.entry("bite", "Dark"),
            Map.entry("crunch", "Dark"),
            Map.entry("dark pulse", "Dark"),
            Map.entry("tackle", "Normal"),
            Map.entry("body slam", "Normal"),
            Map.entry("pound", "Normal"),
            Map.entry("dragon claw", "Dragon"),
            Map.entry("dragon tail", "Dragon"),
            Map.entry("wing attack", "Flying"),
            Map.entry("shadow ball", "Ghost"),
            Map.entry("shadow claw", "Ghost"),
            Map.entry("lick", "Ghost"),
            Map.entry("karate chop", "Fighting"),
            Map.entry("low sweep", "Fighting"),
            Map.entry("mach punch", "Fighting"),
            Map.entry("brick break", "Fighting"),
            Map.entry("thunder shock", "Electric"),
            Map.entry("spark", "Electric"),
            Map.entry("rock throw", "Rock"),
            Map.entry("rock slide", "Rock"),
            Map.entry("bulldoze", "Ground"),
            Map.entry("earthquake", "Ground"),
            Map.entry("ice shard", "Ice"),
            Map.entry("powder snow", "Ice"),
            Map.entry("sludge wave", "Poison"),
            Map.entry("toxic sludge", "Poison"),
            Map.entry("psycher beam", "Psychic")
    );

    static String getMoveType(String moveName, String userType) {
        //Named moves use the chart; an unknown move falls back to the user's primary type.
        if (moveName != null) {
            String mapped = MOVE_TYPES.get(moveName.trim().toLowerCase(Locale.ROOT));
            if (mapped != null) return mapped;
        }
        if (userType != null && !userType.isBlank()) {
            return userType.split("/")[0].trim();
        }
        return "Normal";
    }

    //Pokemon type effectiveness chart (attacking move type -> defending type -> multiplier)
    private static final Map<String, Map<String, Double>> EFFECTIVENESS_CHART = Map.ofEntries(
            Map.entry("Normal", Map.of(
                    "Rock", 0.5, "Ghost", 0.0
            )),
            Map.entry("Fire", Map.of(
                    "Fire", 0.5, "Water", 0.5, "Grass", 2.0,
                    "Ice", 2.0, "Rock", 0.5, "Dragon", 0.5
            )),
            Map.entry("Water", Map.of(
                    "Fire", 2.0, "Water", 0.5, "Grass", 0.5,
                    "Ground", 2.0, "Rock", 2.0, "Dragon", 0.5
            )),
            Map.entry("Electric", Map.of(
                    "Water", 2.0, "Electric", 0.5, "Grass", 0.5,
                    "Ground", 0.0, "Flying", 2.0, "Dragon", 0.5
            )),
            Map.entry("Grass", Map.of(
                    "Fire", 0.5, "Water", 2.0, "Grass", 0.5,
                    "Poison", 0.5, "Ground", 2.0, "Flying", 0.5,
                    "Rock", 2.0, "Dragon", 0.5
            )),
            Map.entry("Ice", Map.of(
                    "Fire", 0.5, "Water", 0.5, "Grass", 2.0,
                    "Ice", 0.5, "Ground", 2.0, "Flying", 2.0, "Dragon", 2.0
            )),
            Map.entry("Fighting", Map.of(
                    "Normal", 2.0, "Ice", 2.0, "Poison", 0.5,
                    "Flying", 0.5, "Psychic", 0.5, "Rock", 2.0, "Ghost", 0.0
            )),
            Map.entry("Poison", Map.of(
                    "Grass", 2.0, "Poison", 0.5, "Ground", 0.5,
                    "Rock", 0.5, "Ghost", 0.5
            )),
            Map.entry("Ground", Map.of(
                    "Fire", 2.0, "Electric", 2.0, "Grass", 0.5,
                    "Poison", 2.0, "Flying", 0.0, "Rock", 2.0
            )),
            Map.entry("Flying", Map.of(
                    "Electric", 0.5, "Grass", 2.0, "Fighting", 2.0,
                    "Rock", 0.5
            )),
            Map.entry("Psychic", Map.of(
                    "Fighting", 2.0, "Poison", 2.0, "Psychic", 0.5
            )),
            Map.entry("Rock", Map.of(
                    "Fire", 2.0, "Ice", 2.0, "Fighting", 0.5,
                    "Ground", 0.5, "Flying", 2.0
            )),
            Map.entry("Ghost", Map.of(
                    "Normal", 0.0, "Psychic", 2.0, "Ghost", 2.0
            )),
            Map.entry("Dragon", Map.of(
                    "Dragon", 2.0
            )),
            Map.entry("Dark", Map.of(
                    "Fighting", 0.5, "Psychic", 2.0, "Ghost", 2.0
            ))
    );

    static double typeEffectiveness(String moveType, String defenderType) {
        // Multiply each defending type so dual-type matchups behave like the reference chart.
        if (moveType == null || defenderType == null || defenderType.isBlank()) {
            return 1.0;
        }
        Map<String, Double> matchups = EFFECTIVENESS_CHART.getOrDefault(
                capitalize(moveType.trim()), Map.of()); //Looks up moveType in EFFECTIVENESS_CHART, or gets the default value of move type isnt listed which is 1.0
        double total = 1.0;
        for (String part : defenderType.split("/")) { //For every part of defenderType.split(), add the effectiveness to total
            String defPart = capitalize(part.trim());
            total *= matchups.getOrDefault(defPart, 1.0);
        }
        return total;
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    //creates all of the pokemon found in the program
    static List<PokemonData> create() {
        // The selection screen uses this catalog; each call creates fresh mutable battle objects.
        return List.of(
                new PokemonData(1, "Venusaur",
                        "Grass/Poison",
                        270, 82, 83, 80,
                        "Razor Leaf", "Toxic Sludge"),
                new PokemonData(2, "Charizard",
                        "Fire/Flying",
                        266, 84, 78, 100,
                        "Flame Burst", "Wing Attack"),
                new PokemonData(3, "Blastoise",
                        "Water",
                        268, 83, 100, 78,
                        "Body Slam", "Aqua Tail"),
                new PokemonData(4, "Gyarados",
                        "Water/Flying",
                        300, 125, 79, 81,
                        "Bite", "Aqua Tail"),
                new PokemonData(5, "Snorlax",
                        "Normal",
                        430, 110, 65, 30,
                        "Tackle", "Body Slam"),
                new PokemonData(6, "Dragonite",
                        "Dragon/Flying",
                        292, 134, 95, 80,
                        "Dragon Claw", "Wing Attack"),
                new PokemonData(7, "Gengar",
                        "Ghost/Poison",
                        230, 65, 60, 110,
                        "Shadow Ball", "Toxic Sludge"),
                new PokemonData(8, "Machamp",
                        "Fighting",
                        290, 130, 80, 55,
                        "Karate Chop", "Low Sweep"),
                new PokemonData(9, "Raichu",
                        "Electric",
                        230, 90, 55, 110,
                        "Thunder Shock", "Spark"),
                new PokemonData(10, "Golem",
                        "Rock/Ground",
                        270, 120, 130, 45,
                        "Rock Throw", "Bulldoze"),
                new PokemonData(11, "Lapras",
                        "Water/Ice",
                        370, 85, 80, 60,
                        "Water Pulse", "Ice Shard"),
                new PokemonData(12, "Jynx",
                        "Ice/Psychic",
                        240, 50, 45, 95,
                        "Powder Snow", "Psycher Beam")
        );
    }
}
