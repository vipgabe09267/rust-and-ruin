package com.tetanusmod.expansion;

import com.tetanusmod.registry.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;

public final class VariantSpawnEggItem extends Item {
    private final EntityType<?> type;private final boolean rabid;
    public VariantSpawnEggItem(EntityType<?> type,boolean rabid){super(new Item.Settings());this.type=type;this.rabid=rabid;}
    public boolean rabid(){return rabid;}
    public EntityType<?> entityType(){return type;}
    public int color(int tint){
        var original=SpawnEggItem.forEntity(type);
        if(original==null)original=SpawnEggItem.forEntity(type==EntityType.WITHER?EntityType.WITHER_SKELETON:type==EntityType.ENDER_DRAGON?EntityType.ENDERMAN:EntityType.ZOMBIE);
        int base=original==null?0xAAAAAA:original.getColor(tint);
        int overlay=rabid?0x913C94:0xA85F31;
        float strength=rabid?(tint==0?0.30F:0.50F):(tint==0?0.24F:0.48F);
        int r=(int)(((base>>16)&255)*(1-strength)+((overlay>>16)&255)*strength);
        int g=(int)(((base>>8)&255)*(1-strength)+((overlay>>8)&255)*strength);
        int b=(int)((base&255)*(1-strength)+(overlay&255)*strength);
        return r<<16|g<<8|b;
    }
    @Override public Text getName(ItemStack stack){return Text.translatable(rabid?"item.tetanusmod.rabid_spawn_egg":"item.tetanusmod.tetanized_spawn_egg",type.getName());}
    public boolean spawn(ServerWorld world,PlayerEntity player,ItemStack stack,BlockPos pos){
        if(stack.isEmpty()||!stack.isOf(this)||player.isSpectator()||!world.getWorldBorder().contains(pos)||!world.canPlayerModifyAt(player,pos))return false;
        var entity=type.create(world);if(!(entity instanceof MobEntity mob))return false;
        mob.refreshPositionAndAngles(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,world.random.nextFloat()*360,0);
        mob.initialize(world,world.getLocalDifficulty(pos),SpawnReason.SPAWN_EGG,null,null);
        if(rabid)Rabies.makeRabid(mob);else com.tetanusmod.entity.MobVariants.makePermanent(mob);
        if(!world.spawnEntity(mob))return false;
        if(!player.getAbilities().creativeMode)stack.decrement(1);
        // Variant eggs are Creative tools; they no longer award a survival advancement.
        return true;
    }
    @Override public ActionResult useOnBlock(ItemUsageContext context){
        var player=context.getPlayer();if(player==null||player.isSpectator()||!player.canPlaceOn(context.getBlockPos().offset(context.getSide()),context.getSide(),context.getStack()))return ActionResult.FAIL;
        if(context.getWorld().isClient)return ActionResult.SUCCESS;
        var pos=context.getWorld().getBlockState(context.getBlockPos()).isReplaceable()?context.getBlockPos():context.getBlockPos().offset(context.getSide());
        return spawn((ServerWorld)context.getWorld(),player,context.getStack(),pos)?ActionResult.CONSUME:ActionResult.FAIL;
    }
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
        var stack=player.getStackInHand(hand);var hit=raycast(world,player,net.minecraft.world.RaycastContext.FluidHandling.SOURCE_ONLY);
        if(hit.getType()!=HitResult.Type.BLOCK)return TypedActionResult.pass(stack);
        if(!(world.getBlockState(hit.getBlockPos()).getBlock() instanceof net.minecraft.block.FluidBlock))return TypedActionResult.pass(stack);
        if(!world.canPlayerModifyAt(player,hit.getBlockPos())||!player.canPlaceOn(hit.getBlockPos(),hit.getSide(),stack))return TypedActionResult.fail(stack);
        if(world.isClient)return TypedActionResult.success(stack);
        return spawn((ServerWorld)world,player,stack,hit.getBlockPos())?TypedActionResult.consume(stack):TypedActionResult.fail(stack);
    }
    public static void registerAll(){
        for(var type:Registries.ENTITY_TYPE){
            var id=Registries.ENTITY_TYPE.getId(type);if(!id.getNamespace().equals("minecraft"))continue;
            if(type.getSpawnGroup()==SpawnGroup.MISC&&!Set.of(EntityType.VILLAGER,EntityType.WANDERING_TRADER,EntityType.IRON_GOLEM,EntityType.SNOW_GOLEM,EntityType.WITHER,EntityType.ENDER_DRAGON).contains(type))continue;
            ModItems.register("tetanized_"+id.getPath()+"_spawn_egg",new VariantSpawnEggItem(type,false));
            if(Set.of("wolf","fox","cat","bat","rabbit","cow","pig","sheep","goat","horse","donkey","mule","llama","trader_llama","camel","panda","polar_bear","hoglin","zoglin","zombie","husk","drowned").contains(id.getPath()))ModItems.register("rabid_"+id.getPath()+"_spawn_egg",new VariantSpawnEggItem(type,true));
        }
    }
}