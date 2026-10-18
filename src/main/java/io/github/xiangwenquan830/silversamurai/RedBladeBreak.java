package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.*;

final class RedBladeBreak {
    static boolean frontGuard(LivingEntity attacker, LivingEntity defender) {
        Vec3 toward = attacker.position().subtract(defender.position()).multiply(1, 0, 1);
        return toward.lengthSqr() > .0001 && defender.getViewVector(1).dot(toward.normalize()) > 0;
    }

    static void destroyShield(LivingEntity target, InteractionHand hand, ItemStack original) {
        if (target.level().isClientSide || original.isEmpty()
                || target.getItemInHand(hand) != original) return;
        ItemStack destroyed = original.copy();
        target.stopUsingItem();
        // Destroy exactly one used shield, including unbreakable shields; spare inventory stacks
        // survive.
        original.shrink(1);
        if (original.isEmpty()) target.setItemInHand(hand, ItemStack.EMPTY);
        if (target instanceof Player player)
            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, destroyed, hand);
        target.getPersistentData()
                .putLong(
                        "SilverSamuraiShieldUntil",
                        target.level().getGameTime() + BossConfig.SHIELD.get());
        if (target instanceof Player player) {
            int ticks = BossConfig.SHIELD.get();
            player.getCooldowns().addCooldown(destroyed.getItem(), ticks);
            // Sync cooldowns for carried replacement shields, including modded BLOCK items.
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack spare = player.getInventory().getItem(i);
                if (!spare.isEmpty() && spare.getUseAnimation() == net.minecraft.world.item.UseAnim.BLOCK)
                    player.getCooldowns().addCooldown(spare.getItem(), ticks);
            }
        }
        target.broadcastBreakEvent(hand);
        target.level()
                .playSound(
                        null,
                        target.blockPosition(),
                        SoundEvents.SHIELD_BREAK,
                        SoundSource.PLAYERS,
                        1,
                        .85f);
    }

    private RedBladeBreak() {}
}
