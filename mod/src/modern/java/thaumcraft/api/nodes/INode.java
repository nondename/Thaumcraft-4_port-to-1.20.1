package thaumcraft.api.nodes;

import thaumcraft.api.aspects.AspectList;

/** Minimal TC4 node contract shared by scanning, rendering and wand interaction. */
public interface INode {
    AspectList getAspects();

    AspectList getBaseAspects();

    NodeType getNodeType();

    NodeModifier getNodeModifier();

    String getNodeId();
}
