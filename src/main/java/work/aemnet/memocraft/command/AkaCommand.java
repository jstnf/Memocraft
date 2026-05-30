package work.aemnet.memocraft.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.aemnet.memocraft.Memocraft;

public class AkaCommand implements CommandExecutor {
    private final Memocraft plugin;

    public AkaCommand(Memocraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;

        if (args.length == 0) {
            player.sendMessage(Component.text("Please provide a username to set as your AKA. Usage: /aka <username>"));
            return false;
        }

        String akaUsername = args[0];
        plugin.getMemoRegistry().setAkaUsername(player, akaUsername);
        player.sendMessage(Component.text("You set your AKA username as: " + akaUsername));
        return true;
    }
}
