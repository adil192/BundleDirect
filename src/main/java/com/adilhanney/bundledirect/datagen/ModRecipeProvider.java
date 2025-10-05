package com.adilhanney.bundledirect.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
  public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(output, registries);
  }

  @Override
  protected void buildRecipes(RecipeOutput recipeOutput) {
    ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.BUNDLE)
        .pattern("SLS")
        .pattern("L L")
        .pattern("LLL")
        .define('S', Items.STRING)
        .define('L', Items.LEATHER)
        .unlockedBy("has_leather", has(Items.LEATHER))
        .save(recipeOutput, "bundledirect:bundle_from_leather");

    ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.BUNDLE)
        .pattern("SLS")
        .pattern("L L")
        .pattern("LLL")
        .define('S', Items.STRING)
        .define('L', Items.RABBIT_HIDE)
        .unlockedBy("has_rabbit_hide", has(Items.RABBIT_HIDE))
        .save(recipeOutput, "bundledirect:bundle_from_rabbit_hide");
  }
}
