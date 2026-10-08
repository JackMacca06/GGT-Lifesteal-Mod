package io.github.jackmacca06.ggtlifesteal;




import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


public final class WardenRules {
    private WardenRules() {}

    public static void initialize() {
        LootTableEvents.MODIFY_DROPS.register((holder, context, drops) -> {
            if (holder.unwrapKey().map(key -> key.identifier().equals(
                    Identifier.fromNamespaceAndPath("minecraft", "entities/warden"))).orElse(false)) {
                // Exactly one, even when another pool or Looting has added a mace.
                drops.removeIf(stack -> stack.is(Items.MACE));
                drops.add(new ItemStack(Items.MACE));
            }
        });
    }

    public static float projectileDamage(Warden warden, DamageSource source, float amount) {
        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE)
                || source.getDirectEntity() instanceof Projectile;
        return projectile ? amount * 0.05F : amount;
    }
}