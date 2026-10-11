package com.fiskmods.heroes.common.command;

import java.util.ArrayList;
import java.util.List;

import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /suit <hero> [iteration]} — equips a hero's suit on the executing player.
 */
public class CommandSuit
{
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_HEROES = (context, builder) ->
    {
        List<String> names = new ArrayList<>();

        for (Hero hero : Hero.REGISTRY.getSortedHeroes())
        {
            names.add(hero.getRegistryName().toString());
            names.add(hero.getRegistryName().getPath());
        }

        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_ITERATIONS = (context, builder) ->
    {
        List<String> names = new ArrayList<>();

        try
        {
            String key = StringArgumentType.getString(context, "hero");
            Hero hero = Hero.REGISTRY.getHeroIgnoreCase(key);

            if (hero != null)
            {
                for (String iteration : hero.getIterations().values().stream().map(HeroIteration::getKey).filter(k -> k != null).toList())
                {
                    names.add(iteration);
                }
            }
        }
        catch (Exception e)
        {
            // No hero argument yet
        }

        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("suit")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("hero", StringArgumentType.string())
                        .suggests(SUGGEST_HEROES)
                        .executes(context -> suit(context, null))
                        .then(Commands.argument("iteration", StringArgumentType.string())
                                .suggests(SUGGEST_ITERATIONS)
                                .executes(context -> suit(context, StringArgumentType.getString(context, "iteration"))))));
    }

    private static int suit(CommandContext<CommandSourceStack> context, String iterationKey) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, "hero");
        Hero hero = Hero.REGISTRY.getHeroIgnoreCase(key);

        if (hero == null)
        {
            context.getSource().sendFailure(Component.translatable("command.fiskheroes.suit.unknown", key));
            return 0;
        }

        HeroIteration iteration = iterationKey != null ? hero.getIteration(iterationKey) : hero.getDefaultIteration();
        equip(player, iteration);
        context.getSource().sendSuccess(() -> Component.translatable("command.fiskheroes.suit.success", iteration.getFormattedName()), true);
        return 1;
    }

    /** Puts the suit pieces of the given iteration on the player's armour slots. */
    public static void equip(ServerPlayer player, HeroIteration iteration)
    {
        for (int i = 0; i < 4; ++i)
        {
            if (iteration.getArmorType(i) != null)
            {
                ItemStack stack = ItemHeroArmor.create(iteration, ModItems.heroArmor[i]);
                player.setItemSlot(slotFor(i), stack);
            }
        }
    }

    public static EquipmentSlot slotFor(int index)
    {
        return switch (index)
        {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            default -> EquipmentSlot.FEET;
        };
    }
}
