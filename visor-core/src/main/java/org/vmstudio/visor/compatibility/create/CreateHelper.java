package org.vmstudio.visor.compatibility.create;

import org.jetbrains.annotations.NotNull;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.common.addon.VisorAddon;

public class CreateHelper {
    public static void initializeCompat(@NotNull VisorAddon owner) {
        if (isLoaded()) {
            var registries = VisorAPI.addonManager().getRegistries();
            registries.inputRedirects().registerComponent(new CreateControlsVRInputRedirect(owner));
        }
    }

    public static boolean isLoaded() {
        return ModLoader.get().isModLoaded("create");
    }
}
