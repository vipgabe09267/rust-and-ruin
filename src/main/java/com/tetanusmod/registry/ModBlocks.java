package com.tetanusmod.registry;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.block.CaltropsBlock;
import com.tetanusmod.block.CaltropsBlockEntity;
import com.tetanusmod.item.TooltipHelper;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class ModBlocks {
    public static final Block CALTROPS = register("caltrops", false);
    public static final Block RUSTED_CALTROPS = register("rusted_caltrops", true);
    public static final Block BARBED_WIRE=register("barbed_wire",false);
    public static final Block RUSTED_BARBED_WIRE=register("rusted_barbed_wire",true);
    public static final Block TETANIZED_SOUL_SAND=Registry.register(Registries.BLOCK,TetanusMod.id("tetanized_soul_sand"),
            new net.minecraft.block.SoulSandBlock(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.SOUL_SAND)));
    public static final Item TETANIZED_SOUL_SAND_ITEM=ModItems.register("tetanized_soul_sand",new BlockItem(TETANIZED_SOUL_SAND,new Item.Settings()));
    public static final BlockEntityType<CaltropsBlockEntity> CALTROPS_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, TetanusMod.id("caltrops"), FabricBlockEntityTypeBuilder
                    .create(CaltropsBlockEntity::new, CALTROPS, RUSTED_CALTROPS,BARBED_WIRE,RUSTED_BARBED_WIRE).build());

    private static Block register(String name, boolean rusty) {
        Block block = Registry.register(Registries.BLOCK, TetanusMod.id(name), new CaltropsBlock(rusty,
                FabricBlockSettings.create().strength(0.7F).sounds(BlockSoundGroup.METAL).nonOpaque()));
        ModItems.register(name, new BlockItem(block, new Item.Settings()) {
            @Override public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                tooltip.add(Text.translatable("tooltip.tetanusmod.trap"));
                TooltipHelper.metal(tooltip, rusty);
            }
        });
        return block;
    }
    public static void initialize() { }
}
