package com.tetanusmod.dragon;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import java.util.function.Supplier;

/** The original explosion source stays untouched; only synchronous dragon contribution is attributed. */
public final class DragonExplosionAttribution {
    private record Context(ServerPlayerEntity player,World world,DamageSource source) { }
    private static final ThreadLocal<Context> CURRENT=new ThreadLocal<>();
    private DragonExplosionAttribution() { }
    public static <T> T run(PlayerEntity player,World world,DamageSource source,Supplier<T> action) {
        Context previous=CURRENT.get();
        if(world instanceof ServerWorld&&player instanceof ServerPlayerEntity actor&&actor.getWorld()==world&&actor.isAlive()
                &&(source==null||source.isOf(DamageTypes.BAD_RESPAWN_POINT)))CURRENT.set(new Context(actor,world,source));
        else CURRENT.remove();
        try { return action.get(); }
        finally { if(previous==null)CURRENT.remove();else CURRENT.set(previous); }
    }
    public static <T> T runCurrent(World world,DamageSource source,Supplier<T> action) {
        Context parent=CURRENT.get();
        return run(parent!=null&&parent.world==world?parent.player:null,world,source,action);
    }
    public static ServerPlayerEntity player(DamageSource source,World world) {
        Context context=CURRENT.get();
        return context!=null&&source!=null&&context.source==source&&context.world==world&&context.player.getWorld()==world
                &&source.isOf(DamageTypes.BAD_RESPAWN_POINT)?context.player:null;
    }
}
