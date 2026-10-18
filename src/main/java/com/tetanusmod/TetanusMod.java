package com.tetanusmod;

import com.tetanusmod.effect.TetanusEffect;
import com.tetanusmod.entity.NailEntity;
import com.tetanusmod.item.NailItem;
import com.tetanusmod.item.RebarItem;
import net.fabricmc.api.ModInitializer;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.effect.InfectionState;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class TetanusMod implements ModInitializer {
    public static final String MOD_ID = "tetanusmod";
    public static ModConfig CONFIG = ModConfig.load();
    public static final Identifier CONFIG_PACKET = id("config");
    public static final StatusEffect TETANUS = Registry.register(
            Registries.STATUS_EFFECT, id("tetanus"), new TetanusEffect());
    public static final StatusEffect LOCKJAW=Registry.register(Registries.STATUS_EFFECT,id("lockjaw"),new com.tetanusmod.effect.LockjawEffect());
    public static final StatusEffect SALMONELLA=Registry.register(Registries.STATUS_EFFECT,id("salmonella"),new com.tetanusmod.effect.SalmonellaEffect());
    public static final Item NAIL = ModItems.NAIL;
    public static final Item RUSTED_NAIL = ModItems.RUSTED_NAIL;
    public static final Item REBAR = ModItems.REBAR;
    public static final Item RUSTED_REBAR = ModItems.RUSTED_REBAR;
    public static final net.minecraft.sound.SoundEvent SPASM_SOUND = Registry.register(Registries.SOUND_EVENT,
            id("spasm"), net.minecraft.sound.SoundEvent.of(id("spasm")));
    public static final net.minecraft.sound.SoundEvent INFECTION_SOUND = Registry.register(Registries.SOUND_EVENT,
            id("tetanus_effect"), net.minecraft.sound.SoundEvent.of(id("tetanus_effect")));
    public static final net.minecraft.sound.SoundEvent VOMIT_SOUND=Registry.register(Registries.SOUND_EVENT,id("vomit"),net.minecraft.sound.SoundEvent.of(id("vomit")));
    public static final EntityType<NailEntity> NAIL_ENTITY = Registry.register(
            Registries.ENTITY_TYPE, id("nail"),
            FabricEntityTypeBuilder.<NailEntity>create(SpawnGroup.MISC, NailEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
                    .trackRangeBlocks(64).trackedUpdateRate(10).build());
    public static final EntityType<com.tetanusmod.entity.TetanizedArrowEntity> TETANIZED_ARROW_ENTITY = Registry.register(
            Registries.ENTITY_TYPE, id("tetanized_arrow"), FabricEntityTypeBuilder
                    .<com.tetanusmod.entity.TetanizedArrowEntity>create(SpawnGroup.MISC, com.tetanusmod.entity.TetanizedArrowEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5F,0.5F)).trackRangeBlocks(64).trackedUpdateRate(20).build());

    public static Identifier id(String path) { return new Identifier(MOD_ID, path); }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    @Override
    public void onInitialize() {
        ModItems.initialize();
        com.tetanusmod.registry.ModBrewing.initialize();
        com.tetanusmod.beacon.ModBeacons.initialize();
        com.tetanusmod.entity.RustGolems.initialize();
        com.tetanusmod.effect.InfectionPresence.initialize();
        com.tetanusmod.command.RustRuinCommands.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof net.minecraft.entity.LivingEntity living) com.tetanusmod.entity.MobVariants.loaded(living);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, CONFIG_PACKET)) {
                var packet = PacketByteBufs.create();
                packet.writeString(new com.google.gson.Gson().toJson(CONFIG));
                ServerPlayNetworking.send(handler.player, CONFIG_PACKET, packet);
            }
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getStackInHand(hand);
            if (stack.isFood() && (player.hasStatusEffect(LOCKJAW) || ((InfectionState) player).tetanus$getLockjaw() > 0)) {
                if(!world.isClient)player.sendMessage(net.minecraft.text.Text.translatable("message.tetanusmod.lockjaw"),true);
                return net.minecraft.util.TypedActionResult.fail(stack);
            }
            return net.minecraft.util.TypedActionResult.pass(stack);
        });
    }
}
