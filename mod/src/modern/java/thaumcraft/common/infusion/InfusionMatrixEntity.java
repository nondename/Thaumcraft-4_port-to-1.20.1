package thaumcraft.common.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.tags.BlockTags;
import thaumcraft.api.aspects.*;
import thaumcraft.common.research.ResearchProgression;
import java.util.*;

/** Persisted ritual: suck essentia first, then ingredients, finally replace the centre. */
public final class InfusionMatrixEntity extends BlockEntity {
    private String recipeId="";private UUID owner;
    private AspectList remaining=new AspectList();private List<BlockPos> pedestals=List.of(),jars=List.of();
    private ItemStack input=ItemStack.EMPTY;private int component,clock;
    public InfusionMatrixEntity(BlockPos pos,BlockState state) {super(ModInfusion.MATRIX_ENTITY.get(),pos,state);}
    public boolean active() {return !recipeId.isEmpty();}
    public boolean structure() {
        if(level==null || !(level.getBlockEntity(worldPosition.below(2)) instanceof PedestalEntity))return false;
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})for(int y:new int[]{-1,-2})
            if(!level.getBlockState(worldPosition.offset(x,y,z)).is(ModInfusion.ARCANE_STONE.get()))return false;
        return true;
    }
    private static List<BlockPos> assign(InfusionRecipe recipe,List<PedestalEntity> stores) {
        if(stores.size()!=recipe.components().size())return List.of();
        var unused=new ArrayList<>(stores);var positions=new ArrayList<BlockPos>();
        for(var ingredient:recipe.components()) {
            var match=unused.stream().filter(p -> ingredient.test(p.item())).findFirst().orElse(null);
            if(match==null)return List.of();positions.add(match.getBlockPos());unused.remove(match);
        }
        return List.copyOf(positions);
    }
    public boolean start(Player player) {
        if(level==null || level.isClientSide || player.level()!=level || active() || !structure() || !ResearchProgression.has(player,"INFUSION"))return false;
        var centre=(PedestalEntity)level.getBlockEntity(worldPosition.below(2));
        if(centre.item().getCount()!=1)return false;
        var stores=new ArrayList<PedestalEntity>();
        for(BlockPos p:BlockPos.betweenClosed(worldPosition.offset(-4,-2,-4),worldPosition.offset(4,-2,4)))
            if(!p.equals(centre.getBlockPos()) && level.getBlockEntity(p) instanceof PedestalEntity pedestal && !pedestal.item().isEmpty())stores.add(pedestal);
        for(var recipe:level.getRecipeManager().getAllRecipesFor(InfusionRecipe.TYPE.get()).stream().sorted(Comparator.comparing(r -> r.id().toString())).toList()) {
            if(!recipe.input().test(centre.item()) || !ResearchProgression.has(player,recipe.research()))continue;
            var plan=assign(recipe,stores);if(plan.isEmpty())continue;
            var sources=new ArrayList<BlockPos>();var available=new AspectList();
            for(BlockPos p:BlockPos.betweenClosed(worldPosition.offset(-12,-5,-12),worldPosition.offset(12,5,12)))
                if(level.isLoaded(p) && level.getBlockEntity(p) instanceof EssentiaStoreEntity store && store.aspect()!=null) {
                    available.add(store.aspect(),store.amount());sources.add(p.immutable());
                }
            boolean sufficient=true;for(Aspect a:recipe.aspects().getAspects())if(available.getAmount(a)<recipe.aspects().getAmount(a))sufficient=false;
            if(!sufficient)return false;
            recipeId=recipe.id().toString();owner=player.getUUID();remaining=recipe.aspects().copy();pedestals=plan;jars=List.copyOf(sources);
            input=centre.item().copy();component=0;clock=0;level.setBlock(worldPosition,getBlockState().setValue(InfusionDeviceBlock.ACTIVE,true),3);setChanged();return true;
        }
        return false;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,InfusionMatrixEntity matrix) {
        if(!matrix.active() || ++matrix.clock%5!=0)return;
        var player=matrix.owner==null?null:level.getPlayerByUUID(matrix.owner);
        // An offline owner pauses the ritual rather than bypassing research or losing it.
        if(player==null)return;
        matrix.advance(player);
    }
    /** One server ritual step, also usable by mechanical tests without ticking unrelated entities. */
    public void advance(Player player) {
        var matrix=this;var level=getLevel();var pos=getBlockPos();
        if(level==null || level.isClientSide || !active() || player.level()!=level || !player.getUUID().equals(owner))return;
        var recipe=level.getRecipeManager().byKey(ResourceLocation.tryParse(matrix.recipeId)).filter(r -> r instanceof InfusionRecipe).map(r -> (InfusionRecipe)r).orElse(null);
        if(recipe==null || !matrix.structure() || !ResearchProgression.has(player,recipe.research())) {matrix.stop();return;}
        var centre=(PedestalEntity)level.getBlockEntity(pos.below(2));
        if(!ItemStack.matches(centre.item(),matrix.input)) {matrix.stop();return;}
        if(matrix.pedestals.size()!=recipe.components().size()) {matrix.stop();return;}
        // A modest explicit instability model; balanced pedestals/candles can eliminate risk.
        int balance=0;
        for(var p:matrix.pedestals)if(level.getBlockEntity(new BlockPos(2*pos.getX()-p.getX(),p.getY(),2*pos.getZ()-p.getZ())) instanceof PedestalEntity)balance++;
        for(BlockPos p:BlockPos.betweenClosed(pos.offset(-4,-3,-4),pos.offset(4,-2,4)))if(level.getBlockState(p).is(BlockTags.CANDLES))balance++;
        int risk=Math.max(0,recipe.instability()-balance);
        if(risk>0 && level.random.nextInt(200)<risk) {
            ((ServerLevel)level).sendParticles(ParticleTypes.WITCH,pos.getX()+.5,pos.getY(),pos.getZ()+.5,8,.8,.8,.8,.02);
            player.hurt(level.damageSources().magic(),1);
            thaumcraft.common.alchemy.FluxSpill.attempt((ServerLevel)level,pos.below(2));
        }
        if(matrix.remaining.visSize()>0) {
            var aspect=matrix.remaining.getAspects()[0];boolean took=false;
            for(var p:matrix.jars)if(level.isLoaded(p) && level.getBlockEntity(p) instanceof EssentiaStoreEntity store && store.take(aspect,1)) {
                matrix.remaining.remove(aspect,1);took=true;
                ((ServerLevel)level).sendParticles(ParticleTypes.ENCHANT,p.getX()+.5,p.getY()+.6,p.getZ()+.5,2,.1,.1,.1,0);break;
            }
            if(took)matrix.setChanged();return;
        }
        if(matrix.component<recipe.components().size()) {
            var p=matrix.pedestals.get(matrix.component);
            if(!level.isLoaded(p) || !(level.getBlockEntity(p) instanceof PedestalEntity pedestal)
                    || !recipe.components().get(matrix.component).test(pedestal.item())) {matrix.stop();return;}
            var left=pedestal.item().copy();left.shrink(1);pedestal.set(left);matrix.component++;matrix.setChanged();return;
        }
        centre.set(recipe.output());matrix.stop();
    }
    private void stop() {
        recipeId="";owner=null;remaining=new AspectList();pedestals=List.of();jars=List.of();input=ItemStack.EMPTY;component=0;setChanged();
        if(level!=null && !level.isClientSide && getBlockState().hasProperty(InfusionDeviceBlock.ACTIVE))level.setBlock(worldPosition,getBlockState().setValue(InfusionDeviceBlock.ACTIVE,false),3);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putString("Recipe",recipeId);if(owner!=null)tag.putUUID("Owner",owner);remaining.writeToNBT(tag,"Remaining");
        tag.put("Input",input.save(new CompoundTag()));tag.putInt("Component",component);
        tag.putLongArray("Pedestals",pedestals.stream().mapToLong(BlockPos::asLong).toArray());tag.putLongArray("Jars",jars.stream().mapToLong(BlockPos::asLong).toArray());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);recipeId=tag.getString("Recipe");owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;
        remaining=new AspectList();remaining.readFromNBT(tag,"Remaining");input=ItemStack.of(tag.getCompound("Input"));component=Math.max(0,tag.getInt("Component"));
        pedestals=Arrays.stream(tag.getLongArray("Pedestals")).limit(16).mapToObj(BlockPos::of).toList();jars=Arrays.stream(tag.getLongArray("Jars")).limit(512).mapToObj(BlockPos::of).toList();
        if(ResourceLocation.tryParse(recipeId)==null || owner==null)stop();
    }
}
