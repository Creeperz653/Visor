package org.vmstudio.visor.mixin.client.renderer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.vmstudio.visor.api.client.ClientFeature;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.render.VRRenderPass;
import org.vmstudio.visor.api.client.settings.VRClientSettings;
import org.vmstudio.visor.api.client.settings.enums.MirrorMode;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorState;
import org.vmstudio.visor.core.client.player.VRAimPicker;
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
import org.vmstudio.visor.core.client.render.helpers.RenderPoseHelper;

import java.nio.file.Path;

// common mixin
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

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
    @Shadow
    private int itemActivationTicks;


    /* ****************** *\
  //--------AIM PICK--------\\
    \* ****************** */

    @WrapMethod(method = "pick(F)V")
    private void visor$pickWithVRHands(float partialTick, Operation<Void> original) {
        VRAimPicker.pickWithVRHands(() -> original.call(partialTick));
    }

    // 1.20.5 moved the ray trace into pick(Entity,DDF), pick(F)V has no Vec3 locals left
    //? if >=1.20.5 {
    @ModifyVariable(at = @At("STORE"), method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", ordinal = 0)
            //?} else {
    /*@ModifyVariable(at = @At("STORE"), method = "pick(F)V", ordinal = 0)
     *///?}
    public Vec3 visor$pickPos(Vec3 original) {
        return VRAimPicker.pickPos(original);
    }

    //? if >=1.20.5 {
    @ModifyVariable(at = @At("STORE"), method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", ordinal = 1)
            //?} else {
    /*@ModifyVariable(at = @At("STORE"), method = "pick(F)V", ordinal = 1)
     *///?}
    public Vec3 visor$pickDirection(Vec3 original) {
        return VRAimPicker.pickDirection(original);
    }

    //? if >=1.20.5 {
    @Redirect(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult visor$vrBlockPick(Entity entity, double range, float partialTick, boolean fluid) {
        HitResult vrHit = VRAimPicker.vrBlockPick();
        return vrHit != null ? vrHit : entity.pick(range, partialTick, fluid);
    }
    //?}


    /* ************************* *\
  //--------ITEM ACTIVATION--------\\
    \* ************************* */
    // item activation animation: skipped in the GUI pass, GameEffectVanilla draws it

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


    /* ********************* *\
  //--------VANILLA OFF--------\\
    \* ********************* */
    // vanilla GameRenderer behaviour turned off or gated in VR passes

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
