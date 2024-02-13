package es.virtualplanet.velocitycore.storage.mysql;

public enum Statements {

    USERS_TABLE("CREATE TABLE IF NOT EXISTS `users` (" +
            "`id` INT NOT NULL AUTO_INCREMENT, " +
            "`username` VARCHAR(20) NOT NULL, " +
            "`uuid` BINARY(16) NOT NULL, " +
            "`first_join` TIMESTAMP, " +
            "`last_server` VARCHAR(20), " +
            "`discord_id` LONG NOT NULL, " +
            "PRIMARY KEY (`id`), " +
            "UNIQUE (uuid));"),

    // TODO - Tomar la UUID de la tabla de usuarios.
    STAFF_TABLE("CREATE TABLE IF NOT EXISTS `staff_data` (" +
            "`id` INT NOT NULL AUTO_INCREMENT, " +
            "`username` VARCHAR(20) NOT NULL, " +
            "`uuid` BINARY(16) NOT NULL, " +
            "`password` VARCHAR(64), " +
            "`staff_mode` TINYINT(1), " +
            "PRIMARY KEY (`id`), " +
            "UNIQUE (uuid));");

    /*DISCORD_TABLE("CREATE TABLE IF NOT EXISTS `discord_data` (" +
            "`id` INT NOT NULL AUTO_INCREMENT, " +
            "`user_id` VARCHAR(20) NOT NULL, " +
            "`discord_id` VARCHAR(36) NOT NULL, " +
            "`mc_uuid` VARCHAR(54) NOT NULL, " +
            "`mc_name` VARCHAR(20) NOT NULL, " +
            "`date` TIMESTAMP, " +
            "PRIMARY KEY (`id`), " +
            "FOREIGN KEY (`user_id`) REFERENCES users(`id`), " +
            "FOREIGN KEY (`mc_uuid`) REFERENCES users(`uuid`), " +
            "FOREIGN KEY (`mc_name`) REFERENCES users(`username`));");*/

    private final String statement;

    Statements(String statement) {
        this.statement = statement;
    }

    @Override
    public String toString() {
        return statement;
    }
}
