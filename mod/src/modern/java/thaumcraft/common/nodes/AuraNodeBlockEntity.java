package thaumcraft.common.nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

/** Persistent current/base vis, type and modifier for a TC4-style aura node. */
public final class AuraNodeBlockEntity extends BlockEntity implements INode {
    private final AspectList aspects = new AspectList();
    private final AspectList baseAspects = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier;
    private String nodeId = "";

    public AuraNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModNodes.AURA_NODE_ENTITY.get(), pos, state);
    }

    public void initialize(NodeType type, NodeModifier modifier, AspectList generatedAspects) {
        nodeType = type == null ? NodeType.NORMAL : type;
        nodeModifier = modifier;
        aspects.aspects.clear();
        baseAspects.aspects.clear();
        if (generatedAspects != null) {
            aspects.add(generatedAspects);
            baseAspects.add(generatedAspects);
        }
        ensureNodeId();
        setChangedAndSync();
    }

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public AspectList getBaseAspects() {
        return baseAspects;
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType type) {
        nodeType = type == null ? NodeType.NORMAL : type;
        setChangedAndSync();
    }

    @Override
    public NodeModifier getNodeModifier() {
        return nodeModifier;
    }

    public void setNodeModifier(NodeModifier modifier) {
        nodeModifier = modifier;
        setChangedAndSync();
    }

    @Override
    public String getNodeId() {
        ensureNodeId();
        return nodeId;
    }

    /** ResourceLocation-safe, stable per-world-position discovery key. */
    public String getScanKey() {
        ensureNodeId();
        return nodeId.replace(':', '/').replace(',', '/').replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }

    private void ensureNodeId() {
        if (!nodeId.isEmpty()) return;
        String dimension = level == null ? "unknown" : level.dimension().location().toString();
        nodeId = dimension + ":" + worldPosition.getX() + ":" + worldPosition.getY() + ":" + worldPosition.getZ();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ensureNodeId();
        tag.putString("nodeId", nodeId);
        tag.putString("type", nodeType.name());
        tag.putString("modifier", nodeModifier == null ? "" : nodeModifier.name());
        aspects.writeToNBT(tag, "Aspects");
        baseAspects.writeToNBT(tag, "AspectsBase");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nodeId = tag.getString("nodeId");
        nodeType = parseType(tag.getString("type"));
        nodeModifier = parseModifier(tag.getString("modifier"));
        aspects.readFromNBT(tag, "Aspects");
        baseAspects.readFromNBT(tag, "AspectsBase");
        if (baseAspects.size() == 0 && aspects.size() > 0) {
            baseAspects.add(aspects);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static NodeType parseType(String value) {
        try {
            return NodeType.valueOf(value);
        } catch (Exception ignored) {
            return NodeType.NORMAL;
        }
    }

    private static NodeModifier parseModifier(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return NodeModifier.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
