package com.human.neoforge.compat.create;

import net.minecraft.world.inventory.ContainerData;

/**
 * ⚠ Menu numbers travel as 16-BIT values: vanilla's container-data packet writes each one with writeShort (checked in
 * ClientboundContainerSetDataPacket). An alternator with four speed cards loads Create with 98,304 SU, which does not
 * fit - so every number that can pass 32,767 goes as two halves and is put back together on the client.
 */
public final class ConverterData {

    private ConverterData() {}

    public static int low(long value) {
        return (int) (Math.max(0L, value) & 0xFFFF);
    }

    public static int high(long value) {
        return (int) ((Math.max(0L, value) >>> 16) & 0xFFFF);
    }

    public static long read(ContainerData data, int lowIndex) {
        return (data.get(lowIndex) & 0xFFFFL) | ((data.get(lowIndex + 1) & 0xFFFFL) << 16);
    }
}
