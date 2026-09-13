package org.vmstudio.visor.mixin.client.renderer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.player.VRPose;
import org.vmstudio.visor.api.compatibility.mcversion.McVersionClientUtils;
import org.vmstudio.visor.api.server.VRServerSettings;
import org.vmstudio.visor.compatibility.immportals.ImmPortalsCompatHelper;
import org.vmstudio.visor.compatibility.sable.SableCompatHelper;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorState;
import org.vmstudio.visor.core.client.player.pose.LocalPlayerPose;
import org.vmstudio.visor.core.client.render.VRCameraEntityCache;
import org.vmstudio.visor.extensions.client.render.GameRendererExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.vmstudio.visor.core.client.VisorClientImpl.MC;

@Mixin(GameRenderer.class)
public abstract class GameRendererPickMixin implements GameRendererExtension {

    // ---- Shadow fields ----
    @Shadow
    @Final
    Minecraft minecraft;

    // ---- Unique fields ----
    @Unique
    public Vec3 visor$aimHitPos;
    @Unique
    private HandType visor$pickingHand;
    @Unique
    private final HitResult[] visor$handHitResult = new HitResult[2];
    @Unique
    private final Vec3[] visor$handAimHitPos = new Vec3[2];
    @Unique
    private final Entity[] visor$handPickEntity = new Entity[2];

    // ---- Shadow methods ----
    @Shadow
    public abstract void pick(float f);


    /* ***************** *\
  //--------MIXINS--------\\
    \* ***************** */

    @WrapMethod(method = "pick(F)V")
    private void visor$pickWithVRHands(float partialTick, Operation<Void> original) {
        if(VisorState.get().isNotActive()){
            original.call(partialTick);
            return;
        }
        if (this.minecraft.screen != null && this.minecraft.hitResult != null) {
            return;
        }
        if (this.minecraft.getCameraEntity() == null) {
            this.minecraft.hitResult = visor$fallbackMiss();
            return;
        }

        HandType activeHand = ClientContext.localPlayer.getActiveHand();
        visor$pickWithHand(activeHand, partialTick, original);

        HandType otherHand = activeHand.opposite();
        if (VRServerSettings.isTwoHandedVR()
                && ClientContext.rawPoseHandler.getControllerData(otherHand).isTracking()) {
            HitResult activeHit = this.minecraft.hitResult;
            Entity activePickEntity = this.minecraft.crosshairPickEntity;

            visor$pickWithHand(otherHand, partialTick, original);

            this.minecraft.hitResult = activeHit;
            this.minecraft.crosshairPickEntity = activePickEntity;
            this.visor$aimHitPos = visor$handAimHitPos[activeHand.ordinal()];
        } else {
            visor$handHitResult[otherHand.ordinal()] = null;
            visor$handAimHitPos[otherHand.ordinal()] = null;
            visor$handPickEntity[otherHand.ordinal()] = null;
        }
    }

    @Unique
    private BlockHitResult visor$fallbackMiss() {
        var player = this.minecraft.player;
        if (player == null) {
            return BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO);
        }
        return BlockHitResult.miss(player.position(), player.getDirection(), player.blockPosition());
    }

    @Unique
    private void visor$pickWithHand(HandType hand, float partialTick, Operation<Void> original) {
        visor$pickingHand = hand;

        Entity cameraEntity = this.minecraft.getCameraEntity();
        AABB originalBB = cameraEntity.getBoundingBox();
        this.visor$cacheCameraEntity(cameraEntity);
        this.visor$setupCameraEntity(
                ClientContext.localPlayer
                        .getPoseData(PlayerPoseType.RENDER)
                        .getHand(hand)
        );
        VRCameraEntityCache cameraEntityCache = this.visor$getCameraEntityCache();
        double shiftX = cameraEntity.getX() - cameraEntityCache.getX();
        double shiftY = cameraEntity.getY() - cameraEntityCache.getY();
        double shiftZ = cameraEntity.getZ() - cameraEntityCache.getZ();
        cameraEntity.setBoundingBox(originalBB.move(shiftX, shiftY, shiftZ));

        original.call(partialTick);

        this.visor$restoreCameraEntity(cameraEntity);
        cameraEntity.setBoundingBox(originalBB);

        HitResult hitResult = this.minecraft.hitResult;
        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
            // includes entity hits missed by visor$pickPos
            if (SableCompatHelper.isLoaded()) {
                this.visor$aimHitPos = SableCompatHelper.toWorldPos(
                        this.minecraft.level,
                        hitResult,
                        hitResult.getLocation()
                );
            } else {
                this.visor$aimHitPos = hitResult.getLocation();
            }
        }
        visor$handHitResult[hand.ordinal()] = hitResult;
        visor$handAimHitPos[hand.ordinal()] = this.visor$aimHitPos;
        visor$handPickEntity[hand.ordinal()] = this.minecraft.crosshairPickEntity;
        visor$pickingHand = null;
    }

    // 1.20.5 moved the ray trace into pick(Entity,DDF), pick(F)V has no Vec3 locals left
    //? if >=1.20.5 {
    @ModifyVariable(at = @At("STORE"), method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", ordinal = 0)
    //?} else {
    /*@ModifyVariable(at = @At("STORE"), method = "pick(F)V", ordinal = 0)
    *///?}
    public Vec3 visor$pickPos(Vec3 original) {
        if (VisorState.get().isNotActive()) {
            return original;
        }
        LocalPlayerPose renderPose = ClientContext.localPlayer
                .getPoseData(PlayerPoseType.RENDER);

        HandType hand = visor$pickingHand != null
                ? visor$pickingHand
                : ClientContext.localPlayer.getActiveHand();

        HitResult hitResult = visor$pickBlock(
                renderPose.getHand(hand),
                McVersionClientUtils.blockPickRange(this.minecraft.gameMode, this.minecraft.player),
                false
        );
        this.minecraft.hitResult = hitResult;
        Vec3 fallbackAimHitPos = visor$pointAlongAim(
                renderPose.getHand(hand),
                McVersionClientUtils.blockPickRange(this.minecraft.gameMode, this.minecraft.player)
        );
        this.visor$aimHitPos = hitResult != null && hitResult.getType() != HitResult.Type.MISS
                ? SableCompatHelper.isLoaded() ? SableCompatHelper.toWorldPos(this.minecraft.level, hitResult, hitResult.getLocation()) : hitResult.getLocation()
                : fallbackAimHitPos;

        return new Vec3((Vector3f) renderPose.getHand(hand).getPosition());
    }

    //? if >=1.20.5 {
    @ModifyVariable(at = @At("STORE"), method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", ordinal = 1)
    //?} else {
    /*@ModifyVariable(at = @At("STORE"), method = "pick(F)V", ordinal = 1)
    *///?}
    public Vec3 visor$pickDirection(Vec3 original) {
        if (VisorState.get().isNotActive()) {
            return original;
        }
        HandType hand = visor$pickingHand != null
                ? visor$pickingHand
                : ClientContext.localPlayer.getActiveHand();

        return new Vec3(
                (Vector3f) ClientContext.localPlayer.getPoseData(PlayerPoseType.RENDER)
                        .getHand(hand).getDirection()
        );
    }

    //? if >=1.20.5 {
    @Redirect(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult visor$vrBlockPick(Entity entity, double range, float partialTick, boolean fluid) {
        if (VisorState.get().isNotActive() || this.minecraft.hitResult == null) {
            return entity.pick(range, partialTick, fluid);
        }
        return this.minecraft.hitResult;
    }
    //?}


    /* ************************ *\
  //--------PUBLIC METHODS--------\\
    \* ************************ */

    @Override
    @Unique
    public Vec3 visor$getAimHitPos() {
        return visor$aimHitPos;
    }

    @Override
    @Unique
    public Vec3 visor$getAimHitPos(HandType hand) {
        return visor$handAimHitPos[hand.ordinal()];
    }

    @Override
    @Unique
    public HitResult visor$getHandHitResult(HandType hand) {
        return visor$handHitResult[hand.ordinal()];
    }

    @Override
    @Unique
    public void visor$applyHandPick(HandType hand) {
        HitResult hitResult = visor$handHitResult[hand.ordinal()];
        Vec3 aimHitPos = visor$handAimHitPos[hand.ordinal()];
        if (hitResult == null || aimHitPos == null) {
            this.pick(1.0f);
            return;
        }
        this.minecraft.hitResult = hitResult;
        this.minecraft.crosshairPickEntity = visor$handPickEntity[hand.ordinal()];
        this.visor$aimHitPos = aimHitPos;
    }

    @Unique
    public Vec3 visor$pointAlongAim(VRPose vrPose,
                                           double distance) {
        var dir = vrPose.getDirection();
        return new Vec3(vrPose
                .getPosition().add(
                        dir.x() * (float) distance,
                        dir.y() * (float) distance,
                        dir.z() * (float) distance,
                        new Vector3f()
                )
        );
    }

    @Unique
    public HitResult visor$pickBlock(VRPose vrPose,
                                     double blockReachDistance,
                                     boolean fluid
    ) {
        return ImmPortalsCompatHelper.pickBlock(MC.level, vrPose, blockReachDistance, fluid, MC.player);
    }
}
