package work.aemnet.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.aemnet.Memocraft;

public class MemoCommand implements CommandExecutor {
    private final Memocraft plugin;

    public MemoCommand(Memocraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;

        if (args.length == 0) {
            player.sendMessage(Component.text("Please provide a message to leave. Usage: /memo <message>"));
            return false;
        }

        String message = String.join(" ", args);
        plugin.getMemoRegistry().addMemo(player, message);
        player.sendMessage(Component.text("Your memo and player have been left for others. Use this command again to change your position and message."));
        player.sendMessage(Component.text("TIP: You can also use /aka <username> to display an old username above your head."));
        return true;
    }
}
