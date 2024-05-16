package com.example.examplemod;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Registries {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ExampleMod.MODID);

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ExampleMod.MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ExampleMod.MODID);

    public static final RegistryObject<MenuType<GemPolishingStationMenu>> GEM_POLISHING_STATION_MENU = MENU_TYPES.register("gem_polishing_menu", () -> IForgeMenuType.create((GemPolishingStationMenu::getClientContainer)));

    public static final RegistryObject<Block> GEM_POLISHING_STATION = BLOCKS.register("gem_polishing_station", GemPolishingStationBlock::new);

    public static final RegistryObject<BlockEntityType<GemPolishingStationBlockEntity>> GEM_POLISHING_BE =
            BLOCK_ENTITIES.register("gem_polishing_be", () -> BlockEntityType.Builder.of(GemPolishingStationBlockEntity::new, GEM_POLISHING_STATION.get()).build(null));
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ExampleMod.MODID);

    public static final RegistryObject<RecipeSerializer<GemPolishingRecipe>> GEM_POLISHING_SERIALIZER =
            SERIALIZERS.register("gem_polishing", () -> GemPolishingRecipe.Serializer.INSTANCE);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES,ExampleMod.MODID);

    public static final RegistryObject<RecipeType<GemPolishingRecipe>> GEM_POLISHING = RECIPE_TYPES.register("gem_polishing", GemPolishingRecipe.Type::new);


}


