package org.vmstudio.visor.mixin.client.renderer.entity.player.layers;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.render.VRRenderState;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.vmstudio.visor.core.client.render.player.VRPlayerRenderState;
//?} else {
/*import net.minecraft.world.item.ItemStack;
*///?}

@Mixin(value = PlayerItemInHandLayer.class, priority = 900)
public class PlayerItemInHandLayerMixin {

    // 1.21.2 added a bridge overload, the descriptor keeps the injectors on the real method
    //? if >=1.21.2 {
    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void visor$noItemInGui(
            CallbackInfo ci, @Local(argsOnly = true) PlayerRenderState state, @Local(argsOnly = true) HumanoidArm arm)
    {
        if (visor$hideItem(VRPlayerRenderState.playerOf(state), arm)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderArmWithItem(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean visor$noSpyglass(boolean isSpyglass, @Local(argsOnly = true) PlayerRenderState state) {
        var player = VRPlayerRenderState.playerOf(state);
        return isSpyglass && (player == null || !VRRenderState.isSelfModelHandsRender(player));
    }
    //?} else {
    /*@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void visor$noItemInGui(
            CallbackInfo ci, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) HumanoidArm arm)
    {
        if (visor$hideItem(entity, arm)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getUseItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack visor$noSpyglass(
        ItemStack useItem, @Local(argsOnly = true) LivingEntity entity)
    {
        return VRRenderState.isSelfModelHandsRender(entity) ? ItemStack.EMPTY : useItem;
    }
    *///?}

    @Unique
    private static boolean visor$hideItem(@Nullable LivingEntity entity, HumanoidArm arm) {
        if (entity == null) {
            return false;
        }
        if (VRRenderState.isSpectatedVRView(entity)) {
            return true;
        }
        if (VRRenderState.isSelfModelRender(entity)) {
            if (!VRRenderState.isSelfModelHandsRender(entity)) {
                return true;
            }
            boolean leftHanded = ClientContext.localPlayer.isLeftHanded();
            return !ClientContext.decorationRenderer
                    .getHandState(HandType.fromMcArm(arm, leftHanded)).isWithItem();
        }
        return false;
    }
}
