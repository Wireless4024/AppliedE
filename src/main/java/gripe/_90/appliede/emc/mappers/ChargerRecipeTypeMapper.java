package gripe._90.appliede.emc.mappers;

import net.minecraft.world.item.crafting.RecipeType;

import appeng.recipes.handlers.ChargerRecipe;

import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.emc.mappers.recipe.BaseRecipeTypeMapper;

@SuppressWarnings("unused")
@RecipeTypeMapper
public class ChargerRecipeTypeMapper extends BaseRecipeTypeMapper {
    @Override
    public String getName() {
        return "AE2Charger";
    }

    @Override
    public String getDescription() {
        return "Maps Applied Energistics 2 charger recipes.";
    }

    @Override
    public boolean canHandle(RecipeType<?> recipeType) {
        return ChargerRecipe.TYPE.equals(recipeType);
    }
}
