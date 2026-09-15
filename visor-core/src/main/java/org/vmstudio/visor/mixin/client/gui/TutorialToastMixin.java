package org.vmstudio.visor.mixin.client.gui;

import org.vmstudio.visor.core.client.VisorState;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if <1.21.2 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
*///?}

@Mixin(TutorialToast.class)
public abstract class TutorialToastMixin implements Toast {

    //? if >=1.21.2 {
    @Inject(at = @At("HEAD"), method = "getWantedVisibility", cancellable = true)
    public void visor$noToast(CallbackInfoReturnable<Visibility> ci) {
        if(VisorState.get().isNotActive()) return;
        ci.setReturnValue(Visibility.HIDE);
    }
    //?} else {
    /*@Inject(at = @At("HEAD"), method = "render", cancellable = true)
    public void visor$noToast(GuiGraphics guiGraphics,
                                       ToastComponent toastComponent,
                                       long l, CallbackInfoReturnable<Visibility> ci) {
        if(VisorState.get().isNotActive()) return;
        ci.setReturnValue(Visibility.HIDE);
    }
    *///?}

}
