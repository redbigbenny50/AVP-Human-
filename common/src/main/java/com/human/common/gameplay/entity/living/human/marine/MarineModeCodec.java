package com.human.common.gameplay.entity.living.human.marine;

import com.just.codec.schema.CodecSchema;
import com.just.core.functional.result.Result;

public final class MarineModeCodec {

    private static final String KEY_FOLLOW = "follow";

    private static final String KEY_HOLD = "hold";

    private static final String KEY_SENTRY = "sentry";

    private static final String TYPE = "type";

    public static <T> Result<MarineMode, T> decode(
        CodecSchema<T> schema,
        T input
    ) {
        return schema.getField(input, TYPE)
            .andThen(schema::getStringValue)
            // ⚠ The type string is the ENTIRE payload - see encode. Handing `input` (a map) to the enum sub-codecs
            // would have them try to read a string and fail, which is the mirror image of the encode bug.
            .andThen(type -> switch (type) {
                case KEY_FOLLOW -> Result.<MarineMode, T>ok(MarineMode.FOLLOW);
                case KEY_HOLD -> Result.<MarineMode, T>ok(MarineMode.HOLD);
                case KEY_SENTRY -> Result.<MarineMode, T>ok(MarineMode.SENTRY);
                default -> Result.err(input);
            });
    }

    /**
     * ⚠⚠ THE BASE MUST BE A MAP, NOT THE ENUM'S OWN ENCODING. This previously built each branch on top of
     * {@code MarineMode.Follow.CODEC.encode(...)}, but those are {@code EnumCodec}s, so what came back was the bare
     * STRING {@code "INSTANCE"} — and {@code createField} cannot add a {@code type} key to a string. The mode therefore
     * persisted as a naked {@code "INSTANCE"} tag, and {@link #decode} then failed its very first step
     * ({@code schema.getField(input, TYPE)}) on every world load:
     *
     * <pre>
     * [Server thread/ERROR] [avp_human/]: Failed to load tag 'mode'. Tag: "INSTANCE"
     * </pre>
     * <p>
     * The consequence was silent because {@code readAdditionalSaveData} uses {@code inspectErr} (log, do not throw):
     * every marine came back on its DEFAULT mode, so a marine left on HOLD or SENTRY reverted to FOLLOW on every reload
     * — the exact bug the persistence was added to fix.
     * </p>
     * <p>
     * ⚠ The three enum sub-codecs are deliberately no longer called at all. Each enum has a single INSTANCE, so the
     * {@code type} string IS the whole payload; encoding the constant name as well only re-created the confusion.
     * </p>
     */
    public static <T> T encode(
        CodecSchema<T> schema,
        MarineMode value
    ) {
        if (value == MarineMode.FOLLOW) {
            return schema.createField(schema.emptyMap(), TYPE, schema.createStringValue(KEY_FOLLOW));
        }

        if (value == MarineMode.HOLD) {
            return schema.createField(schema.emptyMap(), TYPE, schema.createStringValue(KEY_HOLD));
        }

        if (value == MarineMode.SENTRY) {
            return schema.createField(schema.emptyMap(), TYPE, schema.createStringValue(KEY_SENTRY));
        }

        throw new IllegalStateException("Unknown MarineMode: " + value);
    }

    private MarineModeCodec() {
        throw new UnsupportedOperationException();
    }
}
