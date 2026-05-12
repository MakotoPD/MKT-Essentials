package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.config.I18n;

import java.util.Optional;

public class EnchantCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enchant")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.utility.enchant", 2))
                .then(Commands.argument("enchantment", ResourceKeyArgument.key(Registries.ENCHANTMENT))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                .executes(ctx -> enchant(ctx, IntegerArgumentType.getInteger(ctx, "level"))))));
    }

    @SuppressWarnings("unchecked")
    private static int enchant(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ItemStack item = player.getMainHandItem();
        if (item.isEmpty()) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("utility.enchant-empty-hand")));
            return 0;
        }

        // Get the ResourceKey from the argument directly
        ResourceKey<Enchantment> enchantmentKey = (ResourceKey<Enchantment>) (ResourceKey<?>) ctx.getArgument("enchantment", ResourceKey.class);

        // Resolve the enchantment holder from the server registry
        Registry<Enchantment> registry = source.getServer().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> holderOpt = registry.getHolder(enchantmentKey);

        if (holderOpt.isEmpty()) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("utility.enchant-invalid")));
            return 0;
        }

        Holder<Enchantment> holder = holderOpt.get();

        // Apply the enchantment to the item
        item.enchant(holder, level);

        String enchantName = enchantmentKey.location().toString();
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("utility.enchant-success", "enchantment", enchantName, "level", String.valueOf(level))), true);
        return 1;
    }
}
