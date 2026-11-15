package com.tetanusmod.dragon;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.*;
/** Offline participants receive their earned advancement when they next join. */
public final class DragonRewardState extends PersistentState {
    private final Set<UUID> pending=new HashSet<>();
    public static DragonRewardState get(MinecraftServer server){return server.getOverworld().getPersistentStateManager().getOrCreate(DragonRewardState::read,DragonRewardState::new,"rust_ruin_dragon_rewards");}
    public void add(UUID id){if(pending.size()<4096&&pending.add(id))markDirty();}
    public boolean remove(UUID id){boolean changed=pending.remove(id);if(changed)markDirty();return changed;}
    private static DragonRewardState read(NbtCompound nbt){var state=new DragonRewardState();var list=nbt.getList("Players",10);for(int i=0;i<Math.min(4096,list.size());i++){var row=list.getCompound(i);if(row.containsUuid("Player"))state.pending.add(row.getUuid("Player"));}return state;}
    @Override public NbtCompound writeNbt(NbtCompound nbt){var list=new NbtList();for(var id:pending){var row=new NbtCompound();row.putUuid("Player",id);list.add(row);}nbt.put("Players",list);return nbt;}
}
