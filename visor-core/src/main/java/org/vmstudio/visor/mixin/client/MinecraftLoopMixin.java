package org.vmstudio.visor.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import org.vmstudio.visor.api.compatibility.mcversion.render.McModelViewStack;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorState;
import org.vmstudio.visor.core.client.render.VRRenderState;
import org.vmstudio.visor.core.client.render.context.PreRenderContext;
import org.vmstudio.visor.core.client.render.context.RenderContext;
import org.vmstudio.visor.extensions.client.MinecraftExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.profiling.ProfilerFiller;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?} else {
/*import net.minecraft.client.Timer;
 *///?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

// game VR loop
@Mixin(Minecraft.class)
public abstract class MinecraftLoopMixin implements MinecraftExtension {

    // ---- Shadow fields ----
    @Shadow
    private ProfilerFiller profiler;
    @Shadow
    private boolean pause;
    //? if >=1.21 {
    @Final
    @Shadow
    private DeltaTracker.Timer timer;
    //?} else {
    /*@Shadow
    private float pausePartialTick;
    @Final
    @Shadow
    private Timer timer;
    *///?}


    /* ***************** *\
  //--------MIXINS--------\\
    \* ***************** */

    /**
     * Pre Ticks Visor right before mc tick() is called
     *
     * @param ci s
     */
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"), method = "runTick")
    public void visor$preTick(CallbackInfo ci) {
        if(ClientContext.visor != null) {
            ClientContext.visor.preTickVR();
        }
    }

    /**
     * Ticks Visor (before mc tick methods called)
     *
     * @param info s
     */
    @Inject(at = @At("HEAD"), method = "tick()V")
    public void visor$tick(CallbackInfo info) {
        if(ClientContext.visor != null) {
            ClientContext.visor.tickVR();
        }
    }

    /**
     * Post Ticks Visor right after mc tick() is called
     *
     * @param ci s
     */
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V", shift = Shift.AFTER), method = "runTick")
    public void visor$postTick(CallbackInfo ci) {
        if(ClientContext.visor != null) {
            ClientContext.visor.postTickVR();
        }
    }

    /**
     * Calls pre render task at the beginning of a frame
     *
     * @param tick     s
     * @param callback s
     */
    @Inject(at = @At("HEAD"), method = "runTick(Z)V")
    public void visor$runVR(boolean tick, CallbackInfo callback) {
        VisorState.updateState();
        if(ClientContext.visor != null) {
            ClientContext.visor
                    .onGameLoopStart();
        }
    }

    @Inject(method = "runTick", at = @At(value = "CONSTANT", args = "stringValue=render"))
    public void visor$preRenderVR(boolean tick, CallbackInfo callback) {
        if(ClientContext.visor != null) {
            ClientContext.visor
                    .preRenderVR(
                            new PreRenderContext(
                                    profiler, tick,
                                    visor$getPartialTicks()
                            )
                    );
        }
    }

    /**
     * Wraps vanilla GameRenderer.render() call
     * to update renderer state and start VRGui phase instead
     *
     * @param renderLevel s
     */
    //? if >=1.21 {
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"), method = "runTick", require = 1)
    public void visor$startVRGuiPhase(GameRenderer instance, DeltaTracker deltaTracker, boolean renderLevel, Operation<Void> original) {
        visor$renderVRGuiPhase(renderLevel, level -> original.call(instance, deltaTracker, level));
    }
    //?} else {
    /*@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"), method = "runTick", require = 1)
    public void visor$startVRGuiPhase(GameRenderer instance, float partialTicks, long nanoTime, boolean renderLevel, Operation<Void> original) {
        visor$renderVRGuiPhase(renderLevel, level -> original.call(instance, partialTicks, nanoTime, level));
    }
    *///?}

    private void visor$renderVRGuiPhase(boolean renderLevel, Consumer<Boolean> render) {
        if (VisorState.get().isNotActive()) {
            render.accept(renderLevel);
            return;
        }
        ClientContext.renderer.onGameRenderStart(renderLevel);
        boolean level = renderLevel && !VRRenderState.getPhase().isVRGui(); //disabled in VRGui phase, fallback on exception

        // keeps visor$matrix's identity() off the model-view base
        McModelViewStack.push();
        try {
            render.accept(level);
        } finally {
            McModelViewStack.pop();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /**
     * Calls VR rendering after mc rendered
     *
     * @param renderLevel s
     * @param ci          s
     * @param nanoTime    s
     */
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 4, shift = Shift.AFTER), method = "runTick")
    public void visor$renderVR(boolean renderLevel, CallbackInfo ci, @Local(ordinal = 0) long nanoTime) {
        if (ClientContext.visor != null) {
            ClientContext.visor
                    .renderVR(
                            new RenderContext(
                                    profiler,
                                    renderLevel,
                                    nanoTime,
                                    visor$getPartialTicks()
                            )
                    );
        }
    }

    /**
     * Ensures the render phase
     * and main render target are correct on resize
     *
     * @param ci
     */
    @Inject(at = @At("HEAD"), method = "resizeDisplay")
    private void visor$ensurePhaseOnResize(CallbackInfo ci) {
        if (VisorState.get().isInitialized()) {
            if (VisorState.get().isActive()) {
                VRRenderState.startVRGuiPhase();
            } else {
                VRRenderState.startVanillaPhase();
            }
        }
    }

    /**
     * Disables vanilla hit result calculation on tick.
     *
     * @param instance s
     * @param f        s
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V"), method = "tick")
    public void visor$noVanillaHitResult(GameRenderer instance, float f) {
        if (VisorState.get().isNotActive()) {
            instance.pick(f);
        }
    }


    /* ************************ *\
  //--------PUBLIC METHODS--------\\
    \* ************************ */

    @Override
    public float visor$getPartialTicks() {
        //? if >=1.21 {
        return this.timer.getGameTimeDeltaPartialTick(true);
        //?} else {
        /*return pause ? pausePartialTick : this.timer.partialTick;
        *///?}
    }
}
