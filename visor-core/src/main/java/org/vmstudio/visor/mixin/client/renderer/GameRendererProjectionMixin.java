package org.vmstudio.visor.mixin.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.phoenixra.atumvr.api.enums.EyeType;
import org.vmstudio.visor.api.client.render.VRRenderPass;
import org.vmstudio.visor.api.client.settings.VRClientSettings;
import org.vmstudio.visor.api.client.settings.enums.MirrorMode;
import org.vmstudio.visor.api.compatibility.mcversion.render.McModelViewStack;
import org.vmstudio.visor.api.compatibility.mcversion.render.McRenderUtils;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorState;
import org.vmstudio.visor.core.client.render.VRRenderState;
import org.vmstudio.visor.extensions.client.render.GameRendererExtension;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// projection per render pass (eye / third person / mixed reality), clip planes, FOV
@Mixin(GameRenderer.class)
public abstract class GameRendererProjectionMixin implements GameRendererExtension {

    // ---- Shadow fields ----
    @Shadow
    @Final
    Minecraft minecraft;
    @Shadow
    private float renderDistance;
    @Shadow
    private float zoom;
    @Shadow
    private float zoomX;
    @Shadow
    private float zoomY;
    @Shadow @Final
    private Camera mainCamera;

    // ---- Unique fields ----
    @Unique
    public Matrix4f visor$thirdPersonProjection = new Matrix4f();
    @Unique
    public float visor$nearClipPlane = 0.02F;
    @Unique
    private float visor$farClipPlane = 128.0F;

    // ---- Shadow methods ----
    @Shadow
    public abstract Matrix4f getProjectionMatrix(double fov);
    @Shadow
    protected abstract double getFov(Camera mainCamera2, float partialTicks, boolean b);
    @Shadow
    public abstract void resetProjectionMatrix(Matrix4f projectionMatrix);


    /* ***************** *\
  //--------MIXINS--------\\
    \* ***************** */

    @Inject(at = @At("HEAD"), method = "getFov(Lnet/minecraft/client/Camera;FZ)D", cancellable = true)
    public void visor$fov(Camera camera, float f, boolean bl, CallbackInfoReturnable<Double> info) {
        if (VisorState.get().isActive() && VRRenderState.getSceneType().isMainMenu()) {
            info.setReturnValue(Double.valueOf(this.minecraft.options.fov().get()));
        }
    }

    @Inject(at = @At("HEAD"), method = "getProjectionMatrix(D)Lorg/joml/Matrix4f;", cancellable = true)
    public void visor$projection(double d, CallbackInfoReturnable<Matrix4f> info) {
        if (VisorState.get().isNotActive()) {
            return;
        }
        PoseStack posestack = new PoseStack();
        visor$setupClipPlanes();
        ClientContext.renderer.updateProjection();

        VRRenderPass renderPass = VRRenderState.getRenderPass();
        if(renderPass == VRRenderPass.EYE_LEFT){
            McRenderUtils.mulPose(posestack,
                    ClientContext.renderer.getEyeProjection(EyeType.LEFT)
            );
            info.setReturnValue(
                    posestack.last().pose()
            );
            return;
        }
        if (renderPass == VRRenderPass.EYE_RIGHT) {
            McRenderUtils.mulPose(posestack,
                    ClientContext.renderer.getEyeProjection(EyeType.RIGHT)
            );
            info.setReturnValue(posestack.last().pose());
            return;
        }
        if (renderPass == VRRenderPass.THIRD_PERSON) {
            if (VRClientSettings.getMirrorMode() == MirrorMode.MIXED_REALITY) {
                McRenderUtils.mulPose(posestack,
                        new Matrix4f().setPerspective(
                                VRClientSettings.getMixedRealityFov() * Mth.DEG_TO_RAD,
                                VRClientSettings.getMixedRealityAspectRatio(), this.visor$nearClipPlane,
                                this.visor$farClipPlane
                        )
                );
            }else {
                McRenderUtils.mulPose(posestack,
                        new Matrix4f().setPerspective(
                                VRClientSettings.getThirdPersonFov() * Mth.DEG_TO_RAD,
                                (float) this.minecraft.getWindow().getScreenWidth()
                                        / (float) this.minecraft.getWindow().getScreenHeight(),
                                this.visor$nearClipPlane, this.visor$farClipPlane
                        )
                );
            }
            this.visor$thirdPersonProjection = new Matrix4f(posestack.last().pose());
            info.setReturnValue(posestack.last().pose());
            return;
        }

        if (this.zoom != 1.0F) {
            posestack.translate(this.zoomX, -this.zoomY, 0.0D);
            posestack.scale(this.zoom, this.zoom, 1.0F);
        }
        McRenderUtils.mulPose(posestack,
                new Matrix4f()
                        .setPerspective(
                                (float) d * Mth.DEG_TO_RAD,
                                (float) this.minecraft.getWindow().getScreenWidth()
                                        / (float) this.minecraft.getWindow().getScreenHeight(),
                                this.visor$nearClipPlane,
                                this.visor$farClipPlane
                        )
        );

        info.setReturnValue(posestack.last().pose());
    }

    //? if >=1.21 {
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;viewport(IIII)V", remap = false, shift = Shift.AFTER), method = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V")
    public void visor$matrix(DeltaTracker deltaTracker, boolean renderWorldIn, CallbackInfo info) {
    //?} else {
    /*@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;viewport(IIII)V", remap = false, shift = Shift.AFTER), method = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V")
    public void visor$matrix(float partialTicks, long nanoTime, boolean renderWorldIn, CallbackInfo info) {
    *///?}
        if(VisorState.get().isNotActive()) return;
        this.resetProjectionMatrix(
                this.getProjectionMatrix(
                        minecraft.options.fov().get()
                )
        );
        McModelViewStack.identity();
        RenderSystem.applyModelViewMatrix();
    }


    /* ************************ *\
  //--------PUBLIC METHODS--------\\
    \* ************************ */

    @Override
    @Unique
    public void visor$setupClipPlanes() {
        this.renderDistance = (float) (this.minecraft.options.getEffectiveRenderDistance() * 16);
        this.visor$farClipPlane = this.renderDistance + 1024.0F;
    }

    @Override
    @Unique
    public float visor$getNearClipPlane() {
        return this.visor$nearClipPlane;
    }

    @Override
    @Unique
    public float visor$getFarClipPlane() {
        return this.visor$farClipPlane;
    }

    @Override
    @Unique
    public void visor$resetProjectionMatrix(float partialTicks) {
        this.resetProjectionMatrix(this.getProjectionMatrix(this.getFov(this.mainCamera, partialTicks, true)));
    }

    @Override
    @Unique
    public Matrix4f visor$getThirdPersonProjection() {
        return visor$thirdPersonProjection;
    }
}
