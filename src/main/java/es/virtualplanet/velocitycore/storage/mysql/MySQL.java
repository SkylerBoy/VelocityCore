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
import java.util.concurrent.TimeUnit;

public class MySQL {

    protected final VelocityCorePlugin plugin;
    private static HikariDataSource dataSource;

    public MySQL(VelocityCorePlugin plugin) {
        this.plugin = plugin;

        configureHikariPool();

        try {
            try(Connection connection = getConnection()){
                Statement statement = connection.createStatement();;

                statement.executeUpdate(Statements.USERS_TABLE.toString());
                statement.executeUpdate(Statements.STAFF_TABLE.toString());
                //statement.executeUpdate(Statements.DISCORD_TABLE.toString());
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
                statement.setString(1, user.getUniqueId().toString());
                ResultSet resultSet = statement.executeQuery();

                if(resultSet.next()) {
                    // User data.
                    user.setId(resultSet.getInt("id"));
                    user.setUniqueId(UUID.fromString(resultSet.getString("uuid")));
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
                exception.printStackTrace();
            }
        });
    }

    private void registerUser(User user) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()){
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `users` (" +
                        "username, uuid, first_join, last_server, discord_id) VALUES " +
                        "(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

                statement.setString(1, user.getName());
                statement.setString(2, user.getUniqueId().toString());

                statement.setTimestamp(3, Timestamp.from(Instant.now()));
                statement.setString(4, null);
                statement.setLong(5, 0L);
                statement.executeUpdate();

                ResultSet resultSet = statement.getGeneratedKeys();

                if(resultSet.next()) {
                    user.setId(resultSet.getInt(1));
                }

                statement.close();
                user.setFirstJoin(Timestamp.from(Instant.now()));

                plugin.getServer().getEventManager().fire(new UserDataLoadEvent(user));
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }


    public void saveUserDataSync(User user) {
        try (Connection connection = getConnection()) {

            // PlayerData.
            PreparedStatement statement = connection.prepareStatement("UPDATE `users` SET " +
                    "username = ?, last_server = ?, discord_id = ? WHERE id = ? LIMIT 1");

            statement.setString(1, user.getName());
            statement.setString(2, user.getLastServer());
            statement.setLong(3, user.getDiscordId());
            statement.setInt(4, user.getId());

            statement.executeUpdate();
            statement.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void userExists(UUID uniqueId, Callback<Boolean> callback) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM `users` WHERE uuid = ? LIMIT 1");

                statement.setString(1, uniqueId.toString());
                callback.call(statement.executeQuery().next());

                statement.close();
            } catch (SQLException exception){
                exception.printStackTrace();
            }
        });
    }

    public void getStaffData(StaffPlayer staffPlayer) {
        staffExists(staffPlayer.getUniqueId(), (exists) -> {
            if(exists) {
                loadStaffData(staffPlayer);
                return;
            }

            registerStaff(staffPlayer);
        });
    }

    private void loadStaffData(StaffPlayer staffPlayer) {
        CompletableFuture.runAsync(() -> {
            try(Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM staff_data WHERE uuid = ? LIMIT 1");
                statement.setString(1, staffPlayer.getUniqueId().toString());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    staffPlayer.setId(resultSet.getInt("id"));
                    staffPlayer.setUniqueId(UUID.fromString(resultSet.getString("uuid")));
                    staffPlayer.setName(resultSet.getString("username"));
                    staffPlayer.setPassword(resultSet.getString("password"));
                    staffPlayer.setStaffChatEnabled(resultSet.getBoolean("staffmode"));

                    plugin.getServer().getEventManager().fire(new StaffDataLoadEvent(staffPlayer));
                }

                statement.close();
                resultSet.close();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void registerStaff(StaffPlayer staffPlayer) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()){
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `staff_data` (" +
                        "username, uuid, password, staffmode) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

                String password = Utils.generateCode(4, true, false);
                String hashedPassword = Utils.sha256(password);

                statement.setString(1, staffPlayer.getName());
                statement.setString(2, staffPlayer.getUniqueId().toString());
                statement.setString(3, hashedPassword);
                statement.setBoolean(4, false);
                statement.executeUpdate();

                ResultSet resultSet = statement.getGeneratedKeys();

                if(resultSet.next()) {
                    staffPlayer.setId(resultSet.getInt(1));
                }

                statement.close();

                staffPlayer.setPassword(hashedPassword);
                plugin.getServer().getEventManager().fire(new StaffDataLoadEvent(staffPlayer));

                plugin.getServer().getScheduler().buildTask(plugin, () ->
                        plugin.getServer().getPlayer(staffPlayer.getUniqueId()).ifPresent(player -> {
                            player.sendMessage(Component.text("Se ha generado una contraseña de staff temporal.").color(NamedTextColor.YELLOW));
                            player.sendMessage(Component.text("Puedes cambiarla usando: ").color(NamedTextColor.WHITE).append(Component.text("/setpass " + password + " <nueva>").color(NamedTextColor.GOLD)));
                })).delay(1, TimeUnit.SECONDS).schedule();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void saveStaffDataSync(StaffPlayer staffPlayer) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `staff_data` SET username = ?, password = ?, staffmode = ? WHERE id = ? LIMIT 1");

            statement.setString(1, staffPlayer.getName());
            statement.setString(2, staffPlayer.getPassword());
            statement.setBoolean(3, staffPlayer.isStaffChatEnabled());
            statement.setInt(4, staffPlayer.getId());

            statement.executeUpdate();
            statement.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void staffExists(UUID uniqueId, Callback<Boolean> callback) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM `staff_data` WHERE uuid = ? LIMIT 1");

                statement.setString(1, uniqueId.toString());
                callback.call(statement.executeQuery().next());

                statement.close();
            } catch (SQLException exception){
                exception.printStackTrace();
            }
        });
    }

    public long getDiscordId(UUID uniqueId) {
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
            exception.printStackTrace();
        }

        return 0L;
    }


    /*
    protected final VelocityCorePlugin plugin = VelocityCorePlugin.getInstance();

    public abstract Connection getConnection() throws SQLException;

    public abstract void closeConnection();
    public abstract void refreshConnection();


    public void loadStaffPlayer(UUID uniqueId) {
        StaffPlayer staffPlayer = loadPlayerData(uniqueId);

        if (staffPlayer == null) {
            return;
        }

        plugin.getStaffManager().getStaffList().put(uniqueId, staffPlayer);
    }

    private StaffPlayer loadPlayerData(UUID uniqueId) {
        // set a temporal random name
        StaffPlayer staffPlayer = new StaffPlayer(uniqueId);
        String uuidString = uniqueId.toString();

        try (Connection connection = getConnection()) {
            boolean loaded = false;

            try (PreparedStatement st = connection.prepareStatement(Statements.LOAD_STAFF.getStatement("staff_data"))) {
                st.setString(1, uuidString);

                try (ResultSet result = st.executeQuery()) {
                    if (result.next()) {
                        loadStaffData(result, staffPlayer);
                        loaded = true;
                    }
                }
            }

            if (!loaded) {
                return staffPlayer;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warn("Hubo un error al cargar los datos de {}: {}",  uuidString, exception.getMessage());
        }

        return staffPlayer;
    }

    private void loadStaffData(ResultSet r, StaffPlayer staffPlayer) throws SQLException {
        staffPlayer.setPassword(r.getString("password"));
        staffPlayer.setStaffMode(r.getBoolean("staffmode"));
    }

    public void savePlayer(StaffPlayer staffPlayer) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement st = connection.prepareStatement(Statements.SAVE_STAFF_DATA.getStatement("staff_data"))) {
                st.setString(1, staffPlayer.getUniqueId().toString());
                st.setString(2, staffPlayer.getPassword());
                st.setBoolean(3, staffPlayer.isStaffMode());

                st.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void savePlayerAsync(UUID uniqueId, StaffPlayer staffPlayer) {
        CompletableFuture.runAsync(() -> savePlayer(staffPlayer));
        plugin.getStaffManager().getStaffList().remove(uniqueId, staffPlayer);
    }
     */
}
