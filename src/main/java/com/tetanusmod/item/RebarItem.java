package com.tetanusmod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.tetanusmod.TetanusMod;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class RebarItem extends Item {
    private final boolean rusty;
    private final float damage;
    private final boolean pipe;

    public RebarItem(boolean rusty, float damage, boolean pipe, Settings settings) {
        super(settings);
        this.rusty = rusty;
        this.damage = damage;
        this.pipe = pipe;
    }

    private float totalDamage() { return damage == 1 ? TetanusMod.CONFIG.rebarDamage : damage; }
    public boolean isRusty() { return rusty; }

    private Multimap<EntityAttribute, EntityAttributeModifier> attributes() {
        // Subtract the player's base 1 HP, so this is total damage, not bonus damage.
        return ImmutableMultimap.<EntityAttribute, EntityAttributeModifier>builder()
                .put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID,
                        "Industrial weapon damage", totalDamage() - 1.0, EntityAttributeModifier.Operation.ADDITION))
                .put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID,
                        "Rebar speed", -2.8, EntityAttributeModifier.Operation.ADDITION)).build();
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attributes() : super.getAttributeModifiers(slot);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient) {
            if (rusty) {
                com.tetanusmod.rust.RustingSystem.particles(target.getWorld(), target.getPos().add(0, 0.7, 0));
            }
            if (pipe) target.takeKnockback(0.65, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            target.getWorld().playSound(null, target.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_CHAIN_HIT,
                    net.minecraft.sound.SoundCategory.PLAYERS, 0.7F, pipe ? 0.65F : 1.0F);
            stack.damage(1, attacker, entity -> entity.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (TooltipHelper.details()) {
            tooltip.add(Text.translatable("tooltip.tetanusmod.durability", stack.getMaxDamage() - stack.getDamage(), stack.getMaxDamage()).formatted(Formatting.GRAY));
        }
        TooltipHelper.metal(tooltip, rusty);
    }
}
