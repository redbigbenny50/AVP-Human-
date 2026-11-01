package com.human.common.gameplay.gene.machine.splicer;

/** The LABEL of the Splicer whose screen was just opened (it arrives right after the screen does). */
public final class ClientSplicerLabel {

    private static String label = "";

    private static int version;

    private ClientSplicerLabel() {}

    public static synchronized void accept(String value) {
        label = value;
        version++;
    }

    public static String label() {
        return label;
    }

    public static int version() {
        return version;
    }
}
