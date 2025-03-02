package dmvmc.distMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class UpdateConfigCommandTabCompleter implements TabCompleter {

    private final DistMap plugin;
    public UpdateConfigCommandTabCompleter(DistMap plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {

        // Check sender permission
        if (!sender.hasPermission("distmap.update")) {
            return Collections.emptyList();
        }

        // Return existing keys
        if (args.length == 1) {
            return plugin.getConfig().getKeys(false).stream()
                    .filter(property -> property.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Return existing value
        if (args.length == 2)
            return Collections.singletonList(plugin.getConfig().getString(args[0]));

        return Collections.emptyList();

    }

}
