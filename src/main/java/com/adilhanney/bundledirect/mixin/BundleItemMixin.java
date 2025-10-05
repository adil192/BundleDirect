package com.adilhanney.bundledirect.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(BundleItem.class)
public abstract class BundleItemMixin extends Item {
  public BundleItemMixin(Properties properties) {
    super(properties);
  }

  @Inject(method = "use", at = @At("HEAD"), cancellable = true)
  public void use(Level level, Player player, InteractionHand usedHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
    if (!player.isShiftKeyDown()) {
      final ItemStack itemstack = player.getItemInHand(usedHand);
      cir.setReturnValue(InteractionResultHolder.fail(itemstack));
    }
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    final Player player = context.getPlayer();
    if (player == null || player.isShiftKeyDown()) {
      return super.useOn(context);
    }

    final ItemStack bundleItemStack = context.getItemInHand();
    final BundleContents bundleContents = bundleItemStack.get(DataComponents.BUNDLE_CONTENTS);
    if (bundleContents == null || bundleContents.isEmpty()) {
      return super.useOn(context);
    }

    final Level level = context.getLevel();
    InteractionResult result = InteractionResult.FAIL;
    final List<Integer> availableIndexes = bundleDirect$containedBlockIndexes(bundleContents);
    while (!availableIndexes.isEmpty() && result != InteractionResult.sidedSuccess(level.isClientSide)) {
      final RandomSource random = level.getRandom();
      // TODO(adil192): Upstream this bug fix
      final int randomIndex = availableIndexes.get(random.nextInt(availableIndexes.size()));
      final ItemStack randomItemStack = bundleContents.getItemUnsafe(randomIndex);
      final BlockItem randomItem = (BlockItem) randomItemStack.getItem();

      result = randomItem.useOn(new UseOnContext(
          level, player, context.getHand(),
          // Copy so it doesn't decrement item count before we're ready.
          randomItemStack.copy(),
          new BlockHitResult(
              context.getClickLocation(),
              context.getClickedFace(),
              context.getClickedPos(),
              context.isInside()
          )
      ));
      if (result == InteractionResult.sidedSuccess(level.isClientSide)) {
        if (!level.isClientSide && !player.isCreative()) {
          bundleDirect$removeItem(player, bundleItemStack, randomIndex);
        }
        return result;
      }

      availableIndexes.remove(randomIndex);
    }

    return super.useOn(context);
  }

  @Unique
  private List<Integer> bundleDirect$containedBlockIndexes(BundleContents bundleContents) {
    final List<Integer> availableIndexes = new ArrayList<>();
    for (int i = 0; i < bundleContents.size(); i++) {
      final ItemStack itemStack = bundleContents.getItemUnsafe(i);
      if (itemStack.getItem() instanceof BlockItem) {
        availableIndexes.add(i);
      }
    }
    return availableIndexes;
  }

  @Unique
  private void bundleDirect$removeItem(Player player, ItemStack bundleItemStack, int index) {
    BundleContents bundleContents = bundleItemStack.get(DataComponents.BUNDLE_CONTENTS);
    if (bundleContents == null || bundleContents.isEmpty()) return;

    final List<ItemStack> stacks = new ArrayList<>(bundleContents.itemCopyStream().toList());
    final ItemStack itemStack = bundleContents.getItemUnsafe(index).copy();
    final ItemStack itemStackCopy = itemStack.copy();

    itemStack.setCount(itemStack.getCount() - 1);
    if (!itemStack.isEmpty()) {
      stacks.set(index, itemStack);
      bundleContents = new BundleContents(stacks);
    } else {
      stacks.remove(index);
      bundleContents = new BundleContents(stacks);
      final Pair<BundleContents, Boolean> result = bundleDirect$tryRefillItemStack(player, itemStackCopy, bundleContents);
      bundleContents = result.getFirst();
      if (!result.getSecond()) {
        player.displayClientMessage(
            Component.translatable("text.bundledirect.bundle.out_of_item", itemStackCopy.getDisplayName()),
            true
        );
      }
    }

    bundleItemStack.set(DataComponents.BUNDLE_CONTENTS, bundleContents);
  }

  @Unique
  private Pair<BundleContents, Boolean> bundleDirect$tryRefillItemStack(Player player, ItemStack itemStack, BundleContents bundleContents) {
    final Inventory inventory = player.getInventory();
    for (int i = 0; i < 36; ++i) {
      final ItemStack inventoryItemStack = inventory.getItem(i);
      if (ItemStack.isSameItemSameComponents(inventoryItemStack, itemStack)) {
        final BundleContents.Mutable builder = new BundleContents.Mutable(bundleContents);
        final int itemsAdded = builder.tryInsert(inventoryItemStack);
        if (itemsAdded > 0) {
          inventory.removeItem(i, itemsAdded);
          return new Pair<>(builder.toImmutable(), true);
        }
      }
    }
    return new Pair<>(bundleContents, false);
  }
}
