import java.util.List;

/**
 * Factory methods for the four fixed Elite Four teams.
 * Each method creates fresh PokemonData objects so a new run cannot reuse
 * damaged HP values or object identity from an earlier run.
 */
class ArceusEnemy {

    //Id's not in Arceus are unique to enemies 
    //1. Lorelei (Ice/Water)
    static List<Arceus.PokemonData> enemyOne() {
        return List.of(
                new Arceus.PokemonData(
                        13, "Dewgong", "Water/Ice",
                        290, 70, 80, 70,
                        "Water Pulse", "Ice Shard", 1.12),

                new Arceus.PokemonData(
                        14, "Cloyster", "Water/Ice",
                        210, 95, 180, 70,
                        "Hydro Pump", "Ice Shard", 1.12),

                new Arceus.PokemonData(
                        15, "Slowbro", "Water/Psychic",
                        300, 75, 110, 30,
                        "Water Pulse", "Body Slam", 1.12),

                new Arceus.PokemonData(
                        12, "Jynx", "Ice/Psychic",
                        240, 50, 35, 95,
                        "Psycher Beam", "Powder Snow", 1.12),

                new Arceus.PokemonData(
                        11, "Lapras", "Water/Ice",
                        370, 85, 80, 60,
                        "Water Pulse", "Ice Shard", 1.12)
        );
    }

    //2. Bruno (Fighting/Rock)
    static List<Arceus.PokemonData> enemyTwo() {
        return List.of(
                new Arceus.PokemonData(
                        16, "Onix", "Rock/Ground",
                        180, 45, 160, 70,
                        "Rock Throw", "Bulldoze", 1.12),

                new Arceus.PokemonData(
                        17, "Hitmonchan", "Fighting",
                        210, 105, 79, 76,
                        "Mach Punch", "Brick Break", 1.12),

                new Arceus.PokemonData(
                        18, "Hitmonlee", "Fighting",
                        210, 120, 53, 87,
                        "Low Sweep", "Blaze Kick", 1.12),

                new Arceus.PokemonData(
                        16, "Onix", "Rock/Ground",
                        180, 45, 160, 70,
                        "Rock Slide", "Earthquake", 1.12),

                new Arceus.PokemonData(
                        8, "Machamp", "Fighting",
                        290, 130, 80, 55,
                        "Karate Chop", "Low Sweep", 1.12)
        );
    }

    //3. Agatha (Ghost/Poison)
    static List<Arceus.PokemonData> enemyThree() {
        return List.of(
                new Arceus.PokemonData(
                        7, "Gengar", "Ghost/Poison",
                        230, 65, 60, 110,
                        "Shadow Ball", "Lick", 1.12),

                new Arceus.PokemonData(
                        19, "Golbat", "Poison/Flying",
                        260, 80, 70, 90,
                        "Wing Attack", "Bite", 1.12),

                new Arceus.PokemonData(
                        7, "Gengar", "Ghost/Poison",
                        230, 65, 60, 110,
                        "Shadow Claw", "Dark Pulse", 1.12),

                new Arceus.PokemonData(
                        20, "Arbok", "Poison",
                        230, 95, 69, 80,
                        "Bite", "Crunch", 1.12),

                new Arceus.PokemonData(
                        7, "Gengar", "Ghost/Poison",
                        230, 65, 60, 110,
                        "Shadow Ball", "Sludge Wave", 1.12)
        );
    }

    //4. Lance (Dragon/Flying)
    static List<Arceus.PokemonData> enemyFour() {
        return List.of(
                new Arceus.PokemonData(
                        4, "Gyarados", "Water/Flying",
                        300, 125, 79, 81,
                        "Bite", "Aqua Tail", 1.12),

                new Arceus.PokemonData(
                        21, "Dragonair", "Dragon",
                        232, 84, 65, 70,
                        "Dragon Tail", "Aqua Tail", 1.12),

                new Arceus.PokemonData(
                        21, "Dragonair", "Dragon",
                        232, 84, 65, 70,
                        "Dragon Claw", "Wing Attack", 1.12),

                new Arceus.PokemonData(
                        22, "Aerodactyl", "Rock/Flying",
                        270, 105, 65, 130,
                        "Wing Attack", "Rock Slide", 1.12),

                new Arceus.PokemonData(
                        6, "Dragonite", "Dragon/Flying",
                        292, 134, 95, 80,
                        "Dragon Claw", "Wing Attack", 1.12)
        );
    }

    // Call once when starting a new gauntlet run.
    static List<List<Arceus.PokemonData>> createGauntlet() { //Outer list is for each elite four, inner is for their actual team of pokemons
        return List.of(
                enemyOne(), enemyTwo(), enemyThree(), enemyFour()
        );
    }
}
