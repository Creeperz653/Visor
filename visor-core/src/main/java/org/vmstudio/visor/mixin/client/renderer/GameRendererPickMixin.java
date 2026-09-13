package org.vmstudio.visor.mixin.client.renderer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.vmstudio.visor.core.client.player.VRAimPicker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class GameRendererPickMixin {

    /* ***************** *\
  //--------MIXINS--------\\
    \* ***************** */

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
}
