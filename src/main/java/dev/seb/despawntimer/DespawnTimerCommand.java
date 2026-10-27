package dev.seb.despawntimer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.List;

final class DespawnTimerCommand implements CommandExecutor, TabCompleter {
    private final UniversalDespawnTimerPlugin plugin;

    DespawnTimerCommand(UniversalDespawnTimerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "UniversalDespawnTimer reloaded.");
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(ChatColor.RED + "Reload failed: " + exception.getMessage());
            }
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.GOLD + "UniversalDespawnTimer " + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.GRAY + "LifeStealZ: "
                    + (plugin.isLifeStealConnected() ? ChatColor.GREEN : ChatColor.YELLOW)
                    + plugin.getLifeStealStatus());
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <reload|status>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        return List.of("reload", "status").stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
