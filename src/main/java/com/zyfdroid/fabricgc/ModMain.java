package com.zyfdroid.fabricgc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;

public class ModMain implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("gc").requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(3)).executes(this::cmdGc));
        }));
        new GcInvokeThread().start();

    }

    static class GcInvokeThread extends Thread {

        public GcInvokeThread() {
            super("GCInvokerThread");
            setDaemon(true);
        }

        private static void delay(int sec) throws InterruptedException {
            Thread.sleep(sec * 1000);
        }

        @Override
        public void run() {
            try {
                delay(15);
                while (true) {
                    doGc(System.out::println);
                    delay(600);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private int cmdGc(CommandContext<ServerCommandSource> ctx) {
        doGc(str -> ctx.getSource().sendFeedback(Text.of(str), true));
        return 1;
    }

    public static void doGc(PrintCallback pcb) {
        Runtime r = Runtime.getRuntime();
        long before = r.totalMemory() - r.freeMemory();
        long begin = System.currentTimeMillis();
        pcb.print("=============Begin GC==============");
        r.gc();
        long end = System.currentTimeMillis();
        long after = r.totalMemory() - r.freeMemory();
        pcb.print("GC take " + (end - begin) + "ms,");
        pcb.print("Ram: " + (Math.round((before) / 104857.6d) / 10d) + " MB -> " + (Math.round((after) / 104857.6d) / 10d)+" MB");
        pcb.print("Free " + (Math.round((before - after) / 104857.6d) / 10d) + " MB Memory");
        pcb.print("============= End GC ==============");
    }

    static interface PrintCallback {
        void print(String str);
    }
}
