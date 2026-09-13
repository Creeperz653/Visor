package org.vmstudio.visor.mixin.client.renderer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.render.VRRenderPass;
import org.vmstudio.visor.api.common.player.VRPose;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.render.VRCameraEntityCache;
import org.vmstudio.visor.core.client.render.VRGameCamera;
import org.vmstudio.visor.core.client.render.VRRenderState;
import org.vmstudio.visor.core.client.render.helpers.RenderEffectsHelper;
import org.vmstudio.visor.core.client.render.helpers.RenderHelper;
import org.vmstudio.visor.core.client.render.helpers.RenderPoseHelper;
import org.vmstudio.visor.core.client.tasks.types.movement.TaskTeleport;
import org.vmstudio.visor.extensions.client.render.GameRendererExtension;
import net.minecraft.client.Camera;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import org.joml.Quaternionf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.vmstudio.visor.core.client.VisorClientImpl.MC;

@Mixin(GameRenderer.class)
public abstract class GameRendererCameraMixin implements GameRendererExtension {

    // ---- Unique fields ----
    @Shadow @Final
    Minecraft minecraft;

    // ---- Unique fields ----
    @Unique
    public VRCameraEntityCache visor$cameraEntityCache = new VRCameraEntityCache();
    @Unique
    private boolean visor$cameraEntityCached;
    @Unique
    private int visor$cameraEntityCacheDepth;

    @Unique
    public boolean visor$onfire;
    @Unique
    public boolean visor$inBlock = false;
    @Unique
    public float visor$blockProximity = 0.0f;

    // ---- Shadow methods ----
    @Shadow
    public abstract void pick(float f);


    /* ***************** *\
  //--------MIXINS--------\\
    \* ***************** */

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/client/Camera"))
    public Camera visor$useVRCamera() {
        return new VRGameCamera();
    }

    //? if >=1.21 {
    // 1.21 builds the frustum from Camera.rotation() instead of its euler angles
    @Redirect(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;rotation()Lorg/joml/Quaternionf;"),
            method = "renderLevel")
    public Quaternionf visor$noVanillaCameraRotation(Camera camera) {
        if (VRRenderState.getPhase().isVanilla()) {
            return camera.rotation();
        }
        return new Quaternionf();
    }
    //?} else {
    /*@Redirect(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getXRot()F"),
            method = "renderLevel")
    public float visor$noVanillaCameraPitch(Camera camera) {
        if (VRRenderState.getPhase().isVanilla()) {
            return camera.getXRot();
        }
        return 0F;
    }

    @Redirect(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getYRot()F"),
            method = "renderLevel")
    public float visor$noVanillaCameraYaw(Camera camera) {
        if (VRRenderState.getPhase().isVanilla()) {
            return camera.getYRot();
        }
        // -180 cancels the +180 vanilla
        return -180F;
    }
    *///?}

    //? if >=1.21 {
    @ModifyExpressionValue(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotation(Lorg/joml/Quaternionfc;)Lorg/joml/Matrix4f;", remap = false))
    public Matrix4f visor$orientCameraToPass(Matrix4f frustumMatrix) {
    //?} elif >=1.20.5 {
    /*@ModifyExpressionValue(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotationXYZ(FFF)Lorg/joml/Matrix4f;", remap = false))
    public Matrix4f visor$orientCameraToPass(Matrix4f frustumMatrix) {
    *///?}
    //? if >=1.20.5 {
        if (VRRenderState.getPhase().isNotVanilla()) {
            RenderPoseHelper.applyCameraOrientation(
                    VRRenderState.getRenderPass(), frustumMatrix
            );
        }
        return frustumMatrix;
    }
    //?} else {
    /*@Inject(at = @At(value = "NEW", target = "org/joml/Matrix3f", remap = false),
            method = "renderLevel")
    public void visor$orientCameraToPass(float partialTicks, long nanos, PoseStack poseStack, CallbackInfo ci) {
        if (VRRenderState.getPhase().isNotVanilla()) {
            RenderPoseHelper.applyCameraOrientation(
                    VRRenderState.getRenderPass(), poseStack
            );
        }
    }
    *///?}

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V"), method = "renderLevel")
    public void visor$pickAndSetupCamera(GameRenderer g, float pPartialTicks) {
        if (VRRenderState.getPhase().isVanilla()) {
            g.pick(pPartialTicks);
            return;
        }
        if (VRRenderState.getRenderPass() == VRRenderPass.worldUpdater()) {
            this.pick(pPartialTicks);

            if(MC.screen == null){
                TaskTeleport.updateTeleportDestination(MC.player);
            }
        }

        this.visor$cacheCameraEntity(this.minecraft.getCameraEntity());
        this.visor$setupCameraEntityAsVRCamera();
        this.visor$updateCameraOverlaps(pPartialTicks);
    }

    @Inject(at = @At(value = "TAIL"), method = "renderLevel")
    //? if >=1.21 {
    public void visor$restoreCamera(DeltaTracker deltaTracker, CallbackInfo i) {
    //?} elif >=1.20.5 {
    /*public void visor$restoreCamera(float f, long j, CallbackInfo i) {
    *///?} else {
    /*public void visor$restoreCamera(float f, long j, PoseStack p, CallbackInfo i) {
    *///?}
        if(VRRenderState.getPhase().isNotVanilla()) {
            this.visor$restoreCameraEntity(
                    this.minecraft.getCameraEntity()
            );
        }
    }

    @Inject(at = @At("TAIL"), method = "renderLevel")
    //? if >=1.21 {
    public void visor$releaseHiddenAreaMask(DeltaTracker deltaTracker, CallbackInfo ci) {
    //?} elif >=1.20.5 {
    /*public void visor$releaseHiddenAreaMask(float f, long l, CallbackInfo ci) {
    *///?} else {
    /*public void visor$releaseHiddenAreaMask(float f, long l, PoseStack poseStack, CallbackInfo ci) {
    *///?}
        if(VRRenderState.getPhase().isNotVanilla()) {
            RenderEffectsHelper.releaseHiddenAreaMask();
        }
    }


     /* ************************ *\
   //--------PUBLIC METHODS--------\\
     \* ************************ */

    @Override
    @Unique
    public void visor$setupCameraEntity(VRPose vrPose) {
        if (!this.visor$cameraEntityCached) {
            return;
        }
        var position = vrPose.getPosition();
        float x = position.x();
        float y = position.y();
        float z = position.z();

        LivingEntity cameraEntity = (LivingEntity) this.minecraft.getCameraEntity();
        cameraEntity.setPosRaw(x, y, z);
        cameraEntity.xo = cameraEntity.xOld = x;
        cameraEntity.yo = cameraEntity.yOld = y;
        cameraEntity.zo = cameraEntity.zOld = z;

        cameraEntity.setXRot(-vrPose.getPitchDegrees());
        cameraEntity.setYRot(vrPose.getYawDegrees());
        cameraEntity.xRotO = cameraEntity.getXRot();
        cameraEntity.yHeadRot = cameraEntity.getYRot();
        cameraEntity.yHeadRotO = cameraEntity.getYRot();

        // collapse the eye offset so the entity position is the pose position
        cameraEntity.eyeHeight = 0.0001F;
    }

    @Override
    @Unique
    public void visor$cacheCameraEntity(Entity cameraEntity) {
        if (this.minecraft.getCameraEntity() != null) {
            this.visor$cameraEntityCacheDepth++;
            if (!this.visor$cameraEntityCached) {
                LivingEntity livingEntity = cameraEntity instanceof LivingEntity ent ? ent : null;
                visor$cameraEntityCache = new VRCameraEntityCache(
                        cameraEntity.getX(), cameraEntity.getY(),
                        cameraEntity.getZ(),

                        cameraEntity.xOld, cameraEntity.yOld,
                        cameraEntity.zOld,

                        cameraEntity.xo, cameraEntity.yo,
                        cameraEntity.zo,

                        livingEntity != null ? livingEntity.yHeadRot : cameraEntity.getYRot(),
                        cameraEntity.getXRot(),

                        livingEntity != null ? livingEntity.yHeadRotO : cameraEntity.yRotO,
                        cameraEntity.xRotO,

                        cameraEntity.getEyeHeight()
                );
                this.visor$cameraEntityCached = true;
            }
        }
    }

    @Override
    @Unique
    public void visor$restoreCameraEntity(Entity cameraEntity) {
        if (this.visor$cameraEntityCacheDepth > 0) {
            this.visor$cameraEntityCacheDepth--;
        }
        if (cameraEntity != null
                && this.visor$cameraEntityCached
                && this.visor$cameraEntityCacheDepth == 0) {
            visor$cameraEntityCache.apply(cameraEntity);
            this.visor$cameraEntityCached = false;
        }
    }

    @Override
    @Unique
    public void visor$applyCachedCameraEntityPosition(Entity cameraEntity) {
        if (cameraEntity != null && this.visor$cameraEntityCached) {
            this.visor$cameraEntityCache.apply(cameraEntity);
        }
    }

    @Override
    public VRCameraEntityCache visor$getCameraEntityCache() {
        return visor$cameraEntityCache;
    }

    @Override
    @Unique
    public boolean visor$isOnFire() {
        return visor$onfire;
    }

    @Override
    @Unique
    public boolean visor$isInBlock() {
        return visor$inBlock;
    }

    @Override
    @Unique
    public float visor$getBlockProximity() {
        return visor$blockProximity;
    }

    @Unique
    private void visor$updateCameraOverlaps(float partialTicks) {
        this.visor$inBlock = false;
        this.visor$blockProximity = 0.0f;

        this.visor$onfire = false;

        if(minecraft.player.isSpectator()
                || !minecraft.player.isAlive()
                || VRRenderState.getSceneType().isMainMenu()){
            return;
        }
        // fix for immersive portals issue
        if (this.minecraft.level != this.minecraft.player.level()) {
            return;
        }
        VRRenderPass renderPass = VRRenderState.getRenderPass();
        if (renderPass == null) {
            return;
        }
        var cameraPos = RenderPoseHelper.getCameraPosition(
                renderPass,
                ClientContext.localPlayer.getPoseData(PlayerPoseType.RENDER)
        );

        float inBlockEffectStart = 0.3f;
        float distance = RenderHelper.distanceToNearestSolidBlockSurface(
                new Vec3((Vector3f) cameraPos),
                inBlockEffectStart
        );

        this.visor$blockProximity = Math.max(
                0.0f,
                1.0f - distance / inBlockEffectStart
        );
        this.visor$inBlock = distance < this.visor$getNearClipPlane() * 2.0f;


        this.visor$onfire = VRRenderState.getRenderPass() != VRRenderPass.THIRD_PERSON
                && this.minecraft.player.isOnFire()
                && !ModLoader.get().renderFireOverlay(
                this.minecraft.player, new PoseStack()
        );
    }
}
