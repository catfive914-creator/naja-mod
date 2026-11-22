package com.catfive.naga;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class NagaClient implements ClientModInitializer {
    private static KeyBinding key(String n, int k) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.catfive_naga." + n, InputUtil.Type.KEYSYM, k, "key.categories.catfive_naga"));
    }

    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(NagaMod.BODY, NagaRenderer::new);

        KeyBinding g = key("transform", GLFW.GLFW_KEY_G);
        KeyBinding[] sk = {key("skill1", GLFW.GLFW_KEY_Z), key("skill2", GLFW.GLFW_KEY_X), key("skill3", GLFW.GLFW_KEY_C), key("skill4", GLFW.GLFW_KEY_V)};
        KeyBinding r = key("ult", GLFW.GLFW_KEY_R);
        KeyBinding n = key("menu", GLFW.GLFW_KEY_N);

        ClientPlayNetworking.registerGlobalReceiver(Net.Sync.ID, (p, ctx) -> {
            ClientState.tribe = p.tribe();
            ClientState.level = p.level();
            ClientState.transformed = p.transformed();
            // เริ่มเกม ยังไม่มีเผ่า -> เปิดหน้าต่างสุ่มทันที
            if (p.tribe() < 0 && ctx.client().player != null && ctx.client().currentScreen == null)
                ctx.client().setScreen(new NagaScreen());
        });

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (c.player == null) return;
            while (g.wasPressed()) ClientPlayNetworking.send(new Net.Action(1));
            for (int i = 0; i < 4; i++) while (sk[i].wasPressed()) ClientPlayNetworking.send(new Net.Action(2 + i));
            while (r.wasPressed()) ClientPlayNetworking.send(new Net.Action(6));
            while (n.wasPressed()) c.setScreen(new NagaScreen());
        });
    }
}
