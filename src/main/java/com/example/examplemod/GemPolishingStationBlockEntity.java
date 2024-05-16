package com.example.examplemod;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.example.examplemod.Registries.GEM_POLISHING;
import static com.example.examplemod.Registries.GEM_POLISHING_BE;

public class GemPolishingStationBlockEntity extends BlockEntity {

    private final Map<Direction, LazyOptional<IItemHandlerModifiable>> INVENTORY = new HashMap<>();

    private final Map<Direction,ItemStackHandler> HANDLERS = new HashMap<>();

    private final ItemStackHandler UP_HANDLER = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };
    private final ItemStackHandler DOWN_HANDLER = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    };

    protected final SimpleContainerData data;
    private int progress = 0;
    private int maxProgress = 78;

    public GemPolishingStationBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(GEM_POLISHING_BE.get(), pPos, pBlockState);
        HANDLERS.put(Direction.UP,UP_HANDLER);
        HANDLERS.put(Direction.DOWN,DOWN_HANDLER);
        this.data = new SimpleContainerData(2) {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> GemPolishingStationBlockEntity.this.progress;
                    case 1 -> GemPolishingStationBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> GemPolishingStationBlockEntity.this.progress = pValue;
                    case 1 -> GemPolishingStationBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        INVENTORY.put(Direction.DOWN,LazyOptional.of(()->DOWN_HANDLER));
        INVENTORY.put(Direction.UP,LazyOptional.of(()->UP_HANDLER));
    }



    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return INVENTORY.containsKey(side) ? INVENTORY.get(side).cast() : super.getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        INVENTORY.values().forEach(LazyOptional::invalidate);
    }

    private SimpleContainer makeSimpleInventory() {
       List<ItemStack> stacks = new ArrayList<>();
       HANDLERS.values().forEach(handler -> {
           for(int i=0;i<handler.getSlots();++i){
               stacks.add(handler.getStackInSlot(i));
           }
       });
        SimpleContainer inventory = new SimpleContainer(stacks.toArray(new ItemStack[0]));
        return inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        CompoundTag inventoryTag = new CompoundTag();
        INVENTORY.entrySet().stream().filter(entry -> entry.getValue().resolve().isPresent()).map(entry -> Pair.of(entry.getKey(), entry.getValue())).forEach(pair -> {
            if (pair.getSecond().resolve().isPresent() && pair.getSecond().resolve().get() instanceof ItemStackHandler handler) {
                inventoryTag.put(pair.getFirst().toString(), handler.serializeNBT());
            }
        });
        pTag.put("inventory", inventoryTag);
        pTag.putInt("gem_polishing_station.progress", progress);

        super.saveAdditional(pTag);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        CompoundTag inventoryTag = pTag.getCompound("inventory");
        Arrays.stream(Direction.values()).filter(direction -> inventoryTag.get(direction.toString()) != null).forEach(direction -> {
            if(direction == Direction.UP){
                UP_HANDLER.deserializeNBT(inventoryTag.getCompound(direction.toString()));
            }else if(direction == Direction.DOWN){
                DOWN_HANDLER.deserializeNBT(inventoryTag.getCompound(direction.toString()));
            }
        });
        progress = pTag.getInt("gem_polishing_station.progress");
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (hasRecipe()) {
            increaseCraftingProgress();
            setChanged(pLevel, pPos, pState);

            if (hasProgressFinished()) {
                craftItem();
                resetProgress();
            }
        } else {
            resetProgress();
        }
    }

    private void resetProgress() {
        progress = 0;
    }

    private void craftItem() {
        Optional<RecipeHolder<GemPolishingRecipe>> recipe = getCurrentRecipe();
        ItemStack result = recipe.get().value().getResultItem(null);

        recipe.get().value().getIngredients().stream().map(Ingredient::getItems).forEach(itemStacks -> {
            for(ItemStack item : itemStacks){
                for(int i=0;i<UP_HANDLER.getSlots();++i){
                    if(item.is(UP_HANDLER.getStackInSlot(i).getItem())){
                        UP_HANDLER.extractItem(i,item.getCount(),false);
                        break;
                    }
                }
            }
        });
        DOWN_HANDLER.setStackInSlot(0, new ItemStack(result.getItem(), result.getCount()+DOWN_HANDLER.getStackInSlot(0).getCount()));
    }
    public boolean isItemValidForRecipe(int slot,ItemStack result){
        ItemStack stack = DOWN_HANDLER.getStackInSlot(slot);
        return stack.isEmpty() || (stack.is(result.getItem()) && (result.getCount() + stack.getCount() <= stack.getMaxStackSize()));
    }
    private boolean hasRecipe() {
        Optional<RecipeHolder<GemPolishingRecipe>> recipe = getCurrentRecipe();

        if (recipe.isEmpty()) {
            return false;
        }
        ItemStack result = recipe.get().value().getResultItem(Objects.requireNonNull(getLevel()).registryAccess());

        return isItemValidForRecipe(0,result);
    }

    private Optional<RecipeHolder<GemPolishingRecipe>> getCurrentRecipe() {
        if (level == null) return Optional.empty();
        SimpleContainer inventory = makeSimpleInventory();

        return this.level.getRecipeManager().getRecipeFor(GEM_POLISHING.get(), inventory, level);
    }


    private boolean hasProgressFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    public Map<Direction, LazyOptional<IItemHandlerModifiable>> getInventory() {
        return INVENTORY;
    }
    public ItemStack getRenderStack() {
        ItemStack renderStack = ItemStack.EMPTY;
        for (int i = 0; i < UP_HANDLER.getSlots(); ++i) {
            if(!UP_HANDLER.getStackInSlot(i).isEmpty()){
                return UP_HANDLER.getStackInSlot(i);
            }
        }
        for (int i = 0; i < DOWN_HANDLER.getSlots(); ++i) {
            if(!DOWN_HANDLER.getStackInSlot(i).isEmpty()){
                return DOWN_HANDLER.getStackInSlot(i);
            }
        }
        return renderStack;
    }
    public void drops() {
        SimpleContainer inventory = makeSimpleInventory();
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}