package thaumcraft.common.research;

import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import thaumcraft.api.aspects.AspectList;
import java.util.List;

public final class EssentiaPhial extends Item {
    public EssentiaPhial(Properties properties) {super(properties);}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        if(stack.hasTag()) {var payload=new AspectList();payload.readFromNBT(stack.getTag());for(var a:payload.getAspects())lines.add(Component.literal(a.getName()+" × "+payload.getAmount(a)));}
    }
}
