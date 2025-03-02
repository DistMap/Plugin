package dmvmc.distMap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class UpdateConfigCommand implements CommandExecutor {

    private final DistMap plugin;
    public UpdateConfigCommand(DistMap plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {

        // Check permission to set mob multiplier
        if (!sender.hasPermission("distmap.update")) {
            sender.sendMessage(Component.text()
                    .content("You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Ensure correct usage
        if (args.length != 2) {
            sender.sendMessage("Usage: /updateconfig <property> <value>");
            return true;
        }

        // Update config
        plugin.getConfig().set(args[0], args[1]);
        plugin.saveConfig();
        plugin.restartUpdateTask();
        return true;

    }

}
