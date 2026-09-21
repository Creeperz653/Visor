package org.vmstudio.visor.loader.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.vmstudio.visor.core.client.render.BoardMode;

public class BoardModeCommand {

    private BoardModeCommand() {
        throw new UnsupportedOperationException("This is an utility class and cannot be instantiated");
    }

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("visorboard")
                        .executes(context -> {
                            BoardMode.toggle();
                            context.getSource().sendSuccess(
                                    () -> Component.literal(
                                            BoardMode.isActive()
                                                    ? "Board mode: ON"
                                                    : "Board mode: OFF"
                                    ),
                                    false
                            );
                            return 1;
                        })
        );
    }
}