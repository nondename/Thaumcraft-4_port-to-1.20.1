package thaumcraft.common.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class BlockTaint extends Block {
    public BlockTaint() {
        super(BlockBehaviour.Properties.of()
                .strength(1.5F, 3.0F)
                .sound(SoundType.SLIME_BLOCK)
                .randomTicks());
    }
}
