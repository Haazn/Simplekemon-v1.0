CREATE USER 'pokemon_app'@'localhost'
IDENTIFIED BY 'pikamon';

GRANT SELECT, INSERT
ON pokemon_gauntlet.*
TO 'pokemon_app'@'localhost';