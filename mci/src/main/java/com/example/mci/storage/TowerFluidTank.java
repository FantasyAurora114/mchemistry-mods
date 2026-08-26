/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.material.Fluid
 *  net.neoforged.neoforge.fluids.FluidStack
 *  net.neoforged.neoforge.transfer.fluid.FluidResource
 *  net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler
 */
package com.example.mci.storage;

import com.example.mci.blockentity.SynthesisTowerBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

public class TowerFluidTank
extends FluidStacksResourceHandler {
    private final SynthesisTowerBlockEntity blockEntity;
    private double purity = 1.0;

    public TowerFluidTank(SynthesisTowerBlockEntity blockEntity, int capacity) {
        super(1, capacity);
        this.blockEntity = blockEntity;
    }

    public FluidStack getFluidStack() {
        return (FluidStack)this.stacks.get(0);
    }

    public Fluid getFluid() {
        return this.getFluidStack().getFluid();
    }

    public boolean isEmpty() {
        return this.getFluidStack().isEmpty();
    }

    public int getAmount() {
        return this.getFluidStack().getAmount();
    }

    public double getPurity() {
        return this.purity;
    }

    public void setPurity(double purity) {
        this.purity = purity;
    }

    public int getSpace() {
        return Math.max(0, this.getCapacity(0, FluidResource.EMPTY) - this.getAmount());
    }

    public boolean canAccept(Fluid fluid, int amount) {
        if (amount <= 0) {
            return false;
        }
        if (this.isEmpty()) {
            return amount <= this.getCapacity(0, FluidResource.of((Fluid)fluid));
        }
        return this.getFluidStack().is(fluid) && this.getSpace() >= amount;
    }

    public int addWithPurity(FluidStack stack, double newPurity) {
        FluidStack current = this.getFluidStack();
        if (current.isEmpty()) {
            int accepted = Math.min(stack.getAmount(), this.getCapacity(0, FluidResource.of((FluidStack)stack)));
            if (accepted <= 0) {
                return 0;
            }
            this.stacks.set(0, stack.copyWithAmount(accepted));
            this.purity = newPurity;
            this.onContentsChanged(0, current);
            return accepted;
        }
        if (!current.is(stack.getFluid())) {
            return 0;
        }
        int accepted = Math.min(stack.getAmount(), this.getCapacity(0, FluidResource.of((FluidStack)stack)) - current.getAmount());
        if (accepted <= 0) {
            return 0;
        }
        int oldAmount = current.getAmount();
        this.stacks.set(0, current.copyWithAmount(oldAmount + accepted));
        this.purity = (this.purity * (double)oldAmount + newPurity * (double)accepted) / (double)(oldAmount + accepted);
        this.onContentsChanged(0, current);
        return accepted;
    }

    public FluidStack take(Fluid fluid, int amount) {
        FluidStack current = this.getFluidStack();
        if (current.isEmpty() || !current.is(fluid)) {
            return FluidStack.EMPTY;
        }
        int taken = Math.min(amount, current.getAmount());
        if (taken <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack ret = current.copyWithAmount(taken);
        if (taken >= current.getAmount()) {
            this.stacks.set(0, FluidStack.EMPTY);
        } else {
            this.stacks.set(0, current.copyWithAmount(current.getAmount() - taken));
        }
        this.onContentsChanged(0, current);
        return ret;
    }

    protected void onContentsChanged(int index, FluidStack previousContents) {
        if (this.blockEntity != null) {
            this.blockEntity.setChanged();
        }
    }
}
