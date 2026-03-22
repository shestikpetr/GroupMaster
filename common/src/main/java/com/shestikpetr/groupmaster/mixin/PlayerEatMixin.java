package com.shestikpetr.groupmaster.mixin;

import com.shestikpetr.groupmaster.GroupMasterServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PlayerEatMixin {

    @Inject(method = "eat", at = @At("RETURN"))
    private void groupmaster$onEat(Level level, ItemStack food, CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof ServerPlayer sp && GroupMasterServer.getInstance() != null) {
            GroupMasterServer.getInstance().getEventBonusManager().onEat(sp);
        }
    }
}
