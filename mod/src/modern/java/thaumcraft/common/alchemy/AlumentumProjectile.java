package thaumcraft.common.alchemy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.HitResult;

public final class AlumentumProjectile extends ThrowableItemProjectile {
    public AlumentumProjectile(EntityType<? extends AlumentumProjectile> type,Level level) {super(type,level);}
    public AlumentumProjectile(Level level,LivingEntity owner) {super(ModAlchemyEntities.ALUMENTUM.get(),owner,level);}
    @Override protected Item getDefaultItem() {return ModAlchemy.ALUMENTUM.get();}
    @Override protected float getGravity() {return 0.03F;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) {
            for(int i=0;i<3;i++) level().addParticle(ParticleTypes.FLAME,
                    getX()+(random.nextDouble()-.5)*.3,getY()+(random.nextDouble()-.5)*.3,
                    getZ()+(random.nextDouble()-.5)*.3,0,0,0);
        }
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if(!level().isClientSide && !isRemoved()) {
            level().explode(null,getX(),getY(),getZ(),1.66F,
                    level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)?Level.ExplosionInteraction.MOB:Level.ExplosionInteraction.NONE);
            discard();
        }
    }
}
