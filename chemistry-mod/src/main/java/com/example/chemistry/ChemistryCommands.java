package com.example.chemistry;

import com.example.chemistry.api.ReactionUnlocks;
import com.example.chemistry.network.ChemistryNetworking;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /chemistry open|close：一键解锁/锁定化学手册的全部内容。 */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public final class ChemistryCommands {

    private ChemistryCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("chemistry")
                .then(Commands.literal("open")
                        .executes(ctx -> setAll(ctx, true)))
                .then(Commands.literal("close")
                        .executes(ctx -> setAll(ctx, false)))
                .then(Commands.literal("about")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() ->
                                    net.minecraft.network.chat.Component.literal(
                                            "MChemistry（化学时代） v" + ChemistryMod.VERSION
                                                    + " · 作者 " + ChemistryMod.AUTHOR
                                                    + " · 签名 " + ChemistryMod.SIGNATURE
                                                    + " · 校验码 FA-"
                                                    + ChemistryMod.signatureChecksum()),
                                    false);
                            return 1;
                        })));
    }

    private static int setAll(CommandContext<CommandSourceStack> ctx, boolean unlock)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (unlock) {
            ReactionUnlocks.unlockAll(player);
        } else {
            ReactionUnlocks.lockAll(player);
        }
        // 立即把最新状态同步给客户端；若手册已打开，下一帧会刷新。
        ChemistryNetworking.sendHandbookUnlockTo(player);
        return 1;
    }
}
