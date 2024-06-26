package gripe._90.appliede.me.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.AEKeyFilter;
import appeng.core.sync.BasePacketHandler;
import appeng.core.sync.packets.MEInventoryUpdatePacket;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IncrementalUpdateHelper;

import gripe._90.appliede.mixin.tooltip.BasePacketAccessor;
import gripe._90.appliede.mixin.tooltip.MEInventoryUpdatePacketAccessor;

import io.netty.buffer.Unpooled;

/**
 * See {@link MEInventoryUpdatePacket}
 */
public class MEInventoryUpdatePacketBuilder extends MEInventoryUpdatePacket.Builder {
    private static final int UNCOMPRESSED_PACKET_BYTE_LIMIT = 512 * 1024;
    private static final int INITIAL_BUFFER_CAPACITY = 2 * 1024;

    private final List<MEInventoryUpdatePacket> packets = new ArrayList<>();
    private final int containerId;

    @Nullable
    private AEKeyFilter filter;

    @Nullable
    private FriendlyByteBuf data;

    private int itemCountOffset = -1;
    private int itemCount;

    public MEInventoryUpdatePacketBuilder(int containerId, boolean fullUpdate) {
        super(containerId, fullUpdate);
        this.containerId = containerId;
    }

    @Override
    public void setFilter(@Nullable AEKeyFilter filter) {
        this.filter = filter;
    }

    public void addChanges(
            IncrementalUpdateHelper updateHelper,
            KeyCounter networkStorage,
            Set<AEKey> craftables,
            KeyCounter requestables,
            Set<AEItemKey> transmutables) {
        for (AEKey key : updateHelper) {
            if (filter != null && !filter.matches(key)) {
                continue;
            }

            AEKey sendKey;
            var serial = updateHelper.getSerial(key);

            // Try to serialize the item into the buffer
            if (serial == null) {
                // This is a new key, not sent to the client
                sendKey = key;
                serial = updateHelper.getOrAssignSerial(key);
            } else {
                // This is an incremental update referring back to the serial
                sendKey = null;
            }

            // The queued changes are actual differences, but we need to send the real stored properties
            // to the client.
            var storedAmount = networkStorage.get(key);
            var craftable = craftables.contains(key);
            var requestable = requestables.get(key);
            var transmutable = key instanceof AEItemKey && transmutables.contains(key);

            GridInventoryEntry entry;

            if (storedAmount <= 0 && requestable <= 0 && !craftable) {
                // This happens when an update is queued but the item is no longer stored
                entry = new GridInventoryEntry(serial, sendKey, 0, 0, false);
                updateHelper.removeSerial(key);
            } else {
                entry = new GridInventoryEntry(serial, sendKey, storedAmount, requestable, craftable);
                ((GridInventoryEMCEntry) entry).appliede$setTransmutable(transmutable);
            }

            add(entry);
        }

        updateHelper.commitChanges();
    }

    @Override
    public void add(GridInventoryEntry entry) {
        var data = ensureData();
        GridInventoryEMCEntry.writeEntry(data, entry);
        ++itemCount;

        if (data.writerIndex() >= UNCOMPRESSED_PACKET_BYTE_LIMIT || itemCount >= Short.MAX_VALUE) {
            flushData();
        }
    }

    @SuppressWarnings("UnreachableCode")
    private void flushData() {
        if (data != null) {
            data.markWriterIndex();
            data.writerIndex(itemCountOffset);
            data.writeShort(itemCount);
            data.resetWriterIndex();

            var packet = MEInventoryUpdatePacketAccessor.create();
            ((BasePacketAccessor) packet).invokeConfigureWrite(data);
            packets.add(packet);

            data = null;
            itemCountOffset = -1;
            itemCount = 0;
        }
    }

    private FriendlyByteBuf ensureData() {
        if (data == null) {
            data = createPacketHeader();
        }

        return data;
    }

    private FriendlyByteBuf createPacketHeader() {
        var data = new FriendlyByteBuf(Unpooled.buffer(INITIAL_BUFFER_CAPACITY));
        data.writeInt(BasePacketHandler.PacketTypes.ME_INVENTORY_UPDATE.getPacketId());
        data.writeVarInt(containerId);
        data.writeBoolean(false);

        itemCountOffset = data.writerIndex();
        data.writeShort(0);

        return data;
    }

    @Override
    public List<MEInventoryUpdatePacket> build() {
        flushData();
        return packets;
    }
}
