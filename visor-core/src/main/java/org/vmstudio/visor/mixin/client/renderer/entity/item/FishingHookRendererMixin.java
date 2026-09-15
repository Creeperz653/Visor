package org.vmstudio.visor.mixin.client.renderer.entity.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.render.VRRenderState;
import org.vmstudio.visor.core.client.render.helpers.RenderPoseHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
//?} elif <1.20.5 {
/*import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

import static org.vmstudio.visor.core.client.VisorClientImpl.MC;

@Mixin(FishingHookRenderer.class)
//? if >=1.21.2 {
public abstract class FishingHookRendererMixin extends EntityRenderer<FishingHook, FishingHookRenderState> {
//?} else {
/*public abstract class FishingHookRendererMixin extends EntityRenderer<FishingHook> {
*///?}

    protected FishingHookRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Unique
    private Vec3 visor$savedHandPos;

    //? if >=1.21.2 {
    @Inject(at = @At(value = "HEAD"), method = "render(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
    cancellable = true)
    private void visor$noRenderOnGameScreen(CallbackInfo ci){
        if(MC.screen != null){
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(at = @At(value = "HEAD"), method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
    cancellable = true)
    private void visor$noRenderOnGameScreen(CallbackInfo ci){
        if(MC.screen != null){
            ci.cancel();
        }
    }
    *///?}

    @Unique
    private boolean visor$vrLineAnchored(FishingHook fishingHook) {
        return VRRenderState.getPhase().isNotVanilla()
                && this.entityRenderDispatcher.options.getCameraType().isFirstPerson()
                && fishingHook.getPlayerOwner() == MC.player;
    }

    @Unique
    private Vec3 visor$vrLineAnchor(FishingHook fishingHook) {
        var renderPose = ClientContext.localPlayer
                .getPoseData(PlayerPoseType.RENDER);

        HandType handType = HandType.OFFHAND;
        if (fishingHook.getPlayerOwner().getMainHandItem().getItem() instanceof FishingRodItem) {
            handType = HandType.MAIN;
        }
        Vector3f handPos = new Vector3f(
                RenderPoseHelper.getHandPosition(handType)
        );

        Vector3f handDir = renderPose
                .getGripHand(handType).transformDirection(
                        new Vector3f(-0.05f,-0.06f,-1.0f)
                );

        float worldScale = renderPose.getWorldScale();
        Vector3f finalPos = handPos.add(
                        new Vector3f(handDir).mul(
                                0.525f * worldScale
                        )
                );

        return new Vec3(finalPos);
    }

    // 1.20.5 replaced the three loose double locals of the line anchor with one getPlayerHandPos call,
    // 1.21.2 moved that call into extractRenderState
    //? if >=1.21.2 {
    @WrapOperation(method = "extractRenderState(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;getPlayerHandPos(Lnet/minecraft/world/entity/player/Player;FF)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 visor$lineAnchor(FishingHookRenderer instance, Player player,
                                  float handAnim, float partialTicks,
                                  Operation<Vec3> original,
                                  @Local(argsOnly = true) FishingHook fishingHook) {
        if (!visor$vrLineAnchored(fishingHook)) {
            return original.call(instance, player, handAnim, partialTicks);
        }
        return visor$vrLineAnchor(fishingHook);
    }
    //?} elif >=1.20.5 {
    /*@WrapOperation(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;getPlayerHandPos(Lnet/minecraft/world/entity/player/Player;FF)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 visor$lineAnchor(FishingHookRenderer instance, Player player,
                                  float handAnim, float partialTicks,
                                  Operation<Vec3> original,
                                  @Local(argsOnly = true) FishingHook fishingHook) {
        if (!visor$vrLineAnchored(fishingHook)) {
            return original.call(instance, player, handAnim, partialTicks);
        }
        return visor$vrLineAnchor(fishingHook);
    }
    *///?} else {
    /*@ModifyVariable(at = @At(value = "LOAD"),
            method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", index = 25)
    private double visor$lineAnchorX(double value, FishingHook fishingHook) {
        if(!visor$vrLineAnchored(fishingHook)){
            return value;
        }
        visor$savedHandPos = visor$vrLineAnchor(fishingHook);

        return visor$savedHandPos.x;
    }

    @ModifyVariable(at = @At(value = "LOAD"),
            method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", index = 27)
    private double visor$lineAnchorY(double value, FishingHook fishingHook) {
        if(!visor$vrLineAnchored(fishingHook)){
            return value;
        }

        return visor$savedHandPos.y;
    }

    @ModifyVariable(at = @At(value = "LOAD"),
            method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", index = 29)
    private double visor$lineAnchorZ(double value, FishingHook fishingHook) {
        if(!visor$vrLineAnchored(fishingHook)){
            return value;
        }

        return visor$savedHandPos.z;
    }
    *///?}

}
