package com.adilhanney.bundledirect.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(BreedGoal.class)
public abstract class BreedGoalMixin extends Goal {
  @Unique
  private static final int RABBIT_EXTRA_CHILDREN = 3;

  @Shadow
  @Final
  protected Animal animal;
  @Shadow
  @Final
  protected Level level;
  @Shadow
  @Nullable
  protected Animal partner;

  @Inject(method = "breed", at = @At("HEAD"))
  private void breedRabbits(CallbackInfo ci) {
    if (this.animal instanceof Rabbit) {
      for (int i = 0; i < level.getRandom().nextInt(RABBIT_EXTRA_CHILDREN); ++i) {
        assert partner != null;
        this.animal.spawnChildFromBreeding((ServerLevel) level, partner);
      }
    }
  }
}
