package org.vmstudio.visor.mixin.client.renderer.entity.player.layers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.vmstudio.visor.core.client.player.VRClientPlayers;
import org.vmstudio.visor.core.client.render.player.BackLayerPlacement;
//? if >=1.21.2 {
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.vmstudio.visor.api.client.player.VRClientPlayer;
import org.vmstudio.visor.core.client.render.player.VRPlayerRenderState;
//?} else {
/*import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

@Mixin(CapeLayer.class)
//? if >=1.21.2 {
public abstract class CapeLayerMixin extends RenderLayer<PlayerRenderState, PlayerModel> {
//?} else {
/*public abstract class CapeLayerMixin extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
*///?}

    //? if <1.21.2 {
    /*@Unique
    private static final float VANILLA_CROUCH_CAPE_LIFT = 25.0F;
    *///?}

    @Unique
    private static final float ARMOR_CLEARANCE_Y = -0.85F;
    @Unique
    private static final float ARMOR_CLEARANCE_Z = 1.1F;
    @Unique
    private final BackLayerPlacement visor$placement = new BackLayerPlacement();

    @Unique
    private final Vector3f visor$offset = new Vector3f();

    //? if >=1.21.2 {
    public CapeLayerMixin(RenderLayerParent<PlayerRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void visor$noArmorShift(PoseStack poseStack, float x, float y, float z, Operation<Void> original,
                                    @Local(argsOnly = true) PlayerRenderState state) {
        if (visor$vrPlayer(state) == null) {
            original.call(poseStack, x, y, z);
        }
    }

    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void visor$capeAnchor(
        HumanoidModel<?> capeModel, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
        Operation<Void> original, @Local(argsOnly = true) PlayerRenderState state)
    {
        AbstractClientPlayer player = VRPlayerRenderState.playerOf(state);
        VRClientPlayer vrPlayer = player == null ? null : VRClientPlayers.getPlayer(player.getUUID());
        if (vrPlayer == null || !capeModel.body.hasChild("cape")) {
            original.call(capeModel, poseStack, consumer, packedLight, packedOverlay);
            return;
        }

        PlayerModel model = getParentModel();
        visor$placement.aim(model.body, true);
        float bodyPitch = visor$placement.pitch();
        // read the slot, not the render state: 1.21.4 renamed its chestItem field to chestEquipment
        boolean armor = !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();

        visor$offset.set(0F, 0F, BackLayerPlacement.restingDepth(model.body));
        if (armor) {
            visor$offset.add(0F, ARMOR_CLEARANCE_Y, ARMOR_CLEARANCE_Z);
        }
        visor$placement.place(vrPlayer, model.body, visor$offset, visor$offset);
        poseStack.translate(visor$offset.x, -visor$offset.y, -visor$offset.z);

        float flatten = state.isFallFlying ? 1F : state.swimAmount;
        float capePitch = state.capeFlap + Math.max(bodyPitch, -Mth.HALF_PI * flatten) * Mth.RAD_TO_DEG;
        float leanFraction = bodyPitch / Mth.HALF_PI;
        float walkLift = leanFraction < 0F ? 0F : state.capeLean * (1F - Math.min(leanFraction, 1F));
        poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + walkLift / 2.0F + capePitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.capeLean2 / 2.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.capeLean2 / 2.0F + Mth.RAD_TO_DEG * visor$placement.yaw()));

        capeModel.body.resetPose();
        ModelPart cape = capeModel.body.getChild("cape");
        cape.setRotation(0F, 0F, 0F);

        if (armor) {
            cape.setPos(0F, state.isCrouching ? 0.8F : -0.85F, state.isCrouching ? 0.3F : -1.1F);
        } else {
            cape.setPos(0F, state.isCrouching ? 1.85F : 0F, state.isCrouching ? 1.4F : 0F);
        }
        original.call(capeModel, poseStack, consumer, packedLight, packedOverlay);
    }

    @Unique
    private static VRClientPlayer visor$vrPlayer(PlayerRenderState state) {
        AbstractClientPlayer player = VRPlayerRenderState.playerOf(state);
        return player == null ? null : VRClientPlayers.getPlayer(player.getUUID());
    }
    //?} else {
    /*public CapeLayerMixin(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void visor$capeAnchor(
        PoseStack poseStack, float x, float y, float z, Operation<Void> original,
        @Local(argsOnly = true) AbstractClientPlayer player, @Local(argsOnly = true, ordinal = 2) float partialTick,
        @Share("capeBodyPitch") LocalFloatRef bodyPitchRef, @Share("capeBodyYaw") LocalFloatRef bodyYawRef)
    {
        var vrPlayer = VRClientPlayers.getPlayer(player.getUUID());
        if (vrPlayer == null) {
            original.call(poseStack, x, y, z);
            return;
        }

        PlayerModel<AbstractClientPlayer> model = getParentModel();
        visor$placement.aim(model.body, true);
        bodyPitchRef.set(visor$placement.pitch());
        bodyYawRef.set(visor$placement.yaw());

        visor$offset.set(0F, 0F, BackLayerPlacement.restingDepth(model.body));
        if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            visor$offset.add(0F, ARMOR_CLEARANCE_Y, ARMOR_CLEARANCE_Z);
        }

        visor$placement.place(vrPlayer, model.body, visor$offset, visor$offset);
        original.call(poseStack, visor$offset.x, -visor$offset.y, -visor$offset.z);
    }

    @ModifyVariable(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isCrouching()Z"), ordinal = 7)
    private float visor$capePitchWithBody(
        float capePitch, @Local(argsOnly = true) AbstractClientPlayer player,
        @Local(ordinal = 2, argsOnly = true) float partialTick, @Share("capeBodyPitch") LocalFloatRef bodyPitchRef)
    {
        if (!VRClientPlayers.isTracked(player)) {
            return capePitch;
        }
        if (player.isCrouching()) {
            // undo the lift vanilla CapeLayer adds while crouching, the VR body pitch replaces it
            capePitch -= VANILLA_CROUCH_CAPE_LIFT;
        }
        float flatten = player.isFallFlying() ? 1F : player.getSwimAmount(partialTick);
        float lowestPitch = -Mth.HALF_PI * flatten;
        return capePitch + Math.max(bodyPitchRef.get(), lowestPitch) * Mth.RAD_TO_DEG;
    }

    @ModifyVariable(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isCrouching()Z"), ordinal = 8)
    private float visor$capeWalkLift(
        float walkLift, @Local(argsOnly = true) AbstractClientPlayer player,
        @Share("capeBodyPitch") LocalFloatRef bodyPitchRef)
    {
        if (!VRClientPlayers.isTracked(player)) {
            return walkLift;
        }
        float leanFraction = bodyPitchRef.get() / Mth.HALF_PI;
        if (leanFraction < 0F) {
            return 0F;
        }
        return walkLift * (1F - Math.min(leanFraction, 1F));
    }

    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 2))
    private float visor$capeYawWithBody(
        float capeYaw, @Local(argsOnly = true) AbstractClientPlayer player,
        @Share("capeBodyYaw") LocalFloatRef bodyYawRef)
    {
        if (VRClientPlayers.isTracked(player)) {
            capeYaw += Mth.RAD_TO_DEG * bodyYawRef.get();
        }
        return capeYaw;
    }
    *///?}
}
