package com.catfive.naga;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/** โมเดลพญานาคแบบก้อนสี่เหลี่ยม (voxel): หัวทอง ตาแดงเรืองแสง ผมสีตามเผ่า ลำตัวคดเคี้ยว */
public class NagaRenderer extends EntityRenderer<NagaBodyEntity> {
    private static final Identifier TEX = Identifier.of(NagaMod.ID, "textures/entity/white.png");
    private static final int[] GOLD = {222, 152, 28}, GOLD_D = {150, 95, 20}, BEIGE = {238, 222, 190}, EYE = {255, 30, 30};
    private static final float[] U = {0, 1, 1, 0}, V = {0, 0, 1, 1};
    private static final int FULL = 15728880;
    private static final float S = 1.5f;

    public NagaRenderer(EntityRendererFactory.Context ctx) { super(ctx); }
    @Override public Identifier getTexture(NagaBodyEntity e) { return TEX; }
    @Override public boolean shouldRender(NagaBodyEntity e, Frustum f, double x, double y, double z) { return true; }

    @Override
    public void render(NagaBodyEntity e, float yaw, float td, MatrixStack m, VertexConsumerProvider vp, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity owner = e.getWorld().getEntityById(e.getOwnerId());
        Vec3d base = e.getLerpedPos(td), pos = base, vel = Vec3d.ZERO;
        float hy = e.getYaw(), pitch = 0;
        boolean mine = false;
        if (owner != null) {
            pos = owner.getLerpedPos(td);
            hy = owner.getYaw(td);
            pitch = owner.getPitch(td);
            vel = owner.getPos().subtract(owner.prevX, owner.prevY, owner.prevZ);
            mine = owner == mc.player && mc.options.getPerspective().isFirstPerson();
        }
        int tribe = e.getTribe(), heads = e.getHeads();
        int[] col = NagaInfo.RGB[tribe];
        int[] dark = {col[0] / 5, col[1] / 5, col[2] / 5};
        float time = e.age + td;
        VertexConsumer solid = vp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
        VertexConsumer glow = vp.getBuffer(RenderLayer.getEyes(TEX));

        m.push();
        m.translate(pos.x - base.x, pos.y - base.y, pos.z - base.z);
        double yr = Math.toRadians(hy);
        Vec3d h = new Vec3d(-Math.sin(yr), 0, Math.cos(yr)), r = new Vec3d(Math.cos(yr), 0, Math.sin(yr));

        // ลำตัว
        for (int i = 1; i <= 38; i++) {
            double d = i * 0.62;
            double lat = Math.sin(time * 0.10 - i * 0.42) * (0.25 + i * 0.045);
            double x = -h.x * d + r.x * lat - vel.x * i * 0.9, z = -h.z * d + r.z * lat - vel.z * i * 0.9;
            double y = Math.max(0.4, 1.35 - i * 0.2) + Math.sin(time * 0.12 - i * 0.45) * 0.12 - vel.y * i * 0.5;
            float sz = Math.max(0.16f, 0.62f - i * 0.012f);
            m.push();
            m.translate(x, y, z);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-hy + (float) Math.sin(time * 0.10 - i * 0.42) * 12f));
            int[] c = i % 3 == 0 ? col : dark;
            box(m, solid, -sz, -sz * 0.6f, -0.34f, sz, sz * 0.7f, 0.34f, c, light);
            box(m, solid, -sz * 0.7f, -sz * 0.75f, -0.3f, sz * 0.7f, -sz * 0.55f, 0.3f, GOLD, light); // ท้องทอง
            float sp = 0.35f + 0.3f * (float) Math.abs(Math.sin(i * 1.3));
            m.push();
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25));
            box(m, solid, -0.05f, sz * 0.5f, -0.05f, 0.05f, sz * 0.5f + sp, 0.05f, i % 2 == 0 ? col : GOLD_D, light);
            m.pop();
            m.pop();
        }

        // หัว (ซ่อนในมุมมองบุคคลที่หนึ่งเพื่อไม่บังตา)
        if (!mine) {
            for (int k = 0; k < heads; k++) {
                int side = k == 0 ? 0 : (k % 2 == 1 ? 1 : -1) * ((k + 1) / 2);
                m.push();
                m.translate(r.x * side * 0.85, 1.55 - Math.abs(side) * 0.12 + Math.sin(time * 0.1 + k) * 0.05, r.z * side * 0.85);
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-hy - side * 12f));
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                m.scale(S, S, S);
                head(m, solid, glow, col, time + k * 7, light);
                m.pop();
            }
        }
        m.pop();
        super.render(e, yaw, td, m, vp, light);
    }

    private void head(MatrixStack m, VertexConsumer vc, VertexConsumer glow, int[] col, float time, int light) {
        box(m, vc, -0.3f, -0.2f, -0.3f, 0.3f, 0.25f, 0.3f, GOLD, light);          // กะโหลก
        box(m, vc, -0.22f, -0.18f, 0.3f, 0.22f, 0.1f, 0.95f, GOLD, light);        // จมูก/ปาก
        box(m, vc, -0.25f, -0.2f, 0.88f, 0.25f, 0.12f, 1.0f, GOLD_D, light);      // ปลายจมูก
        box(m, vc, -0.2f, -0.34f, 0.2f, 0.2f, -0.2f, 0.88f, GOLD_D, light);       // ขากรรไกรล่าง
        for (int k = 0; k < 5; k++) for (int s = -1; s <= 1; s += 2) {             // ฟัน
            float x = s * 0.18f, z = 0.38f + k * 0.11f;
            box(m, vc, x - 0.02f, -0.3f, z, x + 0.02f, -0.16f, z + 0.04f, BEIGE, light);
        }
        for (int s = -1; s <= 1; s += 2) {
            box(m, glow, s * 0.30f - 0.03f, 0.03f, 0.08f, s * 0.30f + 0.05f, 0.17f, 0.26f, EYE, FULL); // ตาเรืองแสง
            m.push();                                                               // หนวด
            m.translate(s * 0.2f, -0.1f, 0.9f);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(25));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s * 12f));
            box(m, vc, -0.015f, -0.015f, 0, 0.015f, 0.015f, 0.9f, BEIGE, light);
            m.pop();
            m.push();                                                               // เขางอกด้านข้าง
            m.translate(s * 0.25f, 0.2f, -0.1f);
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-s * 35f));
            box(m, vc, -0.05f, 0, -0.05f, 0.05f, 0.6f, 0.05f, GOLD, light);
            m.pop();
        }
        m.push();                                                                   // เขากลางสูง
        m.translate(0, 0.25f, -0.1f);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-18));
        box(m, vc, -0.08f, 0, -0.08f, 0.08f, 1.1f, 0.08f, GOLD, light);
        m.pop();
        for (int k = 0; k < 28; k++) {                                              // ผม/ครีบสีตามเผ่า
            float len = 0.8f + 0.7f * (float) Math.abs(Math.sin(k * 1.7));
            m.push();
            m.translate(0, 0.12f, -0.25f);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((k % 7 - 3) * 13f + (float) Math.sin(time * 0.2 + k) * 3f));
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees((k / 7 - 1.5f) * 15f));
            box(m, vc, -0.04f, -0.04f, -len, 0.04f, 0.04f, 0, k % 2 == 0 ? col : new int[]{col[0] / 2, col[1] / 2, col[2] / 2}, light);
            m.pop();
        }
    }

    private static void box(MatrixStack m, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int[] c, int light) {
        MatrixStack.Entry en = m.peek();
        quad(en, vc, new float[]{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1}, 0, 1, 0, 1.0f, c, light);
        quad(en, vc, new float[]{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, 0, -1, 0, 0.5f, c, light);
        quad(en, vc, new float[]{x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1}, 0, 0, 1, 0.85f, c, light);
        quad(en, vc, new float[]{x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0}, 0, 0, -1, 0.85f, c, light);
        quad(en, vc, new float[]{x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0}, 1, 0, 0, 0.7f, c, light);
        quad(en, vc, new float[]{x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0}, -1, 0, 0, 0.7f, c, light);
    }

    private static void quad(MatrixStack.Entry en, VertexConsumer vc, float[] p, float nx, float ny, float nz, float sh, int[] c, int light) {
        for (int i = 0; i < 4; i++)
            vc.vertex(en, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                .color((int) (c[0] * sh), (int) (c[1] * sh), (int) (c[2] * sh), 255)
                .texture(U[i], V[i]).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, nx, ny, nz);
    }
}
