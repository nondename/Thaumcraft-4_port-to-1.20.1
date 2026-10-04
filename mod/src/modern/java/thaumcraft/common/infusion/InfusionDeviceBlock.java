package thaumcraft.common.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.api.aspects.*;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.research.ModResearch;

public final class InfusionDeviceBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE=net.minecraft.world.level.block.state.properties.BooleanProperty.create("active");
    private final String kind;
    public InfusionDeviceBlock(String kind,Properties properties) {super(properties);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder) {builder.add(ACTIVE);}
    @Override public RenderShape getRenderShape(BlockState state) {return kind.equals("matrix") && state.getValue(ACTIVE)?RenderShape.ENTITYBLOCK_ANIMATED:RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return switch(kind) {case "jar" -> box(3,0,3,13,14,13);case "alembic" -> box(2,0,2,14,12,14);
            case "matrix" -> box(2,2,2,14,14,14);case "pedestal" -> Shapes.or(box(2,0,2,14,3,14),box(5,3,5,11,12,11),box(1,12,1,15,16,15));default -> Shapes.block();};
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return switch(kind) {case "jar","alembic" -> new EssentiaStoreEntity(pos,state);case "pedestal" -> new PedestalEntity(pos,state);
            case "matrix" -> new InfusionMatrixEntity(pos,state);default -> new AlchemyFurnaceEntity(pos,state);};
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        if(level.isClientSide)return null;
        return switch(kind) {case "furnace" -> createTickerHelper(type,ModInfusion.FURNACE_ENTITY.get(),AlchemyFurnaceEntity::tick);
            case "matrix" -> createTickerHelper(type,ModInfusion.MATRIX_ENTITY.get(),InfusionMatrixEntity::tick);default -> null;};
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        var held=player.getItemInHand(hand);var entity=level.getBlockEntity(pos);
        if(entity instanceof PedestalEntity pedestal) {
            if(!level.isClientSide) {
                if(pedestal.item().isEmpty() && !held.isEmpty()) {pedestal.set(held.copyWithCount(1));if(!player.getAbilities().instabuild)held.shrink(1);}
                else if(held.isEmpty()) {var out=pedestal.item().copy();pedestal.set(ItemStack.EMPTY);if(!player.getInventory().add(out))player.drop(out,false);}
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(entity instanceof InfusionMatrixEntity matrix && held.is(ModItems.WAND.get())) {
            if(!level.isClientSide)player.displayClientMessage(Component.translatable(matrix.start(player)?"tc.infusion.started":"tc.infusion.failed"),true);return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(entity instanceof AlchemyFurnaceEntity furnace) {
            if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,ignored) ->
                    new AlchemyMenu(id,inventory,furnace,ContainerLevelAccess.create(level,pos)),Component.translatable("block.thaumcraft.alchemy_furnace")),pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(entity instanceof EssentiaStoreEntity store) {
            if(!level.isClientSide) {
                if(held.is(ModResearch.PHIAL.get())) {
                    var payload=new AspectList(); if(held.hasTag())payload.readFromNBT(held.getTag());
                    var output=new ItemStack(ModResearch.PHIAL.get());
                    if(payload.size()==0 && store.amount()>=8) {
                        var aspect=store.aspect();store.take(aspect,8);new AspectList().add(aspect,8).writeToNBT(output.getOrCreateTag());
                    } else if(payload.size()==1 && payload.visSize()==8 && store.add(payload.getAspects()[0],8)) {
                        // Full phial becomes an empty one; no partial transfers or remainder loss.
                    } else return InteractionResult.PASS;
                    if(!player.getAbilities().instabuild)held.shrink(1);
                    if(!player.getInventory().add(output))player.drop(output,false);
                } else player.displayClientMessage(Component.literal(store.aspect()==null?"0":store.aspect().getName()+" "+store.amount()+" / "+store.capacity()),true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!replacement.is(this) && !level.isClientSide) {
            var entity=level.getBlockEntity(pos);
            if(entity instanceof PedestalEntity pedestal)popResource(level,pos,pedestal.item().copy());
            if(entity instanceof AlchemyFurnaceEntity furnace)Containers.dropContents(level,pos,furnace);
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
}
