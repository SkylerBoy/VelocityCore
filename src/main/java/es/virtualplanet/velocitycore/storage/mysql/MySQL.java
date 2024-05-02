package es.virtualplanet.velocitycore.storage.mysql;

import com.zaxxer.hikari.HikariDataSource;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.common.Utils;
import es.virtualplanet.velocitycore.config.MainConfig;
import es.virtualplanet.velocitycore.listener.event.staff.StaffDataLoadEvent;
import es.virtualplanet.velocitycore.listener.event.user.UserDataLoadEvent;
import es.virtualplanet.velocitycore.storage.Callback;
import es.virtualplanet.velocitycore.user.User;
import es.virtualplanet.velocitycore.user.staff.StaffPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQL {

    protected final VelocityCorePlugin plugin;
    private static HikariDataSource dataSource;

    public MySQL(VelocityCorePlugin plugin) {
        this.plugin = plugin;

        configureHikariPool();

        try {
            try(Connection connection = getConnection()){
                for (Statements statement : Statements.values()) {
                    connection.prepareStatement(statement.toString()).execute();
                }
            }

            plugin.getLogger().info("MySQL inicializada correctamente.");
        } catch (SQLException exception) {
            plugin.getLogger().warn("Un error ha ocurrido al iniciar la conexion con MySQL: ", exception);
        }
    }

    private synchronized void configureHikariPool() {
        MainConfig.MySQLInfo mySQL = plugin.getConfig().getMySQL();
        dataSource = new HikariDataSource();

        dataSource.setPoolName("VelocityCore Database Pool");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl("jdbc:mysql://" + mySQL.getHost() + ":" + mySQL.getPort() + "/" + mySQL.getDatabase() + "?useSSL=false");
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource.addDataSourceProperty("characterEncoding", "utf8");
        dataSource.addDataSourceProperty("encoding", "UTF-8");
        dataSource.addDataSourceProperty("useUnicode", "true");
        dataSource.setUsername(mySQL.getUser());
        dataSource.setPassword(mySQL.getPassword());
        dataSource.setMaxLifetime(180000L);
        dataSource.setIdleTimeout(60000L);
        dataSource.setMinimumIdle(5);
        dataSource.setConnectionTimeout(30000);
        dataSource.setMaximumPoolSize(15);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection() {
        if (dataSource == null){
            return;
        }

        dataSource.close();
        dataSource = null;
    }

    public void refreshConnection() {
        if (dataSource != null) {
            dataSource.close();
        }

        configureHikariPool();
    }

    public void getUserData(User user) {
        userExists(user.getUniqueId(), (exists) -> {
            if(exists) {
                loadUserData(user);
                return;
            }

            registerUser(user);
        });
    }

    private void loadUserData(User user) {
        CompletableFuture.runAsync(() -> {
            try(Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE uuid = ? LIMIT 1");
                statement.setBytes(1, Utils.convertUUIDToBytes(user.getUniqueId()));
                ResultSet resultSet = statement.executeQuery();

                if(resultSet.next()) {
                    // User data.
                    user.setId(resultSet.getInt("id"));
                    user.setUniqueId(Utils.convertBytesToUUID(resultSet.getBytes("uuid")));
                    user.setName(resultSet.getString("username"));
                    user.setFirstJoin(resultSet.getTimestamp("first_join"));
                    user.setLastServer(resultSet.getString("last_server"));
                    user.setDiscordId(resultSet.getLong("discord_id"));

                    statement.close();
                    resultSet.close();

                    // Load user data.
                    plugin.getServer().getEventManager().fire(new UserDataLoadEvent(user));
                }

            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private void registerUser(User user) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()){
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `users` (" +
                        "username, uuid, first_join, last_server) VALUES " +
                        "(?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

                statement.setString(1, user.getName());
                statement.setBytes(2, Utils.convertUUIDToBytes(user.getUniqueId()));

                statement.setTimestamp(3, Timestamp.from(Instant.now()));
                statement.setString(4, null);
                statement.executeUpdate();

                ResultSet resultSet = statement.getGeneratedKeys();

                if(resultSet.next()) {
                    user.setId(resultSet.getInt(1));
                }

                statement.close();
                user.setFirstJoin(Timestamp.from(Instant.now()));

                plugin.getServer().getEventManager().fire(new UserDataLoadEvent(user));
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }


    public void saveUserDataSync(User user) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `users` SET username = ?, last_server = ? WHERE id = ? LIMIT 1");

            statement.setString(1, user.getName());
            statement.setString(2, user.getLastServer());
            statement.setInt(3, user.getId());

            statement.executeUpdate();
            statement.close();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void userExists(UUID uniqueId, Callback<Boolean> callback) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM `users` WHERE uuid = ? LIMIT 1");

                statement.setBytes(1, Utils.convertUUIDToBytes(uniqueId));
                callback.call(statement.executeQuery().next());

                statement.close();
            } catch (SQLException exception){
                throw new RuntimeException(exception);
            }
        });
    }

    public void getStaffData(StaffPlayer staffPlayer) {
        staffExists(staffPlayer.getUniqueId(), (exists) -> {
            if(!exists) {
                return;
            }

            loadStaffData(staffPlayer);
        });
    }

    private void loadStaffData(StaffPlayer staffPlayer) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT id, password, staff_mode FROM staff_data WHERE uuid = ? LIMIT 1");
                statement.setBytes(1, Utils.convertUUIDToBytes(staffPlayer.getUniqueId()));
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    staffPlayer.setId(resultSet.getInt("id"));
                    staffPlayer.setPassword(resultSet.getString("password"));
                    staffPlayer.setStaffChatEnabled(resultSet.getBoolean("staff_mode"));

                    plugin.getStaffManager().getStaffList().put(staffPlayer.getUniqueId(), staffPlayer);
                    plugin.getServer().getEventManager().fire(new StaffDataLoadEvent(staffPlayer));
                }

                statement.close();
                resultSet.close();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public void registerStaffPlayer(StaffPlayer staffPlayer) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()){
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `staff_data` (" +
                        "username, uuid, password, staff_mode) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

                String password = Utils.generateCode(6, true, true);
                String hashedPassword = Utils.sha256(password);

                statement.setString(1, staffPlayer.getName());
                statement.setBytes(2, Utils.convertUUIDToBytes(staffPlayer.getUniqueId()));
                statement.setString(3, hashedPassword);
                statement.setBoolean(4, false);
                statement.executeUpdate();

                ResultSet resultSet = statement.getGeneratedKeys();

                if(resultSet.next()) {
                    staffPlayer.setId(resultSet.getInt(1));
                }

                statement.close();

                staffPlayer.setPassword(hashedPassword);

                staffPlayer.toPlayer().sendMessage(Component.text("Has sido registrado con una contraseña temporal.").color(NamedTextColor.WHITE));
                staffPlayer.toPlayer().sendMessage(Component.text("Puedes cambiarla usando: ").color(NamedTextColor.WHITE).append(Component.text("/setpass " + password + " <nueva>").color(NamedTextColor.YELLOW)));

                plugin.getStaffManager().getStaffList().put(staffPlayer.getUniqueId(), staffPlayer);
                plugin.getServer().getEventManager().fire(new StaffDataLoadEvent(staffPlayer));
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public void unregisterStaffPlayer(UUID uniqueId) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                   PreparedStatement statement = connection.prepareStatement("DELETE FROM `staff_data` WHERE uuid = ?");
                statement.setBytes(1, Utils.convertUUIDToBytes(uniqueId));

                statement.executeUpdate();
                statement.close();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public void unregisterStaffPlayer(String username) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM `staff_data` WHERE username = ?");
                statement.setString(1, username);

                statement.executeUpdate();
                statement.close();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public void saveStaffDataSync(StaffPlayer staffPlayer) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `staff_data` SET username = ?, password = ?, staff_mode = ? WHERE id = ? LIMIT 1");

            statement.setString(1, staffPlayer.getName());
            statement.setString(2, staffPlayer.getPassword());
            statement.setBoolean(3, staffPlayer.isStaffChatEnabled());
            statement.setInt(4, staffPlayer.getId());

            statement.executeUpdate();
            statement.close();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void staffExists(UUID uniqueId, Callback<Boolean> callback) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM `staff_data` WHERE uuid = ? LIMIT 1");

                statement.setBytes(1, Utils.convertUUIDToBytes(uniqueId));
                callback.call(statement.executeQuery().next());

                statement.close();
            } catch (SQLException exception){
                throw new RuntimeException(exception);
            }
        });
    }

    public long getDiscordId(UUID uniqueId) {
        /*
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT discord_id FROM `users` WHERE uuid = ? LIMIT 1");

            statement.setString(1, uniqueId.toString());
            ResultSet resultSet = statement.executeQuery();

            if(resultSet.next()) {
                return resultSet.getLong("discord_id");
            }

            statement.close();
            resultSet.close();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
         */

        return 0L;
    }

    public void updateDiscordId(User user, long discordId) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO discord_data (discord_id, user_id, date) VALUES (?,?,?)");

                statement.setLong(1, discordId);
                statement.setInt(2, user.getId());
                statement.setTimestamp(3, Timestamp.from(Instant.now()));

                statement.executeUpdate();
                statement.close();
            } catch (SQLException exception) {
                throw new RuntimeException("Error updating discord id for '" + user.getName() + "': " + exception.getMessage());
            }
        });
    }
}
