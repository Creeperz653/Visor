package org.vmstudio.visor.mixin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.render.VRRenderState;
import org.vmstudio.visor.extensions.client.render.GameRendererExtension;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// GameRenderer.render() GUI pass: cancelled in the VR world phase, VR main menu scene drawn instead
@Mixin(GameRenderer.class)
public abstract class GameRendererGuiPhaseMixin implements GameRendererExtension {

    // ---- Unique fields ----
    @Unique
    private boolean visor$isVRGuiVisible;


    /* ***************** *\
  //--------MIXINS--------\\
    \* ***************** */

    /**
     * Cancels GUI rendering for VRWorld stage and render VR main menu room
     */
    //? if >=1.21 {
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getWindow()Lcom/mojang/blaze3d/platform/Window;", ordinal = 6), method = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V", cancellable = true)
    public void visor$onRenderGUI(DeltaTracker deltaTracker, boolean renderWorldIn, CallbackInfo info) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
    //?} else {
    /*@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getWindow()Lcom/mojang/blaze3d/platform/Window;", ordinal = 6), method = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V", cancellable = true)
    public void visor$onRenderGUI(float partialTicks, long nanoTime, boolean renderWorldIn, CallbackInfo info) {
    *///?}

        if (VRRenderState.getPhase().isNotVRWorld()) {
            // Proceed rendering GUI for Vanilla and VRGui stage
            return;
        }

        info.cancel();


        // Render Main Menu View
        if (VRRenderState.getSceneType().isMainMenu()) {

            GL11.glDisable(GL11.GL_STENCIL_TEST);

            PoseStack poseStack = new PoseStack();
            //render VR main menu
            ClientContext.decorationRenderer.renderMainMenu(
                    poseStack,
                    partialTicks
            );
        }
    }

    /**
     * Draw GUI only after first level render
     */
    //? if >=1.21 {
    @ModifyVariable(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getWindow()Lcom/mojang/blaze3d/platform/Window;", shift = Shift.AFTER, ordinal = 6), method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", ordinal = 0, argsOnly = true)
    //?} else {
    /*@ModifyVariable(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getWindow()Lcom/mojang/blaze3d/platform/Window;", shift = Shift.AFTER, ordinal = 6), method = "render(FJZ)V", ordinal = 0, argsOnly = true)
    *///?}
    private boolean visor$vrGuiVisibility(boolean doRender) {
        if (VRRenderState.getPhase().isVanilla()) {
            return doRender;
        }
        return visor$isVRGuiVisible();
    }

    @Inject(at = @At("HEAD"), method = "renderConfusionOverlay", cancellable = true)
    private void visor$noConfusionOverlayInGUI(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        if (VRRenderState.getPhase().isVRGui()) {
            ci.cancel();
        }
    }


     /* ************************ *\
   //--------PUBLIC METHODS--------\\
     \* ************************ */

    @Override
    public boolean visor$isVRGuiVisible(){
        return visor$isVRGuiVisible;
    }

    @Override
    public void visor$setVRGuiVisible(boolean flag){
        visor$isVRGuiVisible = flag;
    }
}
