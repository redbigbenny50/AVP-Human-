package com.human.common.gameplay.power.diag;

import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * An opt-in log of what the power grid actually moved, written to its own file.
 * <h2>Why it exists</h2> ⭐ [stated] Sep 26: "keeps a log of power transfer for testing to make sure everything is
 * moving smoothly through the system ... this way its not mixed into the normal log and i can give it to you to verify
 * the system is fully working." The conservation fuzz proves the arithmetic in isolation; nothing until now showed that
 * a real base behaves the same way.
 * <h2>⚠ OFF COSTS ONE BOOLEAN READ</h2> {@link #isEnabled()} is checked before any string is built, so a release with
 * this compiled in pays a field read per grid per tick and nothing else. No formatting, no allocation, no file handle.
 * <h2>⚠ ON CHANGE PLUS A HEARTBEAT - NEVER EVERY TICK</h2> Twenty lines a second per grid fills a file faster than
 * anyone can read it. A grid writes when its numbers MOVE, and once every {@value #HEARTBEAT_TICKS} ticks regardless so
 * a steady state still appears. That keeps an idle base quiet and a changing one detailed.
 */
public final class PowerDiagnostics {

    /** 5 seconds. Long enough to stay quiet, short enough that a steady grid still proves it is alive. */
    private static final int HEARTBEAT_TICKS = 100;

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final DateTimeFormatter LINE_STAMP = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static volatile boolean enabled;

    private static Writer writer;

    private static Path file;

    private PowerDiagnostics() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static Path currentFile() {
        return file;
    }

    /**
     * @return the file being written, or null when switching off
     */
    public static synchronized Path setEnabled(boolean value, Path logsRoot) {
        if (value == enabled) {
            return file;
        }

        if (value) {
            open(logsRoot);
        } else {
            close();
        }

        enabled = value;

        return file;
    }

    private static void open(Path logsRoot) {
        try {
            var directory = logsRoot.resolve("avp_power");

            Files.createDirectories(directory);

            file = directory.resolve("power-" + LocalDateTime.now().format(FILE_STAMP) + ".log");
            writer = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

            writeRaw("# avp_human power diagnostics");
            writeRaw(
                "# columns: time | grid | dim | anchor | nodes P/C/S | gen | demand | delivered | unmet"
                    + " | store-out | store-in | stored | check"
            );
            writeRaw(
                "# check=OK means delivered + stored-this-tick == drawn from producers and stores."
                    + " Anything else is a conservation fault and is the reason this log exists."
            );
        } catch (IOException e) {
            writer = null;
            file = null;

            throw new UncheckedIOException(e);
        }
    }

    private static void close() {
        if (writer == null) {
            return;
        }

        try {
            writeRaw("# stopped");
            writer.close();
        } catch (IOException ignored) {
            // Nothing useful to do while shutting a debug log; never let it break a tick.
        } finally {
            writer = null;
        }
    }

    /** ⚠ Every field the arithmetic needs to be CHECKED, not just read. */
    public static synchronized void logTick(
        int gridId,
        String dimension,
        BlockPos anchor,
        int producers,
        int consumers,
        int stores,
        long generated,
        long demand,
        long delivered,
        long fromStores,
        long toStores,
        long stored
    ) {
        if (!enabled || writer == null) {
            return;
        }

        var unmet = Math.max(0L, demand - delivered);

        // ⚠⚠ THE INVARIANT, STATED SO IT CAN ACTUALLY FAIL.
        //
        // Everything that ARRIVED this tick - power delivered to consumers plus power put into stores - can only have
        // come from what was GENERATED plus what was taken OUT of stores. If arrivals ever exceed sources, the grid
        // created power from nothing; that is the single fault this log exists to catch, and it should never appear.
        var arrived = delivered + toStores;
        var sources = generated + fromStores;
        var check = arrived > sources ? "FAULT +" + (arrived - sources) : "OK";

        writeRaw(
            String.format(
                Locale.ROOT,
                "%s | #%d | %s | %s | %d/%d/%d | gen %d | dem %d | del %d | unmet %d | s-out %d | s-in %d"
                    + " | stored %d | %s",
                LocalDateTime.now().format(LINE_STAMP),
                gridId,
                dimension,
                anchor == null ? "-" : anchor.toShortString(),
                producers,
                consumers,
                stores,
                generated,
                demand,
                delivered,
                unmet,
                fromStores,
                toStores,
                stored,
                check
            )
        );
    }

    public static synchronized void logNote(String note) {
        if (enabled && writer != null) {
            writeRaw("# " + note);
        }
    }

    public static int heartbeatTicks() {
        return HEARTBEAT_TICKS;
    }

    private static void writeRaw(String line) {
        try {
            writer.write(line);
            writer.write(System.lineSeparator());
            writer.flush();
        } catch (IOException ignored) {
            // ⚠ A diagnostic must never take the server down. A failed write is dropped silently.
        }
    }
}
