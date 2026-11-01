package com.human.common.gameplay.gene.tube;

import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.item.GeneTooltips;
import com.human.common.registry.init.HumanGeneTubes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A filled gene tube: one gene with all of its doses, from one donor. Its contents live in the stack's custom data, so
 * tubes holding the same gene, value and donor stack (up to 16) and any difference keeps them apart. The fluid takes
 * the gene's colour (layer 1 tint, client side): mutations gold, defects dark red, everything else a colour of its own.
 */
public class GeneTubeItem extends Item {

    public static final String KEY = "avp_human_gene_tube";

    public record Contents(
        ResourceLocation gene,
        GeneOperationType operation,
        double value,
        @Nullable String donor,
        int doses
    ) {

        /** The strength of one of this tube's doses. */
        public double perDose() {
            return doses <= 0 ? value : value / doses;
        }
    }

    public GeneTubeItem(Properties properties) {
        super(properties);
    }

    /** A filled tube for one gene. Values are rounded to 4 places so equal samples stack. */
    public static ItemStack create(ResourceLocation gene, GeneOperationType operation, double value, @Nullable String donor) {
        return create(gene, operation, value, donor, 1);
    }

    /** A filled tube holding {@code doses} doses (one draw is one dose; the Recombinator adds them). */
    public static ItemStack create(ResourceLocation gene, GeneOperationType operation, double value, @Nullable String donor, int doses) {
        var stack = new ItemStack(HumanGeneTubes.GENE_TUBE.get());
        var tag = new CompoundTag();
        tag.putString("gene", gene.toString());
        tag.putString("op", operation.name());
        tag.putDouble("value", Math.round(value * 10000.0) / 10000.0);
        if (donor != null) {
            tag.putString("donor", donor);
        }
        tag.putInt("doses", Math.max(1, doses));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.put(KEY, tag));
        return stack;
    }

    public static @Nullable Contents read(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        var root = data.copyTag();
        if (!root.contains(KEY)) {
            return null;
        }
        var tag = root.getCompound(KEY);
        var gene = ResourceLocation.tryParse(tag.getString("gene"));
        if (gene == null) {
            return null;
        }
        GeneOperationType operation;
        try {
            operation = GeneOperationType.valueOf(tag.getString("op"));
        } catch (IllegalArgumentException exception) {
            operation = GeneOperationType.ADDITIVE;
        }
        return new Contents(
            gene,
            operation,
            tag.getDouble("value"),
            tag.contains("donor") ? tag.getString("donor") : null,
            tag.contains("doses") ? Math.max(1, tag.getInt("doses")) : 1
        );
    }

    /**
     * The fluid colour for a tube's gene (ARGB). Stable per gene id - see
     * {@link com.human.common.gameplay.gene.GeneColours}.
     */
    public static int colour(ItemStack stack) {
        var contents = read(stack);
        if (contents == null) {
            return com.human.common.gameplay.gene.GeneColours.UNKNOWN;
        }
        return com.human.common.gameplay.gene.GeneColours.of(contents.gene());
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> lines,
        @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);
        var contents = read(stack);
        if (contents == null) {
            lines.add(Component.literal("Empty sample").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.add(GeneTooltips.line(contents.gene(), contents.operation(), contents.value()));
        lines.add(Component.literal(contents.doses() == 1 ? "1 dose" : contents.doses() + " doses").withStyle(ChatFormatting.DARK_GRAY));
        if (contents.donor() != null) {
            lines.add(GeneTooltips.donor(contents.donor()));
        }
    }
}
