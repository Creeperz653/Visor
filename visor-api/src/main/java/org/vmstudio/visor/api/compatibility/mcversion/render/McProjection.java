package org.vmstudio.visor.api.compatibility.mcversion.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
//? if >=1.21.2 {
import com.mojang.blaze3d.ProjectionType;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexSorting;
*///?}

/**
 * Cross-mc-version facade over the projection matrix
 */
public class McProjection {
    private McProjection() {
        throw new UnsupportedOperationException("This is an utility class and cannot be instantiated");
    }

    public static void setPerspective(Matrix4f projection) {
        //? if >=1.21.2 {
        RenderSystem.setProjectionMatrix(projection, ProjectionType.PERSPECTIVE);
        //?} else {
        /*RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        *///?}
    }

    public static void setOrthographic(Matrix4f projection) {
        //? if >=1.21.2 {
        RenderSystem.setProjectionMatrix(projection, ProjectionType.ORTHOGRAPHIC);
        //?} else {
        /*RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
        *///?}
    }

    public static State save() {
        //? if >=1.21.2 {
        return new State(new Matrix4f(RenderSystem.getProjectionMatrix()), RenderSystem.getProjectionType());
        //?} else {
        /*return new State(new Matrix4f(RenderSystem.getProjectionMatrix()), RenderSystem.getVertexSorting());
        *///?}
    }

    public static void restore(State state) {
        RenderSystem.setProjectionMatrix(state.matrix, state.type);
    }

    public static final class State {
        private final Matrix4f matrix;
        //? if >=1.21.2 {
        private final ProjectionType type;

        private State(Matrix4f matrix, ProjectionType type) {
            this.matrix = matrix;
            this.type = type;
        }
        //?} else {
        /*private final VertexSorting type;

        private State(Matrix4f matrix, VertexSorting type) {
            this.matrix = matrix;
            this.type = type;
        }
        *///?}
    }
}
