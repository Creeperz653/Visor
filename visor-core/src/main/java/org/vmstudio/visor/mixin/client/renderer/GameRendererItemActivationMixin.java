package org.vmstudio.visor.mixin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.render.VRRenderPass;
import org.vmstudio.visor.api.client.settings.VRClientSettings;
import org.vmstudio.visor.api.client.settings.enums.MirrorMode;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.render.VRRenderState;
import org.vmstudio.visor.core.client.render.helpers.RenderPoseHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// item activation (totem) animation: skipped in the GUI pass, re-posed at the VR camera when GameEffectVanilla draws it
@Mixin(GameRenderer.class)
public abstract class GameRendererItemActivationMixin {

    // ---- Shadow fields ----
    @Shadow
    private int itemActivationTicks;


    /* ***************** *\
  //--------MIXINS--------\\
    \* ***************** */

    //? if >=1.21 {
    @Shadow
    public abstract void renderItemActivationAnimation(GuiGraphics guiGraphics, float par1);
    //?} else {
    /*@Shadow
    public abstract void renderItemActivationAnimation(int i, int j, float par1);
    *///?}

    @Redirect(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    //? if >=1.21 {
    private void visor$skipActivationScale(PoseStack poseStack, float x, float y, float z,
                                           GuiGraphics guiGraphics, float partialTicks
    ) {
    //?} else {
    /*private void visor$skipActivationScale(PoseStack poseStack, float x, float y, float z, int width, int height,
                                   float partialTicks
    ) {
    *///?}
        if (VRRenderState.getPhase().isVanilla()) {
            poseStack.scale(x, y, z);
            return;
        }
        VRRenderPass currentCamera = VRRenderState.getRenderPass();
        var cameraPose = ClientContext.localPlayer.getPoseData(PlayerPoseType.RENDER).getCameraPose(currentCamera);

        float time = (40 - this.itemActivationTicks + partialTicks) / 40.0f;
        float t2 = time * time;
        float t3 = time * t2;
        float curve = 10.25f * t3 * t2 - 24.95f * t2 * t2 + 25.5f * t3 - 13.8f * t2 + 4.0f * time;
        float popScale = 0.5F * Mth.sin(curve * Mth.PI);

        poseStack.translate(0, 0, popScale - 1.0F);
        if (currentCamera == VRRenderPass.THIRD_PERSON) {
            float fov = VRClientSettings.getMirrorMode() == MirrorMode.MIXED_REALITY
                    ? VRClientSettings.getMixedRealityFov()
                    : VRClientSettings.getThirdPersonFov();
            popScale *= fov / 70.0F;
        }
        RenderPoseHelper.applyCameraPose(currentCamera, poseStack);
        poseStack.scale(popScale, popScale, popScale);
        poseStack.mulPose(Axis.YP.rotation(-cameraPose.getYaw()));
        poseStack.mulPose(Axis.XP.rotation(-cameraPose.getPitch()));
    }

    //? if >=1.21 {
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemActivationAnimation(Lnet/minecraft/client/gui/GuiGraphics;F)V"), method = "render(Lnet/minecraft/client/DeltaTracker;Z)V")
    private void visor$noItemActivationAnimInGUI(GameRenderer instance, GuiGraphics guiGraphics, float f) {
        if(VRRenderState.getPhase().isVanilla()) {
            renderItemActivationAnimation(guiGraphics, f);
        }
    }
    //?} else {
    /*@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemActivationAnimation(IIF)V"), method = "render(FJZ)V")
    private void visor$noItemActivationAnimInGUI(GameRenderer instance, int i, int j, float f) {
        if(VRRenderState.getPhase().isVanilla()) {
            renderItemActivationAnimation(i, j, f);
        }
    }
    *///?}

    @Redirect(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void visor$noItemTranslate(PoseStack poseStack, float x, float y, float z) {
        if(VRRenderState.getPhase().isVanilla()) {
            poseStack.translate(x, y, z);
        }
    }
}
