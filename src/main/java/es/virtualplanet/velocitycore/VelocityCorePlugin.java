package es.virtualplanet.velocitycore;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import es.virtualplanet.velocitycore.command.CommandManager;
import es.virtualplanet.velocitycore.config.MainConfig;
import es.virtualplanet.velocitycore.config.ResourceConfigManager;
import es.virtualplanet.velocitycore.discord.DiscordManager;
import es.virtualplanet.velocitycore.listener.PlayerListener;
import es.virtualplanet.velocitycore.listener.ServerListener;
import es.virtualplanet.velocitycore.punish.PunishManager;
import es.virtualplanet.velocitycore.scheduler.GeneralTask;
import es.virtualplanet.velocitycore.storage.ServerCache;
import es.virtualplanet.velocitycore.storage.mysql.MySQL;
import es.virtualplanet.velocitycore.storage.redis.Messager;
import es.virtualplanet.velocitycore.storage.redis.RedisManager;
import es.virtualplanet.velocitycore.user.UserManager;
import es.virtualplanet.velocitycore.user.staff.StaffManager;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
@Plugin(
        id = "velocitycore",
        name = "VelocityCore",
        version = "1.0.0",
        authors = {
                "VirtualPlanetNT",
                "Skyler_Boy"
        },
        dependencies = {
            @Dependency(id = "litebans"),
            @Dependency(id = "luckperms")
        })
public class VelocityCorePlugin {

    @Getter
    private static VelocityCorePlugin instance;

    private DiscordManager discordManager;

    private UserManager userManager;
    private StaffManager staffManager;
    private PunishManager punishManager;

    private MySQL mySQL;
    private ServerCache serverCache;

    private ExecutorService executorService;
    private RedisManager redisManager;
    private Messager messager;

    private final ProxyServer server;
    private final Logger logger;

    private MainConfig config;

    private LuckPerms luckPerms;

    @Inject
    public VelocityCorePlugin(ProxyServer server, Logger logger) {
        instance = this;

        this.server = server;
        this.logger = logger;

        logger.info("VelocityCore has been loaded!");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Load configuration file.
        Path configFile = Path.of("plugins/velocitycore/config.yml");
        ResourceConfigManager<MainConfig> configManager = ResourceConfigManager.create(configFile, "config.yml", MainConfig.class);

        configManager.reloadConfig();
        config = configManager.getConfigData();

        // Initialize databases & cache.
        mySQL = new MySQL(this);
        serverCache = new ServerCache();

        executorService = Executors.newFixedThreadPool(10);
        redisManager = new RedisManager(this, executorService);
        messager = getRedisManager();

        // Initialize managers.
        discordManager = new DiscordManager(this);
        userManager = new UserManager(this);
        staffManager = new StaffManager(this);
        punishManager = new PunishManager(this);

        luckPerms = LuckPermsProvider.get();

        // Register listeners.
        EventManager eventManager = server.getEventManager();
        eventManager.register(this, new PlayerListener(this));
        eventManager.register(this, new ServerListener(this));

        //Register commands.
        CommandManager commandManager = new CommandManager(this);
        commandManager.registerCommands();

        // Check available servers.
        messager.sendMessage("bungee:check-servers", "", "");
        logger.info("VelocityCore has been initialized!");

        // Initialize tasks.
        getServer().getScheduler().buildTask(this, new GeneralTask(this)).repeat(1000, TimeUnit.MILLISECONDS).schedule();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        getMySQL().closeConnection();
        logger.info("Connection with MySQL has been closed.");

        getRedisManager().stop();
        logger.info("Connection with Redis has been closed.");
    }
}
