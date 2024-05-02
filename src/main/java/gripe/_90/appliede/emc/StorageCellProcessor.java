package gripe._90.appliede.emc;

import java.math.BigInteger;

import org.jetbrains.annotations.NotNull;

import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;

import gripe._90.appliede.AppliedE;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.nbt.INBTProcessor;
import moze_intel.projecte.api.nbt.NBTProcessor;
import moze_intel.projecte.api.proxy.IEMCProxy;

@SuppressWarnings("unused")
@NBTProcessor
public class StorageCellProcessor implements INBTProcessor {
    @Override
    public String getName() {
        return "AE2CellProcessor";
    }

    @Override
    public String getDescription() {
        return "(AppliedE) Calculates EMC value of Applied Energistics 2 item cells depending on contents.";
    }

    @Override
    public long recalculateEMC(@NotNull ItemInfo itemInfo, long currentEmc) throws ArithmeticException {
        var cell = StorageCells.getCellInventory(itemInfo.createStack(), null);

        if (cell == null) {
            return currentEmc;
        }

        var bigEmc = BigInteger.valueOf(currentEmc);

        for (var key : cell.getAvailableStacks()) {
            if (key.getKey() instanceof AEItemKey item) {
                var keyEmc = IEMCProxy.INSTANCE.getValue(item.toStack());
                bigEmc = bigEmc.add(BigInteger.valueOf(keyEmc).multiply(BigInteger.valueOf(key.getLongValue())));
            }
        }

        return AppliedE.clampedLong(bigEmc);
    }
}
