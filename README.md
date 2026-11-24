# Simplekemon (Pokémon Gauntlet)

A single-player Java Swing turn-based desktop battle game backed by a MySQL database. Players draft an ordered team of six Pokémon from a 12-species catalog and challenge a sequential four-stage Elite Four gauntlet (Lorelei, Bruno, Agatha, and Lance). Completed runs are saved to MySQL and displayed in a persistent Hall of Fame leaderboard.

## Prerequisites

* **JDK Version 17 or newer**
* **Apache NetBeans 32** (for integrated Maven/build support)
* **MySQL Server** running locally or at a reachable host
* **Simplekémon.zip** extracted to a clean project folder

## Database Setup

1. Start **MySQL Server** and open your MySQL client (or terminal at the extracted project root).
2. Run `schema.sql` with an administrative account (`mysql -u root -p < schema.sql` or execute it directly inside your MySQL client connection).
3. Run `testuser.sql` (or execute its SQL statements) to create `'pokemon_app'@'localhost'` with `SELECT` and `INSERT` permissions on `pokemon_gauntlet.*`.
4. Check `Database.connect()` in `Database.java` to confirm `url` (`jdbc:mysql://localhost:3306/pokemon_gauntlet`), `username` (`pokemon_app`), and `password` (`pikamon`) match your MySQL account.

## Build and Run (Apache NetBeans)

1. Ensure the MySQL Connector/J dependency is configured on the project classpath/dependencies in **Apache NetBeans** (or resolved via the project's NetBeans Maven setup).
2. In **Apache NetBeans**, right-click the project and select **Clean and Build** (or **Build**).
   * *Verification:* The NetBeans Output window completes the build with `BUILD SUCCESS` and zero compilation errors.
3. To verify database connectivity first without playing a match, run `Database.main()` in NetBeans (**Run File** on `Database.java`).
   * *Verification:* The Output window prints that the MySQL connection, win count, and Hall of Fame query succeeded.
4. To launch the game, run `Pokemon.main()` in NetBeans (**Run File** on `Pokemon.java`).
   * *Verification:* The **Team Selection** window (`1280x700`) opens.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `'mvn' is not recognized...` in Command Prompt | Standalone Maven is not on Windows `PATH` | Build and run the project inside **Apache NetBeans** using its integrated build/Maven support instead of terminal `mvn` commands. |
| `Communications link failure` | MySQL is stopped or the URL/port is wrong | Start MySQL Server and check `Database.connect()`. |
| `Access denied for user` | Username or password does not match MySQL | Run `testuser.sql` or update `username` and `password` in `Database.connect()` to match your MySQL user. |
| `Table does not exist` | `schema.sql` was not run | Run `mysql -u root -p < schema.sql` (or execute `schema.sql` in your MySQL client). |
| Images do not load | Working directory or classpath resource path (`images/char#.png` or `/images/...`) does not match | Keep the `images` folder in the project working directory (or on the classpath) and run from NetBeans. |
| Victory is not confirmed saved | Database connection or insert failed | Check the log, correct MySQL/configuration, and click **Retry saving victory**. |
| Window does not open | No desktop display is available | Run the Swing UI on a standard desktop environment rather than a headless runner. |