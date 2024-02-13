package es.virtualplanet.velocitycore.command;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.command.list.*;

public class CommandManager {

    private final VelocityCorePlugin plugin;

    public CommandManager(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        registerCommand("registerstaff", new RegisterStaffCommand(plugin), "rs", "regstaff");
        registerCommand("deletestaff", new DeleteStaffCommand(plugin), "dl", "delstaff");
        registerCommand("pass", new PassCommand(plugin));
        registerCommand("setpass", new SetPassCommand(plugin));
        registerCommand("staffchat", new StaffChatCommand(plugin), "sc");
        registerCommand("discord", new DiscordCommand(plugin), "discordlink", "discordverify", "verify");
        registerCommand("stream", new StreamCommand(plugin), "directo");
    }

    private void registerCommand(String name, SimpleCommand command, String... aliases) {
        com.velocitypowered.api.command.CommandManager commandManager = plugin.getServer().getCommandManager();

        if (aliases.length == 0) {
            commandManager.register(name, command);
        } else {
            CommandMeta meta = commandManager.metaBuilder(name).aliases(aliases).plugin(plugin).build();
            commandManager.register(meta, command);
        }
    }
}
