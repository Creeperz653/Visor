package org.vmstudio.visor.mixin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import org.vmstudio.visor.api.client.ClientFeature;
import org.vmstudio.visor.api.client.render.VRRenderPass;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorState;
import org.vmstudio.visor.core.client.render.VRRenderState;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

// vanilla GameRenderer behaviour turned off or gated in VR passes
@Mixin(GameRenderer.class)
public abstract class GameRendererVanillaOffMixin {

    // ---- Shadow fields ----
    @Shadow @Final
    Minecraft minecraft;
    @Shadow
    private boolean renderHand;
    @Shadow
    private boolean effectActive;
    @Shadow
    private float fov;
    @Shadow
    private float oldFov;
    @Shadow
    private long lastActiveTime;


    /* ***************** *\
  //--------MIXINS--------\\
    \* ***************** */

    /**
     * If no crosshair rendered,
     * don't render block outline as well
     * @param cir
     */
    @Inject(at = @At("HEAD"), method = "shouldRenderBlockOutline", cancellable = true)
    public void visor$shouldDrawBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (VRRenderState.getPhase().isVRWorld()) {
            cir.setReturnValue(
                    ClientContext.visor.isFeatureEnabled(ClientFeature.AIM_EFFECTS)
            );
        }
    }

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;effectActive:Z"), method = "render")
    public boolean visor$noPostEffectOnThirdPerson(GameRenderer instance) {
        return this.effectActive && VRRenderState.getRenderPass() != VRRenderPass.THIRD_PERSON;
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWindowActive()Z"), method = "render")
    public boolean visor$noPauseGameIfWindowNotFocused(Minecraft instance) {
        return VisorState.get().isActive() || instance.isWindowActive();
    }

    @Inject(at = @At("HEAD"), method = "tickFov", cancellable = true)
    public void visor$freezeFovInVR(CallbackInfo ci) {
        if(VRRenderState.getPhase().isNotVanilla()) {
            // vanilla tickFov starts from when the view is not modified
            final float neutralFovModifier = 1.0F;
            this.fov = neutralFovModifier;
            this.oldFov = neutralFovModifier;
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "takeAutoScreenshot", cancellable = true)
    public void visor$skipAutoScreenshotInMenu(Path path, CallbackInfo ci) {
        if (VisorState.get().isActive() && VRRenderState.getSceneType().isMainMenu()) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "bobHurt", cancellable = true)
    public void visor$noBobHurt(PoseStack poseStack,
                                float f,
                                CallbackInfo ci) {
        if(VRRenderState.getPhase().isNotVanilla()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void visor$noBobView(PoseStack matrixStack,
                                float f,
                                CallbackInfo ci) {
        if(VRRenderState.getPhase().isNotVanilla()) {
            ci.cancel();
        }
    }

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z"), method = "renderLevel")
    public boolean visor$noVanillaHands(GameRenderer instance) {
        if (VRRenderState.isSpectatedVRView(minecraft.getCameraEntity())) {
            return false;
        }
        return VRRenderState.getPhase().isVanilla() && renderHand;
    }

    /**
     * Only process this when rendering vanilla
     * or VR camera that is a worldUpdater
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pauseGame(Z)V"), method = "render")
    public void visor$pauseOncePerFrame(Minecraft instance, boolean bl) {
        if (VisorState.get().isNotActive() || VRRenderState.getRenderPass() == VRRenderPass.worldUpdater()) {
            instance.pauseGame(bl);
        }
    }

    /**
     * Only process this when rendering vanilla
     * or VR camera that is a worldUpdater
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"), method = "render")
    public long visor$useActiveTimeOncePerFrame() {
        if (VisorState.get().isNotActive() || VRRenderState.getRenderPass() == VRRenderPass.worldUpdater()) {
            return Util.getMillis();
        } else {
            return this.lastActiveTime;
        }
    }
}
