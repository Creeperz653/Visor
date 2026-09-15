package org.vmstudio.visor.api.compatibility.mcversion.render;

import com.mojang.blaze3d.systems.RenderSystem;
//? if >=1.21.2 {
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderProgram;
//?} else {
/*import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import java.util.function.Supplier;
*///?}

/**
 * Cross-mc-version selection of vanilla core shaders
 */
public class McShaders {
    private McShaders() {
        throw new UnsupportedOperationException("This is an utility class and cannot be instantiated");
    }

    public enum Core {
        POSITION,
        POSITION_COLOR,
        POSITION_TEX,
        POSITION_TEX_COLOR,
        RENDERTYPE_TEXT
    }

    public static void use(Core shader) {
        //? if >=1.21.2 {
        ShaderProgram program = switch (shader) {
            case POSITION -> CoreShaders.POSITION;
            case POSITION_COLOR -> CoreShaders.POSITION_COLOR;
            case POSITION_TEX -> CoreShaders.POSITION_TEX;
            case POSITION_TEX_COLOR -> CoreShaders.POSITION_TEX_COLOR;
            case RENDERTYPE_TEXT -> CoreShaders.RENDERTYPE_TEXT;
        };
        RenderSystem.setShader(program);
        //?} else {
        /*Supplier<ShaderInstance> program = switch (shader) {
            case POSITION -> GameRenderer::getPositionShader;
            case POSITION_COLOR -> GameRenderer::getPositionColorShader;
            case POSITION_TEX -> GameRenderer::getPositionTexShader;
            case POSITION_TEX_COLOR -> GameRenderer::getPositionTexColorShader;
            case RENDERTYPE_TEXT -> GameRenderer::getRendertypeTextShader;
        };
        RenderSystem.setShader(program);
        *///?}
    }
}
