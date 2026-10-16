package dev.e.shopscanner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

public class ShopScannerClient implements ClientModInitializer {
    public static final int RADIUS = 60;
    static KeyBinding scanKey, openKey;
    static final KeyBinding.Category CATEGORY =
        KeyBinding.Category.create(Identifier.of("shopscanner", "main"));

    @Override
    public void onInitializeClient() {
        ShopStore.load();
        scanKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.shopscanner.scan", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.shopscanner.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (scanKey.wasPressed()) scan(mc);
            while (openKey.wasPressed()) mc.setScreen(new ShopScreen());
        });
    }

    public static void scan(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        BlockPos p = mc.player.getBlockPos();
        String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
        String dim = mc.world.getRegistryKey().getValue().toString();
        int cr = RADIUS / 16 + 1, pcx = p.getX() >> 4, pcz = p.getZ() >> 4;
        int signs = 0, shops = 0, fresh = 0;

        for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
            for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
                WorldChunk ch = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (ch == null) continue;
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    if (!(be instanceof SignBlockEntity sign)) continue;
                    if (be.getPos().getSquaredDistance(p) > RADIUS * RADIUS) continue;
                    signs++;
                    Shop s = ShopParser.parse(sign, server, dim);
                    if (s == null) continue;
                    shops++;
                    if (ShopStore.put(s)) fresh++;
                }
            }
        }
        ShopStore.save();
        mc.player.sendMessage(Text.literal("Shop Scanner: " + signs + " signs, " + shops
            + " shops (" + fresh + " new). Press K to browse."), true);
    }
}
