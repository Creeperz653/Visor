package org.vmstudio.visor.mixin.client.renderer.entity.player.layers;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vmstudio.visor.core.client.render.VRRenderState;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.vmstudio.visor.core.client.render.player.VRPlayerRenderState;
//?}

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    //? if >=1.21.2 {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void visor$hideHeadDecorationOnVRSelf(CallbackInfo ci,
                                                  @Local(argsOnly = true) LivingEntityRenderState state)
    {
        if (visor$hidesHead(VRPlayerRenderState.playerOf(state))) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void visor$hideHeadDecorationOnVRSelf(CallbackInfo ci,
                                                  @Local(argsOnly = true) LivingEntity entity)
    {
        if (visor$hidesHead(entity)) {
            ci.cancel();
        }
    }
    *///?}

    @Unique
    private static boolean visor$hidesHead(@Nullable LivingEntity entity) {
        return entity != null
                && (VRRenderState.isSelfModelRender(entity)
                || VRRenderState.isSpectatedVRView(entity));
    }
}
