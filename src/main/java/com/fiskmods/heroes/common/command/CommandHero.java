package com.fiskmods.heroes.common.command;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /hero list|null|<hero>} — hero management commands. {@code /hero <hero>} equips the suit,
 * {@code /hero null} removes every suit piece.
 */
public class CommandHero
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("hero")
                .executes(context -> info(context.getSource().getPlayerOrException()))
                .then(Commands.literal("list").executes(context ->
                {
                    CommandSourceStack source = context.getSource();
                    source.sendSuccess(() -> Component.translatable("command.fiskheroes.hero.list", Hero.REGISTRY.size()), false);

                    for (Hero hero : Hero.REGISTRY.getSortedHeroes())
                    {
                        source.sendSuccess(() -> Component.literal(" - ").append(hero.getFormattedName())
                                .append(Component.literal(" (" + hero.getName() + ")")), false);
                    }

                    return Hero.REGISTRY.size();
                }))
                .then(Commands.literal("null").requires(s -> s.hasPermission(2)).executes(context ->
                {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    clear(player);
                    context.getSource().sendSuccess(() -> Component.translatable("command.fiskheroes.hero.cleared"), true);
                    return 1;
                }))
                .then(Commands.literal("equip").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("hero", StringArgumentType.string())
                                .suggests(CommandSuit.SUGGEST_HEROES)
                                .executes(context ->
                                {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String key = StringArgumentType.getString(context, "hero");
                                    Hero hero = Hero.REGISTRY.getHeroIgnoreCase(key);

                                    if (hero == null)
                                    {
                                        context.getSource().sendFailure(Component.translatable("command.fiskheroes.suit.unknown", key));
                                        return 0;
                                    }

                                    CommandSuit.equip(player, hero.getDefaultIteration());
                                    context.getSource().sendSuccess(() -> Component.translatable("command.fiskheroes.suit.success", hero.getFormattedName()), true);
                                    return 1;
                                })));
    }

    private static int info(ServerPlayer player)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;

        if (iteration == null)
        {
            player.sendSystemMessage(Component.translatable("command.fiskheroes.hero.none"));
        }
        else
        {
            player.sendSystemMessage(Component.translatable("command.fiskheroes.hero.current", iteration.getFormattedName()));
        }

        return 1;
    }

    private static void clear(ServerPlayer player)
    {
        for (int i = 0; i < 4; ++i)
        {
            player.setItemSlot(CommandSuit.slotFor(i), net.minecraft.world.item.ItemStack.EMPTY);
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data != null)
        {
            data.reset();
        }
    }
}
