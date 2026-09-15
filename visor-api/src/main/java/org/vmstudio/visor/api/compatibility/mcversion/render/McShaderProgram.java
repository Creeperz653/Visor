package org.vmstudio.visor.api.compatibility.mcversion.render;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import org.joml.Matrix4f;
//? if >=1.21.2 {
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import org.lwjgl.opengl.GL14;
import org.vmstudio.visor.api.compatibility.mcversion.McVersionUtils;
//?} else {
/*import net.minecraft.client.renderer.ShaderInstance;
*///?}

/**
 * Cross-mc-version handler of a shader program
 */
public final class McShaderProgram {
    private final VertexFormat vertexFormat;
    private final boolean alphaBlend;
    //? if >=1.21.2 {
    private final ShaderProgram program;
    //?} else {
    /*private final ShaderInstance instance;
    *///?}

    private McShaderProgram(String name, VertexFormat vertexFormat, boolean alphaBlend) throws Exception {
        this.vertexFormat = vertexFormat;
        this.alphaBlend = alphaBlend;
        //? if >=1.21.2 {
        this.program = new ShaderProgram(
                McVersionUtils.newResourceLoc("minecraft", "core/" + name),
                vertexFormat,
                ShaderDefines.EMPTY
        );
        Minecraft.getInstance().getShaderManager().getProgramForLoading(program);
        //?} else {
        /*this.instance = new ShaderInstance(Minecraft.getInstance().getResourceManager(), name, vertexFormat);
        *///?}
    }

    public static McShaderProgram core(String name, VertexFormat vertexFormat, boolean alphaBlend) throws Exception {
        return new McShaderProgram(name, vertexFormat, alphaBlend);
    }

    public VertexFormat vertexFormat() {
        return vertexFormat;
    }

    public AbstractUniform uniform(String name) {
        //? if >=1.21.2 {
        return compiled().safeGetUniform(name);
        //?} else {
        /*return instance.safeGetUniform(name);
        *///?}
    }

    public void setModelViewMatrix(Matrix4f matrix) {
        //? if >=1.21.2 {
        Uniform uniform = compiled().MODEL_VIEW_MATRIX;
        //?} else {
        /*Uniform uniform = instance.MODEL_VIEW_MATRIX;
        *///?}
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    public void setProjectionMatrix(Matrix4f matrix) {
        //? if >=1.21.2 {
        Uniform uniform = compiled().PROJECTION_MATRIX;
        //?} else {
        /*Uniform uniform = instance.PROJECTION_MATRIX;
        *///?}
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    public void setSampler(String name, int textureId) {
        //? if >=1.21.2 {
        compiled().bindSampler(name, textureId);
        //?} else {
        /*instance.setSampler(name, textureId);
        *///?}
    }

    public void apply() {
        applyBlend();
        //? if >=1.21.2 {
        compiled().apply();
        //?} else {
        /*instance.apply();
        *///?}
    }

    public void clear() {
        //? if >=1.21.2 {
        compiled().clear();
        //?} else {
        /*instance.clear();
        *///?}
    }

    public void use() {
        applyBlend();
        //? if >=1.21.2 {
        RenderSystem.setShader(program);
        //?} else {
        /*RenderSystem.setShader(() -> instance);
        *///?}
    }

    public RenderStateShard.ShaderStateShard shaderState() {
        //? if >=1.21.2 {
        return new RenderStateShard.ShaderStateShard(program);
        //?} else {
        /*return new RenderStateShard.ShaderStateShard(() -> instance);
        *///?}
    }

    private void applyBlend() {
        //? if >=1.21.2 {
        // the json "blend" block is not read since 1.21.2
        if (alphaBlend) {
            RenderSystem.enableBlend();
            RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        //?}
    }

    //? if >=1.21.2 {
    private CompiledShaderProgram compiled() {
        CompiledShaderProgram compiled = Minecraft.getInstance().getShaderManager().getProgram(program);
        if (compiled == null) {
            throw new IllegalStateException("Shader program failed to compile: " + program.configId());
        }
        return compiled;
    }
    //?}
}
