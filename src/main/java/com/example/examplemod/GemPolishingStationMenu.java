package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.example.examplemod.Registries.GEM_POLISHING_STATION;
import static com.example.examplemod.Registries.GEM_POLISHING_STATION_MENU;

public class GemPolishingStationMenu extends AbstractContainerMenu {
    private final SimpleContainerData data;

    public static final Component TITLE = Component.translatable("container.expandable_chest");
    private final BlockPos pos;

    public static MenuConstructor getServerContainerConstructor(GemPolishingStationBlockEntity blockEntity, BlockPos activationPos) {
        return (id, playerInventory, serverPlayer) -> new GemPolishingStationMenu(id, playerInventory, blockEntity.getInventory(), blockEntity.data, activationPos, serverPlayer, null);
    }

    public static GemPolishingStationMenu getClientContainer(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        // init client inventory with dummy slots
        return new GemPolishingStationMenu(id, playerInventory, Map.of(Direction.UP,LazyOptional.of(()->new ItemStackHandler(1)),Direction.DOWN,LazyOptional.of(()->new ItemStackHandler(1))), new SimpleContainerData(2), BlockPos.ZERO, playerInventory.player, buffer);
    }

    public GemPolishingStationMenu(int windowID, Inventory playerInventory, Map<Direction, LazyOptional<IItemHandlerModifiable>> gemStationInventory, SimpleContainerData data, BlockPos pos, Player player, FriendlyByteBuf buffer) {
        super(GEM_POLISHING_STATION_MENU.get(), windowID);
        this.addDataSlots(data);
        this.data = data;
        if(buffer!=null){
            this.setData(0, buffer.readInt());
            this.setData(1, buffer.readInt());
        }
        this.pos = pos;
        addPlayerHotbar(new InvWrapper(playerInventory));
        addPlayerInventory(new InvWrapper(playerInventory));
        if(!gemStationInventory.isEmpty()){
            addGemStationInventory(gemStationInventory);
        }


    }

    private void addGemStationInventory(Map<Direction, LazyOptional<IItemHandlerModifiable>> gemStationInventory) {
        this.addSlot(new SlotItemHandler(gemStationInventory.get(Direction.UP).orElseThrow(RuntimeException::new), 0, 80, 11));
        this.addSlot(new SlotItemHandler(gemStationInventory.get(Direction.DOWN).orElseThrow(RuntimeException::new), 0, 80, 59));
    }


    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);  // Max Progress
        int progressArrowSize = 26; // This is the height in pixels of your arrow

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 2;  // must be the number of slots you have!

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int pIndex) {
        if (pIndex >= 36) {
            moveItemStackTo(this.getItems().get(pIndex), 0, 35, false);
        } else {
            moveItemStackTo(this.getItems().get(pIndex), 36, 37, false);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(pPlayer.level(), pos),
                pPlayer, GEM_POLISHING_STATION.get());
    }

    private void addPlayerHotbar(IItemHandlerModifiable playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new SlotItemHandler(playerInventory, i, 8 + i * 18, 142));
        }
    }
    private void addPlayerInventory(IItemHandlerModifiable playerInventory) {
        for (int rows = 0; rows < 3; ++rows) {
            for (int columns = 0; columns < 9; ++columns) {
                this.addSlot(new SlotItemHandler(playerInventory, columns + rows * 9 + 9, 8 + columns * 18, 84 + rows * 18));
            }
        }
    }
}