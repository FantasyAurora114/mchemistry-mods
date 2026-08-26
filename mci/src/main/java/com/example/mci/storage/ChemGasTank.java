/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.storage.ValueInput
 *  net.minecraft.world.level.storage.ValueOutput
 */
package com.example.mci.storage;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ChemGasTank {
    private final long capacity;
    private String gasId = "";
    private long amount = 0L;
    private double purity = 1.0;

    public ChemGasTank(long capacity) {
        this.capacity = Math.max(0L, capacity);
    }

    public long getCapacity() {
        return this.capacity;
    }

    public String getGasId() {
        return this.gasId;
    }

    public long getAmount() {
        return this.amount;
    }

    public boolean isEmpty() {
        return this.amount <= 0L || this.gasId.isEmpty();
    }

    public long getSpace() {
        return this.capacity - this.amount;
    }

    public double getPurity() {
        return this.purity;
    }

    public boolean isGas(String id) {
        return !this.isEmpty() && this.gasId.equals(id);
    }

    public long insert(String id, long add, boolean simulate) {
        if (add <= 0L || id == null || id.isEmpty()) {
            return 0L;
        }
        if (!this.isEmpty() && !this.gasId.equals(id)) {
            return 0L;
        }
        long inserted = Math.min(add, this.getSpace());
        if (inserted <= 0L) {
            return 0L;
        }
        if (!simulate) {
            if (this.isEmpty()) {
                this.gasId = id;
            }
            this.amount += inserted;
        }
        return inserted;
    }

    public long extract(long take, boolean simulate) {
        if (take <= 0L || this.isEmpty()) {
            return 0L;
        }
        long removed = Math.min(take, this.amount);
        if (removed <= 0L) {
            return 0L;
        }
        if (!simulate) {
            this.amount -= removed;
            if (this.amount <= 0L) {
                this.amount = 0L;
                this.gasId = "";
            }
        }
        return removed;
    }

    public long extract(String id, long take, boolean simulate) {
        return this.isGas(id) ? this.extract(take, simulate) : 0L;
    }

    public long addWithPurity(String id, long add, double newPurity) {
        long oldAmount = this.amount;
        long inserted = this.insert(id, add, false);
        if (inserted <= 0L) {
            return 0L;
        }
        this.purity = oldAmount == 0L ? newPurity : (this.purity * (double)oldAmount + newPurity * (double)inserted) / (double)(oldAmount + inserted);
        return inserted;
    }

    public void setContents(String id, long amount, double purity) {
        this.gasId = id == null ? "" : id;
        this.amount = Math.max(0L, Math.min(this.capacity, amount));
        this.purity = purity;
    }

    public void serialize(ValueOutput output, String key) {
        ValueOutput child = output.child(key);
        child.putString("gas", this.gasId);
        child.putLong("amount", this.amount);
        child.putDouble("purity", this.purity);
    }

    public void deserialize(ValueInput input, String key) {
        input.child(key).ifPresent(child -> {
            this.gasId = child.getStringOr("gas", "");
            this.amount = Math.max(0L, Math.min(this.capacity, child.getLongOr("amount", 0L)));
            this.purity = child.getDoubleOr("purity", 1.0);
        });
    }
}
