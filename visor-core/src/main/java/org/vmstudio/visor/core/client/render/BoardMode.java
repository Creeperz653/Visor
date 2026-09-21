package org.vmstudio.visor.core.client.render;

import org.joml.Vector3f;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.player.pose.LocalPlayerPose;

/**
 * Transient (not saved) client-side state for the desk-board passthrough view.
 * Toggled by a client command; the world is rendered shrunk and clipped to a
 * box anchored at the pose captured the moment the mode was turned on.
 */
public class BoardMode {

    private static boolean active = false;
    private static final Vector3f origin = new Vector3f();
    private static float originYaw = 0f;

    private BoardMode() {
        throw new UnsupportedOperationException("This is an utility class and cannot be instantiated");
    }

    public static boolean isActive() {
        return active;
    }

    public static Vector3f getOrigin() {
        return origin;
    }

    public static float getOriginYaw() {
        return originYaw;
    }

    /**
     * Turns board mode on/off. On enable, captures the player's current
     * render-pose origin and yaw as the board's anchor point.
     */
    public static void toggle() {
        if (active) {
            active = false;
            return;
        }
        LocalPlayerPose renderPose = ClientContext.localPlayer.getPoseData(PlayerPoseType.RENDER);
        origin.set(renderPose.getOrigin());
        originYaw = renderPose.getRotationY();
        active = true;
    }
}