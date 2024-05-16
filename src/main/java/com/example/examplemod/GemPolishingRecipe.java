package com.example.examplemod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.example.examplemod.Registries.GEM_POLISHING;

public class GemPolishingRecipe implements Recipe<SimpleContainer> {
    private final NonNullList<Ingredient> inputItems;
    private final ItemStack result;
    protected final String group;
    final CraftingBookCategory category;
    private final boolean showNotification;

    public GemPolishingRecipe(NonNullList<Ingredient> inputItems, ItemStack result, String group, CraftingBookCategory category, boolean showNotification) {
        this.inputItems = inputItems;
        this.result = result;
        this.group = group;
        this.category = category;
        this.showNotification = showNotification;
    }

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        if (pLevel.isClientSide()) {
            return false;
        }

        return inputItems.get(0).test(pContainer.getItem(0));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputItems;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SimpleContainer pContainer, @NotNull RegistryAccess pRegistryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return result.copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GEM_POLISHING.get();
    }

    public static class Type implements RecipeType<GemPolishingRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "gem_polishing";
    }

    public static class Serializer implements RecipeSerializer<GemPolishingRecipe> {
        public static final Serializer INSTANCE = new Serializer();



        public static final Codec<GemPolishingRecipe> CODEC = RecordCodecBuilder.create((builder) ->
                builder.group(
                        Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter((recipe)->recipe.inputItems),
                        ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter((recipe) -> recipe.result),
                        ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter((recipe) -> recipe.group),

                        CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter((recipe) -> recipe.category),

                        ExtraCodecs.strictOptionalField(Codec.BOOL, "show_notification", true).forGetter((recipe) -> recipe.showNotification))
                        .apply(builder, (List<Ingredient> inputItems1, ItemStack result, String group, CraftingBookCategory group1, Boolean category1) -> new GemPolishingRecipe(NonNullList.of(Ingredient.EMPTY,inputItems1.toArray(new Ingredient[0])), result,group, group1, category1)));

        @Override
        public Codec<GemPolishingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @Nullable GemPolishingRecipe fromNetwork(FriendlyByteBuf friendlyByteBuf) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(friendlyByteBuf.readInt(), Ingredient.EMPTY);
            inputs.replaceAll(ignored -> Ingredient.fromNetwork(friendlyByteBuf));
            ItemStack result = friendlyByteBuf.readItem();
            String group = friendlyByteBuf.readUtf();
            CraftingBookCategory craftingbookcategory = friendlyByteBuf.readEnum(CraftingBookCategory.class);
            boolean showNotification = friendlyByteBuf.readBoolean();
            return new GemPolishingRecipe(inputs,result,group,craftingbookcategory,showNotification);
        }


        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, GemPolishingRecipe pRecipe) {
            pBuffer.writeInt(pRecipe.inputItems.size());
            for (Ingredient ingredient : pRecipe.getIngredients()) {
                ingredient.toNetwork(pBuffer);
            }
            pBuffer.writeItemStack(pRecipe.getResultItem(null), false);
            pBuffer.writeUtf(pRecipe.group);
            pBuffer.writeEnum(pRecipe.category);
            pBuffer.writeBoolean(pRecipe.showNotification);
        }
    }
}