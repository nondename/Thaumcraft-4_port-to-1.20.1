package thaumcraft.common.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class BlockFluxGoo extends Block {
    public BlockFluxGoo() {
        super(BlockBehaviour.Properties.of()
                .strength(0.5F)
                .sound(SoundType.SLIME_BLOCK));
    }
}
