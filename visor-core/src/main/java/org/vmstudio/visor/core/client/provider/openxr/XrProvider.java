package org.vmstudio.visor.core.client.provider.openxr;

import me.phoenixra.atumvr.api.AtumVRLogger;
import me.phoenixra.atumvr.api.rendering.AtumVRRenderer;
import me.phoenixra.atumvr.core.XRProvider;
import me.phoenixra.atumvr.core.XRState;
import me.phoenixra.atumvr.core.enums.XRGraphicsApi;
import me.phoenixra.atumvr.core.enums.XRSessionState;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.settings.VRClientSettings;
import org.vmstudio.visor.api.client.settings.enums.MainMenuSceneMode;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorClientImpl;
import org.vmstudio.visor.core.client.provider.openxr.render.XrRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openxr.EXTCompositionLayerInvertedAlpha;
import org.lwjgl.openxr.FBPassthrough;
import org.lwjgl.openxr.XrPassthroughCreateInfoFB;
import org.lwjgl.openxr.XrPassthroughFB;
import org.lwjgl.openxr.XrPassthroughLayerCreateInfoFB;
import org.lwjgl.openxr.XrPassthroughLayerFB;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;

import static org.vmstudio.visor.core.client.VisorClientImpl.MC;

public class XrProvider extends XRProvider {

    /** Handle of the FB passthrough feature, null if the runtime doesn't support XR_FB_passthrough. */
    private XrPassthroughFB passthroughHandle;
    /** Handle of the reconstruction passthrough layer submitted as the underlay each frame. */
    private XrPassthroughLayerFB passthroughLayerHandle;

    public XrProvider(@NotNull String appName, @NotNull AtumVRLogger logger) {
        super(appName, logger);

        ClientContext.rawPoseHandler = new XrRawPoseHandler(this);
        ClientContext.inputProvider = inputHandler;
    }

    @Override
    public void initializeVR() throws Throwable {

        super.initializeVR();

        ClientContext.rawPoseHandler.getTrackersData().setTracking(
                !getInputHandler().getTrackerProviders().isEmpty()
        );
        ClientContext.rawPoseHandler.getHandsData().setTracking(
                getInputHandler().getHandsProvider() != null
        );

        ClientContext.settingsManager.loadOptions();

        initPassthrough();

        VisorClientImpl.LOGGER.info("OpenXR initialized");
    }

    /**
     * Creates the FB passthrough feature and a single reconstruction layer,
     * used as a compositor underlay for the desk-board passthrough view.
     * <p>
     *     Does nothing if the runtime never actually enabled {@code XR_FB_passthrough}
     *     (AtumVR silently drops unsupported extensions from
     *     {@link #getXRAppExtensions()}, so this checks what actually got enabled).
     * </p>
     */
    private void initPassthrough() {
        if (!session.getInstance().isExtensionEnabled(
                FBPassthrough.XR_FB_PASSTHROUGH_EXTENSION_NAME)) {
            VisorClientImpl.LOGGER.info(
                    "XR_FB_passthrough not enabled, skipping passthrough setup"
            );
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var createInfo = XrPassthroughCreateInfoFB.calloc(stack)
                    .type$Default()
                    .next(0)
                    .flags(0);

            var passthroughPointer = stack.callocPointer(1);
            checkXRError(
                    FBPassthrough.xrCreatePassthroughFB(session.getHandle(), createInfo, passthroughPointer),
                    "xrCreatePassthroughFB"
            );
            passthroughHandle = new XrPassthroughFB(passthroughPointer.get(0), session.getHandle());

            checkXRError(
                    FBPassthrough.xrPassthroughStartFB(passthroughHandle),
                    "xrPassthroughStartFB"
            );

            var layerCreateInfo = XrPassthroughLayerCreateInfoFB.calloc(stack)
                    .type$Default()
                    .next(0)
                    .passthrough(passthroughHandle)
                    .flags(FBPassthrough.XR_PASSTHROUGH_IS_RUNNING_AT_CREATION_BIT_FB)
                    .purpose(FBPassthrough.XR_PASSTHROUGH_LAYER_PURPOSE_RECONSTRUCTION_FB);

            var layerPointer = stack.callocPointer(1);
            checkXRError(
                    FBPassthrough.xrCreatePassthroughLayerFB(session.getHandle(), layerCreateInfo, layerPointer),
                    "xrCreatePassthroughLayerFB"
            );
            passthroughLayerHandle = new XrPassthroughLayerFB(layerPointer.get(0), session.getHandle());

            VisorClientImpl.LOGGER.info("FB passthrough layer created");
        } catch (Throwable t) {
            passthroughHandle = null;
            passthroughLayerHandle = null;
            VisorClientImpl.LOGGER.error(
                    "Failed to set up FB passthrough, continuing without it: "
                            + t.getClass().getSimpleName() + ": " + t.getMessage()
            );
        }
    }

        /**
     * @return true if the passthrough layer is ready to be submitted each frame
     */
    public boolean isPassthroughActive() {
        return passthroughLayerHandle != null;
    }

    /**
     * @return true if the passthrough underlay should actually be shown
     * this frame. Separate from {@link #isPassthroughActive()} (which just
     * means the feature is ready) — until proper board clipping exists
     * (Phase 3), passthrough is restricted to the main menu with
     * MainMenuSceneMode.PASSTHROUGH selected, so it doesn't leak through
     * translucent world content (leaves, glass, water) during gameplay.
     */
        public boolean shouldShowPassthrough() {
        if (!isPassthroughActive()) {
            return false;
        }
        if (org.vmstudio.visor.core.client.render.BoardMode.isActive()) {
            return true;
        }
        return VisorAPI.clientState().sceneType().isMainMenu()
                && VRClientSettings.getMainMenuScene() == MainMenuSceneMode.PASSTHROUGH;
    }

    /**
     * @return the passthrough layer handle to submit at {@code xrEndFrame},
     * or null if passthrough isn't active — check {@link #isPassthroughActive()} first
     */
    @Nullable
    public XrPassthroughLayerFB getPassthroughLayerHandle() {
        return passthroughLayerHandle;
    }

    /**
     * @return true if the runtime actually enabled
     * XR_EXT_composition_layer_inverted_alpha — only then is it valid
     * to set XR_COMPOSITION_LAYER_INVERTED_ALPHA_BIT_EXT on a submitted layer.
     */
    public boolean isInvertedAlphaEnabled() {
        return session.getInstance().isExtensionEnabled(
                EXTCompositionLayerInvertedAlpha.XR_EXT_COMPOSITION_LAYER_INVERTED_ALPHA_EXTENSION_NAME
        );
    }

    @Override
    public void startFrame() {
        super.startFrame();
        ClientContext.rawPoseHandler.updatePose();
    }

    @Override
    public @Nullable XRGraphicsApi getGraphicsApiPreference() {
        return VRClientSettings.getGraphicsApi().toPreference();
    }

    @Override
    public @NotNull XRState createStateHandler() {
        return new XRState(this);
    }

    @Override
    public @NotNull XrInputHandler createInputHandler() {
        return new XrInputHandler(this);
    }

    @Override
    public @NotNull AtumVRRenderer createRenderer() {
        return new XrRenderer(this);
    }

    /**
     * Requests {@code XR_FB_passthrough} (for the underlay layer itself) and
     * {@code XR_EXT_composition_layer_inverted_alpha} (Quest Link inverts the
     * alpha convention: 0 = opaque there) on top of AtumVR's defaults.
     * <p>
     *     Both are dropped automatically by AtumVR if the runtime doesn't
     *     support them, so this is safe to call unconditionally.
     * </p>
     */
    @Override
    public @NotNull List<String> getXRAppExtensions() {
        List<String> extensions = new ArrayList<>(super.getXRAppExtensions());
        extensions.add(FBPassthrough.XR_FB_PASSTHROUGH_EXTENSION_NAME);
        extensions.add(EXTCompositionLayerInvertedAlpha.XR_EXT_COMPOSITION_LAYER_INVERTED_ALPHA_EXTENSION_NAME);
        return extensions;
    }

    @Override
    public void onStateChanged(XRSessionState state) {
        if(state == XRSessionState.EXITING){
            MC.stop();
        }
    }

    @Override
    public @NotNull XrInputHandler getInputHandler() {
        return (XrInputHandler) super.getInputHandler();
    }

    @Override
    public void destroy() {
        if (passthroughLayerHandle != null) {
            checkXRError(
                    false,
                    FBPassthrough.xrDestroyPassthroughLayerFB(passthroughLayerHandle),
                    "xrDestroyPassthroughLayerFB", ""
            );
            passthroughLayerHandle = null;
        }
        if (passthroughHandle != null) {
            checkXRError(
                    false,
                    FBPassthrough.xrDestroyPassthroughFB(passthroughHandle),
                    "xrDestroyPassthroughFB", ""
            );
            passthroughHandle = null;
        }
        super.destroy();
    }
}