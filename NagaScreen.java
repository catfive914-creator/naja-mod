package com.catfive.naga;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** หน้าต่าง N: สุ่มเผ่า (วงล้อหมุน) และดูเลเวล/เศียร/สกิล */
public class NagaScreen extends Screen {
    private int spin = -1; // -1 ไม่หมุน, >=0 กำลังหมุน
    private ButtonWidget btn;

    public NagaScreen() { super(Text.literal("ระบบพญานาค")); }

    @Override protected void init() {
        btn = ButtonWidget.builder(Text.literal("สุ่มเผ่า"), b -> {
            if (spin < 0 && ClientState.tribe < 0) { spin = 0; ClientPlayNetworking.send(new Net.Action(0)); }
        }).dimensions(width / 2 - 50, height / 2 + 65, 100, 20).build();
        addDrawableChild(btn);
    }

    @Override public void tick() {
        if (spin >= 0) {
            spin++;
            btn.active = false;
            if (spin >= 50 && ClientState.tribe >= 0) spin = -1;
        }
        btn.visible = ClientState.tribe < 0;
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void render(DrawContext g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int cx = width / 2, cy = height / 2 - 20;
        g.drawCenteredTextWithShadow(textRenderer, Text.literal("ระบบพญานาค"), cx, 12, 0xFFD700);

        int hi = spin >= 0 ? (spin / 2) % 4 : ClientState.tribe;
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 - Math.PI / 2;
            int x = cx + (int) (Math.cos(a) * 70), y = cy + (int) (Math.sin(a) * 38);
            g.fill(x - 30, y - 12, x + 30, y + 12, NagaInfo.COLOR[i]);
            if (i == hi) g.drawBorder(x - 32, y - 14, 64, 28, 0xFFFFFFFF);
            g.drawCenteredTextWithShadow(textRenderer, Text.literal("เผ่า" + NagaInfo.TRIBE[i]), x, y - 4, 0xFFFFFFFF);
        }
        g.drawCenteredTextWithShadow(textRenderer, Text.literal(spin >= 0 ? "กำลังสุ่ม..." : "วงล้อพญานาค"), cx, cy - 4, 0xFFFFFF);

        if (ClientState.tribe >= 0 && spin < 0) {
            int t = ClientState.tribe, y = cy + 62;
            g.drawCenteredTextWithShadow(textRenderer, Text.literal("คุณได้รับพญานาคเผ่า" + NagaInfo.TRIBE[t] + "!"), cx, y, NagaInfo.COLOR[t]);
            g.drawCenteredTextWithShadow(textRenderer, Text.literal("เลเวล " + ClientState.level + "/100   |   " + NagaState.headsFor(ClientState.level) + " เศียร"), cx, y + 12, 0xFFFFFF);
            for (int i = 0; i < 4; i++)
                g.drawCenteredTextWithShadow(textRenderer, Text.literal("[" + NagaInfo.KEYS[i] + "] " + NagaInfo.SKILL[t][i]), cx, y + 26 + i * 10, 0xDDDDDD);
            g.drawCenteredTextWithShadow(textRenderer, Text.literal("[R] อันติ: " + NagaInfo.ULT[t]), cx, y + 66, 0xFFAA00);
            g.drawCenteredTextWithShadow(textRenderer, Text.literal("[G] แปลงร่าง / คืนร่าง   (เลเวล 20/40/60/80/100 = 2/3/4/7/9 เศียร)"), cx, y + 78, 0x99CCFF);
        }
    }
}
