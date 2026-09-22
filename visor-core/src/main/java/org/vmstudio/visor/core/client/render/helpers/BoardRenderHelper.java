package org.vmstudio.visor.core.client.render.helpers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL11;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.render.VRRenderPass;
import org.vmstudio.visor.api.compatibility.mcversion.render.McProjection;
import org.vmstudio.visor.api.compatibility.mcversion.render.McVertexBuilder;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.player.pose.LocalPlayerPose;
import org.vmstudio.visor.core.client.render.BoardMode;

/**
 * Draws the desk-board's clip volume into the stencil buffer, then leaves
 * the stencil test set up so the following world render only writes color
 * inside that volume. Everywhere else keeps whatever the frame was cleared
 * to (alpha 0), so the passthrough underlay shows through.
 * <p>
 *     World scale / reposition of the actual rendered geometry (steps 3/12
 *     of the original plan) are NOT handled here yet — this only clips
 *     visibility. The world still renders at normal scale; the box just
 *     limits where it's allowed to draw.
 * </p>
 */
public class BoardRenderHelper {

    /** Half-extent of the box, in blocks. 1.0 = a 2x2x2 block cube. */
    private static final float BOARD_HALF_SIZE = 0.5F;
    /** Distance in front of the captured origin's facing direction, in blocks. */
    private static final float BOARD_FORWARD_OFFSET = 1.0F;
    /** Distance below the captured origin's eye height, in blocks. */
    private static final float BOARD_DOWN_OFFSET = 1.0F;

    private BoardRenderHelper() {
        throw new UnsupportedOperationException("This is an utility class and cannot be instantiated");
    }

    /**
     * If board mode is active, writes the box volume into the stencil
     * buffer and switches the stencil test to only pass where that box
     * is. Call once per eye, after the frame clear and before the world
     * render. Must be paired with {@link #endMask()} after the world
     * render completes.
     *
     * @return true if a mask was applied (caller should call
     * {@link #endMask()} after rendering the world), false if board mode
     * is inactive and nothing was changed.
     */
    public static boolean beginMask(VRRenderPass renderPass) {
        if (!BoardMode.isActive() || !renderPass.isEye()) {
            return false;
        }

        var projectionState = McProjection.save();
        McProjection.setPerspective(ClientContext.renderer.getEyeProjection(renderPass.getEyeOrLeft()));

        PoseStack poseStack = buildBoardPoseStack(renderPass);

        // --- write the box into the stencil buffer only ---
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glColorMask(false, false, false, false);
        GL11.glDepthMask(false);
        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        RenderSystem.disableCull();
        renderBoxFaces(poseStack.last().pose());
        RenderSystem.enableCull();

        // --- restore normal writes, constrain by the mask we just wrote ---
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);
        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        McProjection.restore(projectionState);
        return true;
    }

    /** Disables the stencil test after the masked world render is done. */
    public static void endMask() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static PoseStack buildBoardPoseStack(VRRenderPass renderPass) {
        PoseStack poseStack = new PoseStack();
        RenderPoseHelper.applyCameraPose(renderPass, poseStack);

        LocalPlayerPose renderPose = ClientContext.localPlayer.getPoseData(PlayerPoseType.RENDER);
        Vector3fc eye = RenderPoseHelper.getCameraPosition(renderPass, renderPose);
        Vector3f origin = BoardMode.getOrigin();

        poseStack.translate(
                origin.x() - eye.x(),
                origin.y() - eye.y(),
                origin.z() - eye.z()
        );
        poseStack.mulPose(Axis.YP.rotation(BoardMode.getOriginYaw()));
        // NOTE: if the box appears behind the player instead of in front,
        // flip the sign of BOARD_FORWARD_OFFSET below.
        poseStack.translate(0.0, -BOARD_DOWN_OFFSET, BOARD_FORWARD_OFFSET);

        return poseStack;
    }

    private static void renderBoxFaces(Matrix4f matrix) {
        McVertexBuilder buf = McVertexBuilder.get();
        VertexFormat format = DefaultVertexFormat.POSITION;
        float s = BOARD_HALF_SIZE;

        buf.begin(VertexFormat.Mode.QUADS, format);

        // -X face
        quad(buf, matrix, -s, -s, -s,  -s, -s,  s,  -s,  s,  s,  -s,  s, -s);
        // +X face
        quad(buf, matrix,  s, -s,  s,   s, -s, -s,   s,  s, -s,   s,  s,  s);
        // -Y face (bottom)
        quad(buf, matrix, -s, -s, -s,   s, -s, -s,   s, -s,  s,  -s, -s,  s);
        // +Y face (top)
        quad(buf, matrix, -s,  s,  s,   s,  s,  s,   s,  s, -s,  -s,  s, -s);
        // -Z face
        quad(buf, matrix,  s, -s, -s,  -s, -s, -s,  -s,  s, -s,   s,  s, -s);
        // +Z face
        quad(buf, matrix, -s, -s,  s,   s, -s,  s,   s,  s,  s,  -s,  s,  s);

        buf.drawNoShader();
    }

    private static void quad(McVertexBuilder buf, Matrix4f matrix,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3) {
        buf.vertex(matrix, x0, y0, z0).endVertex();
        buf.vertex(matrix, x1, y1, z1).endVertex();
        buf.vertex(matrix, x2, y2, z2).endVertex();
        buf.vertex(matrix, x3, y3, z3).endVertex();
    }
}
