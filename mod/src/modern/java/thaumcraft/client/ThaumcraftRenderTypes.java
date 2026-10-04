package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Render states used by the TC4 node effects.
 *
 * <p>TC4 renders node layers with {@code glBlendFunc(GL_SRC_ALPHA, blend)}, where blend=1 is
 * additive and blend=771 is ordinary alpha. The built-in 1.20.1 see-through text type fixed the
 * depth test but changed that blend mode, turning nodes into hard flat discs. These types preserve
 * both the old blending and the old depth-test behaviour.</p>
 */
public final class ThaumcraftRenderTypes extends RenderType {
    private static final ResourceLocation NODE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/nodes.png");
    private static final ResourceLocation NODE_LIGHTNING_LARGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/p_large.png");
    private static final ResourceLocation NODE_LIGHTNING_SMALL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/p_small.png");

    private static final RenderType NODE_ADDITIVE = nodeType("node_additive", true, false);
    private static final RenderType NODE_ADDITIVE_SEE_THROUGH = nodeType("node_additive_see_through", true, true);
    private static final RenderType NODE_TRANSLUCENT = nodeType("node_translucent", false, false);
    private static final RenderType NODE_TRANSLUCENT_SEE_THROUGH = nodeType("node_translucent_see_through", false, true);

    // Kept for compatibility with any older call sites that still submit POSITION_COLOR vertices.
    private static final RenderType NODE_LIGHTNING = create(
            Thaumcraft.MODID + ":node_lightning",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(false)
    );

    // TC4 renders every bolt twice: p_large.png in dark purple and p_small.png in pink-white.
    private static final RenderType NODE_LIGHTNING_LARGE =
            nodeLightningType("node_lightning_large", NODE_LIGHTNING_LARGE_TEXTURE);
    private static final RenderType NODE_LIGHTNING_SMALL =
            nodeLightningType("node_lightning_small", NODE_LIGHTNING_SMALL_TEXTURE);

    private ThaumcraftRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                  boolean affectsCrumbling, boolean sortOnUpload,
                                  Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    private static RenderType nodeType(String name, boolean additive, boolean seeThrough) {
        return create(
                Thaumcraft.MODID + ":" + name,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(NODE_TEXTURE, false, false))
                        .setTransparencyState(additive ? ADDITIVE_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(seeThrough ? NO_DEPTH_TEST : LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(false)
        );
    }

    private static RenderType nodeLightningType(String name, ResourceLocation texture) {
        return create(
                Thaumcraft.MODID + ":" + name,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                512,
                false,
                true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(false)
        );
    }

    public static RenderType node(boolean additive, boolean seeThrough) {
        if (additive) {
            return seeThrough ? NODE_ADDITIVE_SEE_THROUGH : NODE_ADDITIVE;
        }
        return seeThrough ? NODE_TRANSLUCENT_SEE_THROUGH : NODE_TRANSLUCENT;
    }

    public static RenderType nodeLightning() {
        return NODE_LIGHTNING;
    }

    public static RenderType nodeLightning(boolean innerPass) {
        return innerPass ? NODE_LIGHTNING_SMALL : NODE_LIGHTNING_LARGE;
    }
}
