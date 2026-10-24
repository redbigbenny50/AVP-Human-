package com.human.common.gameplay.block.pipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * [stated] Oct 14: "build in that pipe detection with the wiring tool so it tells us whats not working or being seen".
 * The shared wording for the readout a wiring-tool click on a pipe prints - both pipe systems build their lines with
 * these, so fluid and item pipes read the same.
 * <p>
 * Plain English built in code rather than lang keys: it is a diagnostic readout, and it keeps this off the lang
 * provider (which another unmerged change also edits).
 * </p>
 */
public final class PipeDiagnostics {

    /** At most this many ends are listed; a huge network says how many it left out. */
    public static final int MAX_ENDS_LISTED = 8;

    private PipeDiagnostics() {}

    /** @return the end's setting as the pipe-end screen labels it */
    public static String settingName(byte setting) {
        return switch (setting) {
            case PipeBlockEntity.PULL -> "Extract";
            case PipeBlockEntity.PUSH -> "Input";
            case PipeBlockEntity.BOTH -> "Both";
            case PipeBlockEntity.OFF -> "Off";
            default -> "Auto";
        };
    }

    /** @return "Fluid Tank (bottom face)" - the block's own face, the one the pipe touches */
    public static String where(ServerLevel level, BlockPos target, Direction pipeFace) {
        var name = level.getBlockState(target).getBlock().getName().getString();
        return name + " (" + faceName(pipeFace.getOpposite()) + " face)";
    }

    public static String faceName(Direction face) {
        return switch (face) {
            case UP -> "top";
            case DOWN -> "bottom";
            default -> face.getSerializedName();
        };
    }

    public static MutableComponent header(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD);
    }

    /** One end: where, setting, and what it works out to - green input, red extract, yellow both, grey not working. */
    public static MutableComponent endLine(String where, byte setting, byte mode, String detail) {
        var result = switch (mode) {
            case PipeBlockEntity.IN -> Component.literal("taking in").withStyle(ChatFormatting.GREEN);
            case PipeBlockEntity.OUT -> Component.literal("extracting").withStyle(ChatFormatting.RED);
            case PipeBlockEntity.BOTH -> Component.literal("both ways").withStyle(ChatFormatting.YELLOW);
            default -> Component.literal("NOT WORKING").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);
        };

        var line = Component.literal(" - " + where + ": " + settingName(setting) + " -> ")
            .withStyle(ChatFormatting.GRAY)
            .append(result);

        if (detail != null && !detail.isEmpty()) {
            line.append(Component.literal(" (" + detail + ")").withStyle(ChatFormatting.GRAY));
        }

        return line;
    }

    public static MutableComponent ok(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GREEN);
    }

    public static MutableComponent problem(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }

    public static MutableComponent note(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    /** @return " | filter: Allow 2 lines" when the end has filter lines, else "" (no lines = everything passes) */
    public static String filterNote(PipeFilter filter) {
        if (filter == null || filter.entries().isEmpty()) {
            return "";
        }

        return "filter: " + (filter.isBlockList() ? "Block " : "Allow ") + filter.entries().size()
            + (filter.entries().size() == 1 ? " line" : " lines");
    }

    /** Joins the non-empty parts with ", ". */
    public static String join(List<String> parts) {
        var builder = new StringBuilder();

        for (var part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(", ");
            }

            builder.append(part);
        }

        return builder.toString();
    }
}
