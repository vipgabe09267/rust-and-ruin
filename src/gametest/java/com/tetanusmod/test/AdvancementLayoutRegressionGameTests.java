package com.tetanusmod.test;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.advancement.Advancement;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.util.*;

/** Native loaded data, ordinary inventory triggers and saved progress compatibility. */
public final class AdvancementLayoutRegressionGameTests implements FabricGameTest {
    private static final List<String> ROOTS=List.of("story/root","adventure/root","husbandry/root","nether/root","end/root");

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void everyVanillaDefinitionAndNormalRootCriterionSurvivesTheMod(TestContext ctx)throws Exception {
        var server=ctx.getWorld().getServer();
        var resources=server.getResourceManager().findResources("advancements",id->id.getNamespace().equals("minecraft")&&id.getPath().endsWith(".json"));
        ctx.assertTrue(resources.size()>1000,"Native vanilla advancement/recipe definitions unexpectedly disappeared: "+resources.size());
        for(var path:resources.keySet()) {
            var id=new Identifier("minecraft",path.getPath().substring("advancements/".length(),path.getPath().length()-5));
            ctx.assertTrue(server.getAdvancementLoader().get(id)!=null,"Vanilla advancement definition failed native loading: "+id);
        }
        String[] triggers={"inventory_changed","player_killed_entity","consume_item","changed_dimension","changed_dimension"};
        String[] criteria={"crafting_table","killed_something","consumed_item","entered_nether","entered_end"};
        for(int i=0;i<ROOTS.size();i++) {
            String path=ROOTS.get(i);
            var resource=server.getResourceManager().getResource(new Identifier("minecraft","advancements/"+path+".json")).orElseThrow();
            try(var reader=resource.getReader()) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                ctx.assertTrue(json.getAsJsonObject("criteria").getAsJsonObject(criteria[i]).get("trigger").getAsString().equals("minecraft:"+triggers[i]),"Native root unlock was replaced: "+path);
            }
            var root=server.getAdvancementLoader().get(new Identifier("minecraft",path));
            ctx.assertTrue(root.getParent()==null&&root.getDisplay()!=null&&!root.getDisplay().isHidden(),"Vanilla root is hidden or reparented: "+path);
            ctx.assertTrue(root.getCriteria().get(criteria[i]).getConditions().getId().equals(new Identifier("minecraft",triggers[i])),"Loaded criterion changed: "+path);
        }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void modDisplayTreeHasBalancedBranchesAndNoOverlappingNodes(TestContext ctx) {
        var loader=ctx.getWorld().getServer().getAdvancementLoader();
        var root=loader.get(TetanusMod.id("root"));
        var displayed=loader.getAdvancements().stream().filter(a->a.getId().getNamespace().equals(TetanusMod.MOD_ID)&&a.getDisplay()!=null).toList();
        ctx.assertTrue(displayed.size()==27,"Active displayed advancement count changed: "+displayed.size());
        int rootBranches=0,leaves=0;
        for(var child:root.getChildren())if(child.getDisplay()!=null)rootBranches++;
        ctx.assertTrue(rootBranches==4,"Flat root fan-out returned: "+rootBranches);
        float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(var a:displayed) {
            ctx.assertTrue(a.getRoot()==root,"Mod advancement left its native tab: "+a.getId());
            var d=a.getDisplay();
            ctx.assertTrue(Float.isFinite(d.getX())&&Float.isFinite(d.getY()),"Non-finite native layout position: "+a.getId());
            minX=Math.min(minX,d.getX());maxX=Math.max(maxX,d.getX());minY=Math.min(minY,d.getY());maxY=Math.max(maxY,d.getY());
            boolean hasChild=false;for(var child:a.getChildren())if(child.getDisplay()!=null)hasChild=true;
            if(!hasChild)leaves++;
        }
        ctx.assertTrue(leaves<=8&&maxY-minY<=11,"Narrow vertical advancement tree returned: leaves="+leaves+", rows="+(maxY-minY));
        ctx.assertTrue(maxX-minX>=6&&maxX-minX<=8,"Expected readable deeper progression branches: columns="+(maxX-minX));
        for(int i=0;i<displayed.size();i++)for(int j=i+1;j<displayed.size();j++) {
            var a=displayed.get(i);var b=displayed.get(j);
            int ax=(int)Math.floor(a.getDisplay().getX()*28),ay=(int)Math.floor(a.getDisplay().getY()*27);
            int bx=(int)Math.floor(b.getDisplay().getX()*28),by=(int)Math.floor(b.getDisplay().getY()*27);
            ctx.assertTrue(Math.abs(ax-bx)>=26||Math.abs(ay-by)>=26,"Native advancement frames overlap: "+a.getId()+" / "+b.getId());
        }
        ctx.assertTrue(loader.get(TetanusMod.id("variant_egg")).getDisplay()==null,"Retired Bad Egg was made visible");
        ctx.assertTrue(loader.get(TetanusMod.id("recipes/unlock")).getDisplay()==null,"Recipe helper was made into a visible achievement");
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("ADVANCEMENT_LAYOUT_NATIVE_PASS nodes={} branches={} leaves={} columns={} rows={}",displayed.size(),rootBranches,leaves,maxX-minX,maxY-minY);
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void ordinaryPickupUnlocksVanillaTabAndReloadRetainsExistingEarnedDates(TestContext ctx) {
        var world=ctx.getWorld();var server=world.getServer();
        var player=new ServerPlayerEntity(server,world,new GameProfile(UUID.randomUUID(),"adv-layout"));
        try {
            server.getPlayerManager().onPlayerConnect(new ClientConnection(NetworkSide.SERVERBOUND),player);
            player.changeGameMode(GameMode.SURVIVAL);player.setNoGravity(true);player.setPosition(ctx.getAbsolute(new Vec3d(3.5,3,3.5)));
            player.getInventory().clear();
            pickup(player,new ItemStack(Items.CRAFTING_TABLE));
            assertDone(ctx,player,new Identifier("minecraft","story/root"));
            pickup(player,new ItemStack(Items.IRON_INGOT));
            assertDone(ctx,player,TetanusMod.id("iron_has_a_darker_side"));
            assertDone(ctx,player,TetanusMod.id("recipes/unlock"));
            pickup(player,new ItemStack(ModItems.RUSTY_FISHING_HOOK));
            assertDone(ctx,player,TetanusMod.id("rusty_hook"));
            var before=dates(player);
            ctx.assertTrue(!before.isEmpty(),"Progress preservation fixture earned nothing");
            player.getAdvancementTracker().save();
            player.getAdvancementTracker().reload(server.getAdvancementLoader());
            var after=dates(player);
            ctx.assertTrue(after.entrySet().containsAll(before.entrySet()),"Native save/reload lost earned criteria or dates: before="+before+", after="+after);
        } finally {
            if(server.getPlayerManager().getPlayerList().contains(player))server.getPlayerManager().remove(player);
            player.discard();
        }
        ctx.complete();
    }

    private static void pickup(ServerPlayerEntity player,ItemStack stack) {
        var item=new ItemEntity(player.getWorld(),player.getX(),player.getY(),player.getZ(),stack);
        item.setPickupDelay(0);
        try {player.getWorld().spawnEntity(item);item.onPlayerCollision(player);if(!item.isRemoved())throw new IllegalStateException("Native pickup did not consume item entity: "+stack);}
        finally {if(!item.isRemoved())item.discard();}
    }
    private static void assertDone(TestContext ctx,ServerPlayerEntity player,Identifier id) {
        var advancement=player.getServer().getAdvancementLoader().get(id);
        ctx.assertTrue(advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Ordinary native pickup did not earn "+id);
    }
    private static Map<String,String> dates(ServerPlayerEntity player) {
        var result=new TreeMap<String,String>();
        for(var advancement:player.getServer().getAdvancementLoader().getAdvancements()) {
            var progress=player.getAdvancementTracker().getProgress(advancement);
            for(String criterion:progress.getObtainedCriteria())result.put(advancement.getId()+"/"+criterion,progress.getCriterionProgress(criterion).toJson().toString());
        }
        return result;
    }
}
