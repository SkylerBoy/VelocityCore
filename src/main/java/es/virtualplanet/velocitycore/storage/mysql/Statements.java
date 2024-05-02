package es.virtualplanet.velocitycore.storage.mysql;

public enum Statements {

    USERS_TABLE("CREATE TABLE IF NOT EXISTS `users` (" +
            "`id` INT NOT NULL AUTO_INCREMENT, " +
            "`username` VARCHAR(20) NOT NULL, " +
            "`uuid` BINARY(16) NOT NULL, " +
            "`first_join` TIMESTAMP, " +
            "`last_server` VARCHAR(20), " +
            "PRIMARY KEY (`id`), " +
            "UNIQUE (uuid));"),

    STAFF_TABLE("CREATE TABLE IF NOT EXISTS `staff_data` (" +
            "`id` INT NOT NULL AUTO_INCREMENT, " +
            "`username` VARCHAR(20) NOT NULL, " +
            "`uuid` BINARY(16) NOT NULL, " +
            "`password` VARCHAR(64), " +
            "`staff_mode` TINYINT(1), " +
            "PRIMARY KEY (`id`), " +
            "UNIQUE (uuid));"),

    DISCORD_TABLE("CREATE TABLE IF NOT EXISTS `discord_data` (" +
            "`id` INT NOT NULL AUTO_INCREMENT, " +
            "`discord_id` VARCHAR(36) NOT NULL, " +
            "`user_id` INT NOT NULL, " +
            "`date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "PRIMARY KEY (`id`), " +
            "FOREIGN KEY (`user_id`) REFERENCES users(`id`));");

    private final String statement;

    Statements(String statement) {
        this.statement = statement;
    }

    @Override
    public String toString() {
        return statement;
    }
}
