package com.tetanusmod.world;

import com.tetanusmod.TetanusMod;
import net.minecraft.registry.*;
import net.minecraft.structure.*;
import net.minecraft.world.gen.structure.StructureType;

public final class MedicStructures {
    public static final StructureType<ClinicStructure> CLINIC=Registry.register(Registries.STRUCTURE_TYPE,TetanusMod.id("medic_clinic"),()->ClinicStructure.CODEC);
    public static final StructureType<CaveMedicStructure> TENT=Registry.register(Registries.STRUCTURE_TYPE,TetanusMod.id("medic_tent"),()->CaveMedicStructure.CODEC);
    public static final StructurePieceType PIECE=Registry.register(Registries.STRUCTURE_PIECE,TetanusMod.id("medic_piece"),(context,nbt)->new MedicStructurePiece(context.structureTemplateManager(),nbt));
    public static void initialize(){}
}
