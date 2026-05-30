package work.aemnet.memocraft.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.aemnet.memocraft.Memocraft;

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
        boolean previouslyHadMemo = plugin.getMemoRegistry().addMemo(player, message);
        if (previouslyHadMemo) {
            player.sendMessage(Component.text("Your memo has been moved here with the new message."));
        } else {
            player.sendMessage(Component.text("Your memo and player have been left for others. Run /memo again to move it."));
        }
        player.sendMessage(Component.text("TIP: /aka <name> sets a display name above your NPC."));
        return true;
    }
}
