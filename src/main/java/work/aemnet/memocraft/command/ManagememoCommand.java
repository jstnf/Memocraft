package work.aemnet.memocraft.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import work.aemnet.memocraft.Memocraft;

public class ManagememoCommand implements CommandExecutor {
    private final Memocraft plugin;

    public ManagememoCommand(Memocraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return false;
    }
}
