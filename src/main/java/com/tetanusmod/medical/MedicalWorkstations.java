package com.tetanusmod.medical;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.item.DescribedItem;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

/** Registration is isolated from the optional recipe-viewer and client classes. */
public final class MedicalWorkstations {
    public static final Item PRESCRIPTION=ModItems.register("medical_prescription",new DescribedItem(new Item.Settings(),"medical_prescription"));
    public static final Block WRITER=Registry.register(Registries.BLOCK,TetanusMod.id("prescription_writer"),new PrescriptionWriterBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).strength(3).nonOpaque()));
    public static final Item WRITER_ITEM=ModItems.register("prescription_writer",new BlockItem(WRITER,new Item.Settings()));
    public static final ScreenHandlerType<PrescriptionWriterScreenHandler> SCREEN=Registry.register(Registries.SCREEN_HANDLER,TetanusMod.id("prescription_writer"),new ScreenHandlerType<>(PrescriptionWriterScreenHandler::new,FeatureFlags.VANILLA_FEATURES));
    private MedicalWorkstations(){}
    public static void initialize(){PrescriptionWritingRecipe.initialize();}
}
