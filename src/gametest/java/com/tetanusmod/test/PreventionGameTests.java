package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.*;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.item.FieldGuideItem;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class PreventionGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void vaccineConsumesOnceReturnsBottleAndDoesNotCure(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();Infection.apply(player,null);int infection=player.getStatusEffect(TetanusMod.TETANUS).getDuration();
        var doses=new ItemStack(ModItems.VACCINE,2);var result=ModItems.VACCINE.finishUsing(doses,ctx.getWorld(),player);
        ctx.assertTrue(result==doses&&doses.getCount()==1&&player.getInventory().count(Items.GLASS_BOTTLE)==1,"Dose or bottle accounting incorrect");
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.VACCINATED)&&player.getStatusEffect(TetanusMod.VACCINATED).getDuration()==24000,"Vaccine duration not 20 minutes");
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.TETANUS)&&player.getStatusEffect(TetanusMod.TETANUS).getDuration()==infection,"Vaccination cured or refreshed existing Tetanus");
        ctx.assertTrue(player.addStatusEffect(new StatusEffectInstance(TetanusMod.SALMONELLA,1200)),"Vaccination incorrectly prevented Salmonella");
        var last=ModItems.VACCINE.finishUsing(doses,ctx.getWorld(),player);
        ctx.assertTrue(last.isOf(Items.GLASS_BOTTLE)&&doses.isEmpty()&&player.getInventory().count(Items.GLASS_BOTTLE)==1,"Last dose duplicated bottles");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void vaccineRejectsEmptyWrongItemAndSpectatorUse(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var fake=new ItemStack(Items.DIAMOND);
        ModItems.VACCINE.finishUsing(fake,ctx.getWorld(),player);ModItems.VACCINE.finishUsing(ItemStack.EMPTY,ctx.getWorld(),player);
        ctx.assertFalse(player.hasStatusEffect(TetanusMod.VACCINATED),"Wrong or empty stack granted vaccination");ctx.assertTrue(fake.getCount()==1,"Wrong item was consumed");
        var spectator=new net.minecraft.entity.player.PlayerEntity(ctx.getWorld(),net.minecraft.util.math.BlockPos.ORIGIN,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"vaccine-spectator")) {
            public boolean isSpectator(){return true;}public boolean isCreative(){return false;}
        };
        var dose=new ItemStack(ModItems.VACCINE);ModItems.VACCINE.finishUsing(dose,ctx.getWorld(),spectator);
        ctx.assertTrue(dose.getCount()==1&&!spectator.hasStatusEffect(TetanusMod.VACCINATED),"Spectator mutated world state");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void vaccineCreativeUseDoesNotCreateBottles(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();var dose=new ItemStack(ModItems.VACCINE);
        ModItems.VACCINE.finishUsing(dose,ctx.getWorld(),player);
        ctx.assertTrue(dose.getCount()==1&&player.getInventory().count(Items.GLASS_BOTTLE)==0&&player.hasStatusEffect(TetanusMod.VACCINATED),"Creative vaccine duplicated a container");player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void exactProtectionBoundaries(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,24000));
        ctx.assertFalse(InfectionProtection.allows(player,false,0.799F),"Vaccine failed below 80%");ctx.assertTrue(InfectionProtection.allows(player,false,0.80F),"Vaccine blocked above its chance");
        ((com.tetanusmod.expansion.GloveInventory)player).rustruin$gloves().setStack(0,new ItemStack(ModItems.PROTECTIVE_GLOVES));
        ctx.assertFalse(InfectionProtection.allows(player,true,0.899F),"Combined prevention failed below 90%");ctx.assertTrue(InfectionProtection.allows(player,true,0.90F),"Combined prevention exceeded 90%");
        ctx.assertTrue(com.tetanusmod.expansion.GloveInventory.equipped(player).getDamage()==2,"Combined exposure did not charge gloves once each");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void glovesRequireDedicatedSlotAndPhysicalContact(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);((com.tetanusmod.expansion.GloveInventory)player).rustruin$gloves().setStack(0,gloves);
        ctx.assertFalse(InfectionProtection.allows(player,true,0.499F),"Gloves failed below 50%");ctx.assertTrue(InfectionProtection.allows(player,true,0.5F),"Gloves exceeded 50%");
        ctx.assertTrue(InfectionProtection.allows(player,false,0)&&gloves.getDamage()==2,"Gloves protected chemical exposure or charged durability");
        ((com.tetanusmod.expansion.GloveInventory)player).rustruin$gloves().setStack(0,ItemStack.EMPTY);player.setStackInHand(Hand.MAIN_HAND,gloves);
        ctx.assertTrue(InfectionProtection.allows(player,true,0)&&gloves.getDamage()==2,"Main-hand gloves gave protection");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void lastGloveUseBreaksAndStopsProtection(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.setDamage(gloves.getMaxDamage()-1);((com.tetanusmod.expansion.GloveInventory)player).rustruin$gloves().setStack(0,gloves);
        ctx.assertFalse(InfectionProtection.allows(player,true,0),"Last glove durability gave no protection");
        ctx.assertTrue(com.tetanusmod.expansion.GloveInventory.equipped(player).isEmpty()&&InfectionProtection.allows(player,true,0),"Broken gloves still protected or did not break");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void preventionStopsPotionAndPermanentMutation(TestContext ctx) {
        float chance=TetanusMod.CONFIG.vaccineProtectionChance,animal=TetanusMod.CONFIG.animalConversionChance;
        try {
            TetanusMod.CONFIG.vaccineProtectionChance=1;TetanusMod.CONFIG.animalConversionChance=1;
            var cow=net.minecraft.entity.EntityType.COW.create(ctx.getWorld());cow.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,24000));
            ctx.assertFalse(cow.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS,6000)),"Direct potion/status bypassed vaccination");
            Infection.expose(cow,null);ctx.assertTrue(!cow.hasStatusEffect(TetanusMod.TETANUS)&&!MobVariants.permanent(cow),"Blocked exposure still mutated the animal");
        }finally{TetanusMod.CONFIG.vaccineProtectionChance=chance;TetanusMod.CONFIG.animalConversionChance=animal;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void blockedRefreshPreservesExistingIllness(TestContext ctx) {
        float chance=TetanusMod.CONFIG.vaccineProtectionChance;
        try {
            var player=ctx.createMockSurvivalPlayer();Infection.apply(player,null);var state=(InfectionState)player;state.tetanus$setInfection(500,8000);int duration=player.getStatusEffect(TetanusMod.TETANUS).getDuration();
            player.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,24000));TetanusMod.CONFIG.vaccineProtectionChance=1;
            ctx.assertFalse(Infection.apply(player,null),"Vaccinated refresh was not blocked");
            ctx.assertTrue(player.getStatusEffect(TetanusMod.TETANUS).getDuration()==duration&&state.tetanus$getElapsed()==500,"Blocked hit cured or restarted existing illness");
        }finally{TetanusMod.CONFIG.vaccineProtectionChance=chance;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void glovesDoNotProtectFoodOrSalmonella(TestContext ctx) {
        float glove=TetanusMod.CONFIG.gloveProtectionChance,salmonella=TetanusMod.CONFIG.salmonellaChance;
        try {
            TetanusMod.CONFIG.gloveProtectionChance=1;TetanusMod.CONFIG.salmonellaChance=1;
            var player=ctx.createMockSurvivalPlayer();((com.tetanusmod.expansion.GloveInventory)player).rustruin$gloves().setStack(0,new ItemStack(ModItems.PROTECTIVE_GLOVES));
            Infection.expose(player,null);ctx.assertFalse(player.hasStatusEffect(TetanusMod.TETANUS),"Gloves did not prevent contact infection");
            ModItems.INFECTED_MEAT.finishUsing(new ItemStack(ModItems.INFECTED_MEAT),ctx.getWorld(),player);
            ctx.assertTrue(!player.hasStatusEffect(TetanusMod.TETANUS)&&player.hasStatusEffect(TetanusMod.SALMONELLA)&&com.tetanusmod.expansion.GloveInventory.equipped(player).getDamage()==1,"Gloves prevented food infection or charged twice");
        }finally{TetanusMod.CONFIG.gloveProtectionChance=glove;TetanusMod.CONFIG.salmonellaChance=salmonella;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void protectionPersistsAndMilkRemovesOnlyPrevention(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();Infection.apply(player,null);player.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,12000));
        var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.setDamage(31);gloves.addEnchantment(Enchantments.UNBREAKING,1);((com.tetanusmod.expansion.GloveInventory)player).rustruin$gloves().setStack(0,gloves);
        var copy=ctx.createMockSurvivalPlayer();copy.readNbt(player.writeNbt(new NbtCompound()));
        ctx.assertTrue(copy.hasStatusEffect(TetanusMod.VACCINATED)&&copy.getStatusEffect(TetanusMod.VACCINATED).getDuration()==12000&&com.tetanusmod.expansion.GloveInventory.equipped(copy).getDamage()==31&&com.tetanusmod.expansion.GloveInventory.equipped(copy).hasEnchantments(),"Saved protection or equipment data was lost");
        Items.MILK_BUCKET.finishUsing(new ItemStack(Items.MILK_BUCKET),ctx.getWorld(),copy);
        ctx.assertTrue(!copy.hasStatusEffect(TetanusMod.VACCINATED)&&copy.hasStatusEffect(TetanusMod.TETANUS),"Milk cure rules changed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void naturalVaccineExpiryAndForgedNbt(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var dose=new ItemStack(ModItems.VACCINE);dose.getOrCreateNbt().putInt("VaccineDuration",Integer.MAX_VALUE);
        ModItems.VACCINE.finishUsing(dose,ctx.getWorld(),player);ctx.assertTrue(player.getStatusEffect(TetanusMod.VACCINATED).getDuration()==24000,"Item NBT forged a longer effect");
        player.removeStatusEffect(TetanusMod.VACCINATED);player.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,2));player.tick();player.tick();
        ctx.assertFalse(player.hasStatusEffect(TetanusMod.VACCINATED),"Vaccination did not expire naturally");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void checkedScopeCannotLeakAfterException(TestContext ctx) {
        var first=ctx.createMockSurvivalPlayer();var second=ctx.createMockSurvivalPlayer();
        try{InfectionProtection.checked(first,()->{
            ctx.assertTrue(InfectionProtection.checked(first)&&!InfectionProtection.checked(second),"Scope leaked to another entity");
            InfectionProtection.checked(second,()->{ctx.assertTrue(InfectionProtection.checked(second)&&!InfectionProtection.checked(first),"Nested scope was not isolated");return true;});
            ctx.assertTrue(InfectionProtection.checked(first),"Nested scope failed to restore its parent");throw new IllegalStateException("fixture");
        });}catch(IllegalStateException expected){}
        ctx.assertTrue(!InfectionProtection.checked(first)&&!InfectionProtection.checked(second),"Exception leaked verified protection to later attacks");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void craftPreventionItemsAndReturnExactlyTwoBottles(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var grid=new CraftingInventory(new CraftingScreenHandler(1,player.getInventory()),3,3);
        Item[] inputs={ModItems.ANTITOXIN,Items.GOLDEN_APPLE,Items.GLOWSTONE_DUST,Items.HONEY_BOTTLE};for(int i=0;i<4;i++)grid.setStack(i,new ItemStack(inputs[i]));
        var recipe=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).orElseThrow();
        ctx.assertTrue(recipe.craft(grid,ctx.getWorld().getRegistryManager()).isOf(ModItems.VACCINE),"Vaccine recipe did not match");
        ctx.assertTrue(recipe.getRemainder(grid).stream().filter(stack->stack.isOf(Items.GLASS_BOTTLE)).mapToInt(ItemStack::getCount).sum()==2,"Vaccine crafting did not return exactly two bottles");
        grid.clear();for(int i:new int[]{0,2})grid.setStack(i,new ItemStack(Items.LEATHER));for(int i:new int[]{3,5})grid.setStack(i,new ItemStack(Items.IRON_NUGGET));grid.setStack(4,new ItemStack(Items.COPPER_INGOT));grid.setStack(7,new ItemStack(Items.STRING));
        recipe=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).orElseThrow();ctx.assertTrue(recipe.craft(grid,ctx.getWorld().getRegistryManager()).isOf(ModItems.PROTECTIVE_GLOVES),"Glove recipe failed");
        grid.clear();grid.setStack(0,new ItemStack(Items.BOOK));grid.setStack(4,new ItemStack(ModItems.RUST));grid.setStack(8,new ItemStack(Items.FEATHER));
        recipe=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).orElseThrow();ctx.assertTrue(recipe.craft(grid,ctx.getWorld().getRegistryManager()).isOf(ModItems.FIELD_GUIDE),"Guide recipe failed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void guideUseHasNoServerSideEffects(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.FIELD_GUIDE));var old=FieldGuideItem.open;final boolean[] opened={false};
        try {FieldGuideItem.open=()->opened[0]=true;ModItems.FIELD_GUIDE.use(ctx.getWorld(),player,Hand.MAIN_HAND);ctx.assertTrue(!opened[0]&&player.getMainHandStack().getCount()==1&&player.getStatusEffects().isEmpty(),"Guide executed client UI or mutated server state");}
        finally{FieldGuideItem.open=old;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void hooklineCannotBypassCooldown(TestContext ctx) {
        for(int x=2;x<=4;x++)for(int y=3;y<=5;y++)for(int z=1;z<=6;z++)ctx.setBlockState(x,y,z,net.minecraft.block.Blocks.AIR);
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3.5,3,1.5)));player.setYaw(0);player.setPitch(0);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTY_HOOKLINE));
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3.5,5.5));cow.setAiDisabled(true);cow.setNoGravity(true);
        ctx.runAtTick(2,()->{ModItems.RUSTY_HOOKLINE.use(ctx.getWorld(),player,Hand.MAIN_HAND);float health=cow.getHealth();cow.timeUntilRegen=0;ModItems.RUSTY_HOOKLINE.use(ctx.getWorld(),player,Hand.MAIN_HAND);ctx.assertTrue(cow.getHealth()==health&&player.getMainHandStack().getDamage()==1,"Repeated use bypassed hookline cooldown");ctx.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void vaccineCanBeDrunkDuringLockjawThroughNormalUseTicks(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();Infection.apply(player,null);((InfectionState)player).tetanus$setLockjaw(2400);LockjawEffect.flush(player);
        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.VACCINE,2));ModItems.VACCINE.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        ctx.assertTrue(player.isUsingItem(),"Lockjaw prevented starting a vaccine drink");
        for(int i=0;i<34;i++)player.tick();
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.VACCINATED)&&player.hasStatusEffect(TetanusMod.TETANUS)&&player.getMainHandStack().getCount()==1&&player.getInventory().count(Items.GLASS_BOTTLE)==1,"Normal drinking did not complete exactly one dose during Lockjaw");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void fullInventoryDropsExactlyOneDoseBottle(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);player.getInventory().clear();player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));
        for(int slot=0;slot<36;slot++)player.getInventory().setStack(slot,new ItemStack(Items.STONE,64));
        var doses=new ItemStack(ModItems.VACCINE,2);player.setStackInHand(Hand.MAIN_HAND,doses);ModItems.VACCINE.finishUsing(doses,ctx.getWorld(),player);
        ctx.runAtTick(3,()->{
            int bottles=ctx.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,player.getBoundingBox().expand(3),item->item.getStack().isOf(Items.GLASS_BOTTLE)).stream().mapToInt(item->item.getStack().getCount()).sum();
            ctx.assertTrue(doses.getCount()==1&&bottles==1,"Full inventory bottle accounting: doses="+doses.getCount()+" dropped="+bottles);player.discard();ctx.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void hooklineCannotReachThroughSolidWalls(TestContext ctx) {
        for(int x=2;x<=4;x++)for(int y=3;y<=5;y++)for(int z=1;z<=6;z++)ctx.setBlockState(x,y,z,net.minecraft.block.Blocks.AIR);
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3.5,3,1.5)));player.setYaw(0);player.setPitch(0);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTY_HOOKLINE));
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3.5,5.5));cow.setAiDisabled(true);cow.setNoGravity(true);
        ctx.setBlockState(3,4,3,net.minecraft.block.Blocks.STONE);
        ctx.runAtTick(2,()->{ModItems.RUSTY_HOOKLINE.use(ctx.getWorld(),player,Hand.MAIN_HAND);ctx.assertTrue(cow.getHealth()==10&&player.getMainHandStack().getDamage()==0&&!cow.hasStatusEffect(TetanusMod.TETANUS),"Hookline attacked through a wall");ctx.complete();});
    }
}
