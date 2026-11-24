-- Run with a MySQL account allowed to create databases and tables.
-- Existing victory history is retained when this script is rerun.
CREATE DATABASE IF NOT EXISTS pokemon_gauntlet
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pokemon_gauntlet;

CREATE TABLE IF NOT EXISTS gauntlet_wins (
    run_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    won_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS winning_team (
    run_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    slot_number TINYINT NOT NULL,
    pokemon_id INT NOT NULL,
    pokemon_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (run_id, slot_number),
    CONSTRAINT fk_winning_team_run FOREIGN KEY (run_id)
        REFERENCES gauntlet_wins(run_id),
    CONSTRAINT chk_team_slot CHECK (slot_number BETWEEN 1 AND 6)
) ENGINE=InnoDB;

-- No separate counter can drift: each committed run increments this count by one.
CREATE OR REPLACE VIEW gauntlet_totals AS
    SELECT COUNT(*) AS wins FROM gauntlet_wins;

-- Useful queries:
-- SELECT wins FROM gauntlet_totals;
-- SELECT w.run_id, w.won_at, t.slot_number, t.pokemon_name
-- FROM gauntlet_wins w JOIN winning_team t ON w.run_id = t.run_id
-- ORDER BY w.won_at DESC, w.run_id, t.slot_number;
