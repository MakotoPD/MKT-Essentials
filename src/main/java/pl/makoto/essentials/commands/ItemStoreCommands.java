package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pl.makoto.essentials.util.ItemStorageManager;
import pl.makoto.essentials.util.ItemStorageManager.Category;
import pl.makoto.essentials.util.ItemStorageManager.StoredItem;
import pl.makoto.essentials.util.ItemStoreMenu;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.ArrayList;
import java.util.List;

/**
 * /itemstore — saved item storage: exact item copies grouped into categories, drawn at random
 * for crates and rewards, and handed out with a chat announcement carrying the item's tooltip.
 */
public class ItemStoreCommands {

    private static final String PERM_VIEW = "mktessentials.admin.itemstore.view";
    private static final String PERM_EDIT = "mktessentials.admin.itemstore.edit";
    private static final String PERM_GIVE = "mktessentials.admin.itemstore.give";

    private static final int PER_PAGE = 10;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("itemstore")
                .requires(source -> Permissions.hasPermission(source, PERM_VIEW, 2))
                .executes(context -> categories(context.getSource()))
                .then(Commands.literal("categories")
                        .executes(context -> categories(context.getSource())))
                .then(Commands.literal("create")
                        .requires(source -> Permissions.hasPermission(source, PERM_EDIT, 2))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .executes(context -> create(context.getSource(),
                                        StringArgumentType.getString(context, "category")))))
                .then(Commands.literal("delete")
                        .requires(source -> Permissions.hasPermission(source, PERM_EDIT, 2))
                        .then(categoryArg()
                                .executes(context -> delete(context))))
                .then(Commands.literal("rename")
                        .requires(source -> Permissions.hasPermission(source, PERM_EDIT, 2))
                        .then(categoryArg()
                                .then(Commands.argument("newName", StringArgumentType.word())
                                        .executes(context -> rename(context)))))
                .then(Commands.literal("edit")
                        .requires(source -> Permissions.hasPermission(source, PERM_EDIT, 2))
                        .then(categoryArg()
                                .executes(context -> edit(context))))
                .then(Commands.literal("list")
                        .then(categoryArg()
                                .executes(context -> list(context, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> list(context,
                                                IntegerArgumentType.getInteger(context, "page"))))))
                .then(Commands.literal("remove")
                        .requires(source -> Permissions.hasPermission(source, PERM_EDIT, 2))
                        .then(categoryArg()
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> remove(context)))))
                .then(Commands.literal("move")
                        .requires(source -> Permissions.hasPermission(source, PERM_EDIT, 2))
                        .then(categoryArg()
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("target", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        ItemStorageManager.getCategoryNames(), builder))
                                                .executes(context -> move(context))))))
                .then(Commands.literal("weight")
                        .requires(source -> Permissions.hasPermission(source, PERM_EDIT, 2))
                        .then(categoryArg()
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("weight", IntegerArgumentType.integer(0))
                                                .executes(context -> weight(context))))))
                .then(Commands.literal("announce")
                        .requires(source -> Permissions.hasPermission(source, PERM_EDIT, 2))
                        .then(categoryArg()
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> announce(context)))))
                .then(Commands.literal("give")
                        .requires(source -> Permissions.hasPermission(source, PERM_GIVE, 2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(categoryArg()
                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                .executes(context -> give(context, 0))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                        .executes(context -> give(context,
                                                                IntegerArgumentType.getInteger(context, "amount"))))))))
                .then(Commands.literal("giverandom")
                        .requires(source -> Permissions.hasPermission(source, PERM_GIVE, 2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(categoryArg()
                                        .executes(context -> giveRandom(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 54))
                                                .executes(context -> giveRandom(context,
                                                        IntegerArgumentType.getInteger(context, "count"))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> categoryArg() {
        return Commands.argument("category", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        ItemStorageManager.getCategoryNames(), builder));
    }

    /** Resolves the "category" argument, reporting the failure itself. */
    private static Category resolve(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "category");
        Category category = ItemStorageManager.get(name);
        if (category == null) {
            context.getSource().sendFailure(MessageUtils.prefixed("&cNo item category named &f" + name + "&c."));
        }
        return category;
    }

    // ── Categories ───────────────────────────────────────────────────────────

    private static int categories(CommandSourceStack source) {
        List<Category> all = ItemStorageManager.getCategories();
        if (all.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed(
                    "&7No item categories yet. Create one with &f/itemstore create <name>&7."), false);
            return 1;
        }

        source.sendSuccess(() -> MessageUtils.format("&8&m                &r &6&l Item Categories &r&8&m                "), false);
        for (Category category : all) {
            Component line = MessageUtils.format("&8• &6" + category.name + " &7— &f" + category.size()
                    + " &7item" + (category.size() == 1 ? "" : "s")
                    + (category.announce ? " &8(announced)" : "") + " ")
                    .append(runButton("&8[&bview&8]", "/itemstore list " + category.name))
                    .append(MessageUtils.format(" "))
                    .append(runButton("&8[&eedit&8]", "/itemstore edit " + category.name));
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int create(CommandSourceStack source, String name) {
        if (ItemStorageManager.create(name) == null) {
            source.sendFailure(MessageUtils.prefixed("&cCategory &f" + name + "&c already exists."));
            return 0;
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&aCreated category &6" + name
                + "&a. Fill it with &f/itemstore edit " + name + "&a."), true);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        Category category = resolve(context);
        if (category == null) return 0;
        int size = category.size();
        ItemStorageManager.delete(category.name);
        context.getSource().sendSuccess(() -> MessageUtils.prefixed("&aDeleted category &6" + category.name
                + "&a and its &f" + size + "&a item(s)."), true);
        return 1;
    }

    private static int rename(CommandContext<CommandSourceStack> context) {
        Category category = resolve(context);
        if (category == null) return 0;
        String newName = StringArgumentType.getString(context, "newName");
        String oldName = category.name;
        if (!ItemStorageManager.rename(oldName, newName)) {
            context.getSource().sendFailure(MessageUtils.prefixed("&cA category named &f" + newName + "&c already exists."));
            return 0;
        }
        context.getSource().sendSuccess(() -> MessageUtils.prefixed("&aRenamed &6" + oldName + " &ato &6" + newName + "&a."), true);
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(MessageUtils.prefixed("&cOnly a player can open the item editor."));
            return 0;
        }
        Category category = resolve(context);
        if (category == null) return 0;
        ItemStoreMenu.open(player, category);
        return 1;
    }

    private static int announce(CommandContext<CommandSourceStack> context) {
        Category category = resolve(context);
        if (category == null) return 0;
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ItemStorageManager.setAnnounce(category, enabled);
        context.getSource().sendSuccess(() -> MessageUtils.prefixed("&7Public announcements for &6"
                + category.name + " &7are now " + (enabled ? "&aon" : "&coff") + "&7."), true);
        return 1;
    }

    // ── Items ────────────────────────────────────────────────────────────────

    private static int list(CommandContext<CommandSourceStack> context, int page) {
        CommandSourceStack source = context.getSource();
        Category category = resolve(context);
        if (category == null) return 0;
        if (category.items.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7Category &6" + category.name
                    + " &7is empty. Fill it with &f/itemstore edit " + category.name + "&7."), false);
            return 1;
        }

        int totalWeight = category.items.stream().mapToInt(i -> Math.max(0, i.weight)).sum();
        int pages = (category.items.size() + PER_PAGE - 1) / PER_PAGE;
        final int current = Math.min(page, pages);
        int start = (current - 1) * PER_PAGE;
        int end = Math.min(category.items.size(), start + PER_PAGE);

        source.sendSuccess(() -> MessageUtils.format("&8&m                &r &6&l " + category.name + " &r&8&m                "), false);
        for (int i = start; i < end; i++) {
            StoredItem item = category.items.get(i);
            ItemStack stack = ItemStorageManager.toStack(item, source.registryAccess());
            int index = i + 1;
            String chance = totalWeight > 0
                    ? String.format(" &8(%.1f%%)", 100.0 * Math.max(0, item.weight) / totalWeight)
                    : " &8(never)";
            MutableComponent line = MessageUtils.format("&8" + index + ". &7");
            line.append(stack.isEmpty() ? MessageUtils.format("&c<unreadable item>") : stack.getDisplayName());
            line.append(MessageUtils.format(" &7x&f" + (stack.isEmpty() ? "?" : stack.getCount())
                    + " &7weight &f" + item.weight + chance));
            source.sendSuccess(() -> line, false);
        }
        source.sendSuccess(() -> footer(current, pages, "/itemstore list " + category.name), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        Category category = resolve(context);
        if (category == null) return 0;
        int index = IntegerArgumentType.getInteger(context, "index");
        if (ItemStorageManager.removeItem(category, index - 1) == null) {
            context.getSource().sendFailure(outOfRange(category, index));
            return 0;
        }
        context.getSource().sendSuccess(() -> MessageUtils.prefixed("&aRemoved item &f#" + index
                + "&a from &6" + category.name + "&a."), true);
        return 1;
    }

    private static int move(CommandContext<CommandSourceStack> context) {
        Category from = resolve(context);
        if (from == null) return 0;
        String targetName = StringArgumentType.getString(context, "target");
        Category to = ItemStorageManager.get(targetName);
        if (to == null) {
            context.getSource().sendFailure(MessageUtils.prefixed("&cNo item category named &f" + targetName + "&c."));
            return 0;
        }
        int index = IntegerArgumentType.getInteger(context, "index");
        if (index > from.size()) {
            context.getSource().sendFailure(outOfRange(from, index));
            return 0;
        }
        if (!ItemStorageManager.moveItem(from, index - 1, to)) {
            context.getSource().sendFailure(MessageUtils.prefixed("&cCategory &f" + to.name
                    + "&c is full (&f" + ItemStorageManager.MAX_ITEMS + "&c items)."));
            return 0;
        }
        context.getSource().sendSuccess(() -> MessageUtils.prefixed("&aMoved item &f#" + index
                + "&a from &6" + from.name + " &ato &6" + to.name + "&a."), true);
        return 1;
    }

    private static int weight(CommandContext<CommandSourceStack> context) {
        Category category = resolve(context);
        if (category == null) return 0;
        int index = IntegerArgumentType.getInteger(context, "index");
        int weight = IntegerArgumentType.getInteger(context, "weight");
        if (!ItemStorageManager.setWeight(category, index - 1, weight)) {
            context.getSource().sendFailure(outOfRange(category, index));
            return 0;
        }
        context.getSource().sendSuccess(() -> MessageUtils.prefixed("&7Item &f#" + index + " &7in &6"
                + category.name + " &7now has weight &f" + weight
                + (weight == 0 ? " &8(never drawn)" : "") + "&7."), true);
        return 1;
    }

    // ── Giving ───────────────────────────────────────────────────────────────

    private static int give(CommandContext<CommandSourceStack> context, int amountOverride)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        Category category = resolve(context);
        if (category == null) return 0;

        int index = IntegerArgumentType.getInteger(context, "index");
        if (index > category.size()) {
            context.getSource().sendFailure(outOfRange(category, index));
            return 0;
        }

        ItemStack stack = ItemStorageManager.toStack(category.items.get(index - 1), context.getSource().registryAccess());
        if (stack.isEmpty()) {
            context.getSource().sendFailure(MessageUtils.prefixed(
                    "&cItem &f#" + index + "&c could not be rebuilt — see the server console."));
            return 0;
        }
        if (amountOverride > 0) stack.setCount(amountOverride);

        deliver(context.getSource(), target, List.of(stack), category);
        return 1;
    }

    private static int giveRandom(CommandContext<CommandSourceStack> context, int count)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        Category category = resolve(context);
        if (category == null) return 0;

        List<StoredItem> drawn = ItemStorageManager.drawRandom(category, count);
        if (drawn.isEmpty()) {
            context.getSource().sendFailure(MessageUtils.prefixed("&cCategory &f" + category.name
                    + "&c has nothing that can be drawn (empty, or every weight is 0)."));
            return 0;
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (StoredItem item : drawn) {
            ItemStack stack = ItemStorageManager.toStack(item, context.getSource().registryAccess());
            if (!stack.isEmpty()) stacks.add(stack);
        }
        if (stacks.isEmpty()) {
            context.getSource().sendFailure(MessageUtils.prefixed(
                    "&cNone of the drawn items could be rebuilt — see the server console."));
            return 0;
        }

        deliver(context.getSource(), target, stacks, category);
        return 1;
    }

    /** Hands the stacks over, dropping what does not fit, and announces them if the category says so. */
    private static void deliver(CommandSourceStack source, ServerPlayer target,
                                List<ItemStack> stacks, Category category) {
        int dropped = 0;
        for (ItemStack stack : stacks) {
            ItemStack copy = stack.copy();
            if (!target.getInventory().add(copy)) {
                target.drop(copy, false);
                dropped++;
            }
            target.sendSystemMessage(MessageUtils.prefixed("&aYou received ")
                    .append(stack.getDisplayName())
                    .append(MessageUtils.format(stack.getCount() > 1 ? " &7x&f" + stack.getCount() : "")));
            if (category.announce) {
                broadcast(source, target, stack, category);
            }
        }

        final int droppedCount = dropped;
        source.sendSuccess(() -> MessageUtils.prefixed("&aGave &f" + stacks.size() + "&a item(s) from &6"
                + category.name + " &ato &6" + target.getScoreboardName()
                + (droppedCount > 0 ? " &7(&e" + droppedCount + " dropped — inventory full&7)" : "") + "&a."), true);
    }

    private static void broadcast(CommandSourceStack source, ServerPlayer target,
                                  ItemStack stack, Category category) {
        Component message = MessageUtils.prefixed("&6" + target.getScoreboardName() + " &7received ")
                .append(stack.getDisplayName())
                .append(MessageUtils.format(stack.getCount() > 1 ? " &7x&f" + stack.getCount() : ""))
                .append(MessageUtils.format(" &7from &f" + category.name + "&7!"));
        source.getServer().getPlayerList().broadcastSystemMessage(message, false);
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private static Component outOfRange(Category category, int index) {
        return MessageUtils.prefixed("&cCategory &f" + category.name + "&c has no item &f#" + index
                + "&c (it holds &f" + category.size() + "&c).");
    }

    private static Component footer(int page, int pages, String baseCommand) {
        MutableComponent line = Component.literal("");
        line.append(page > 1
                ? runButton("&a&l[<< Prev]", baseCommand + " " + (page - 1))
                : MessageUtils.format("&8&l[<< Prev]"));
        line.append(MessageUtils.format("  &7Page &f" + page + "&7/&f" + pages + "  "));
        line.append(page < pages
                ? runButton("&a&l[Next >>]", baseCommand + " " + (page + 1))
                : MessageUtils.format("&8&l[Next >>]"));
        return line;
    }

    private static MutableComponent runButton(String label, String command) {
        return MessageUtils.format(label)
                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }
}
