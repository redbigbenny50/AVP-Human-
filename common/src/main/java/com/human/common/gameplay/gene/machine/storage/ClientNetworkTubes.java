package com.human.common.gameplay.gene.machine.storage;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Client copy of a genetic machine screen's NETWORK STORAGE list (Recombinator, Splicer); plain data, so it lives
 * beside the packet handler.
 */
public final class ClientNetworkTubes {

    public record Entry(
        int index,
        ResourceLocation gene,
        double value,
        String operation,
        double perDose,
        int storage,
        int count
    ) {}

    private static List<Entry> entries = List.of();

    private ClientNetworkTubes() {}

    public static synchronized void accept(String text) {
        var list = new ArrayList<Entry>();
        for (var line : text.split("\n")) {
            var f = line.split("\t", -1);
            if (f.length < 6) {
                continue;
            }
            try {
                var gene = ResourceLocation.tryParse(f[1]);
                if (gene != null) {
                    list.add(
                        new Entry(
                            Integer.parseInt(f[0]),
                            gene,
                            Double.parseDouble(f[2]),
                            f[3],
                            Double.parseDouble(f[4]),
                            Integer.parseInt(f[5]),
                            f.length > 6 ? Math.max(1, Integer.parseInt(f[6])) : 1
                        )
                    );
                }
            } catch (NumberFormatException ignored) {}
        }
        entries = List.copyOf(list);
    }

    public static List<Entry> entries() {
        return entries;
    }
}
