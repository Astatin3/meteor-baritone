package com.example.addon.commands;

import com.example.addon.modules.BaseFinder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

/**
 * The Meteor Client command API uses the <a href="https://github.com/Mojang/brigadier">same command system as Minecraft does</a>.
 */
public class CommandExample extends Command {
    /**
     * The {@code name} parameter should be in kebab-case.
     */

    BaseFinder baseFinder;

    public CommandExample(BaseFinder basefinder) {
        super("example", "Sends a message.");
        this.baseFinder = basefinder;
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("Running generator");
            baseFinder.generate();
            return SINGLE_SUCCESS;
        });

        builder.then(literal("name").then(argument("nameArgument", StringArgumentType.word()).executes(context -> {
            String argument = StringArgumentType.getString(context, "nameArgument");
            info("hi, " + argument);
            return SINGLE_SUCCESS;
        })));
    }
}
