package com.human.common.gameplay.gene.machine.storage;

import com.human.Human;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.network.packet.S2CNetworkTubesPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The NETWORK STORAGE list a genetic machine's screen shows (Recombinator, Splicer): the filled tubes in Gene Storage
 * on the machine's power grid, ISOLATED storages left out. Built once a second while the screen is open and sent only
 * when it changed; the server keeps what each listed index points at, so a click is resolved and re-checked here.
 */
public final class NetworkTubeList {

    /** Most tubes listed (keeps the packet well under its size limit). */
    public static final int MAX_LISTED = 300;

    public interface Menu {

        /** A click on listed tube {@code index}. */
        void pull(int index);
    }

    private final List<GeneStorageNetwork.Member> storages = new ArrayList<>();

    private final List<Integer> slots = new ArrayList<>();

    private @Nullable String lastSent;

    /** Rebuilds the list and sends it to {@code player} if it changed. */
    public void refresh(ServerLevel level, BlockPos machine, ServerPlayer player) {
        storages.clear();
        slots.clear();
        var text = new StringBuilder();
        var storageNumber = 0;
        outer:
        for (var member : GeneStorageNetwork.storagesFor(level, machine, machine)) {
            storageNumber++;
            var rack = member.rack();
            for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                var contents = GeneTubeItem.read(rack.getItem(slot));
                if (contents == null) {
                    continue;
                }
                if (slots.size() >= MAX_LISTED) {
                    break outer;
                }
                text.append(slots.size())
                    .append('\t')
                    .append(contents.gene())
                    .append('\t')
                    .append(contents.value())
                    .append('\t')
                    .append(contents.operation().name())
                    .append('\t')
                    .append(contents.perDose())
                    .append('\t')
                    .append(storageNumber)
                    .append('\n');
                storages.add(member);
                slots.add(slot);
            }
        }
        var snapshot = text.toString();
        if (!snapshot.equals(lastSent)) {
            lastSent = snapshot;
            Human.MOD.networking().sendToClient(player, new S2CNetworkTubesPayload(snapshot));
        }
    }

    /** The storage and slot listed at {@code index}, or null when the index is stale. */
    public @Nullable GeneStorageNetwork.Member storage(int index) {
        return index >= 0 && index < storages.size() ? storages.get(index) : null;
    }

    public int slot(int index) {
        return index >= 0 && index < slots.size() ? slots.get(index) : -1;
    }

    /** Forces the next refresh to send (after something was taken). */
    public void invalidate() {
        lastSent = null;
    }
}
