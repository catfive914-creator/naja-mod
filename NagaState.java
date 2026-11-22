package com.catfive.naga;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.item.ItemStack;

public record NagaState(int tribe, int level, boolean transformed, List<ItemStack> stash) {
    public static final NagaState DEFAULT = new NagaState(-1, 1, false, List.of());
    public static final Codec<NagaState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("tribe").forGetter(NagaState::tribe),
        Codec.INT.fieldOf("level").forGetter(NagaState::level),
        Codec.BOOL.fieldOf("transformed").forGetter(NagaState::transformed),
        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("stash").forGetter(NagaState::stash)
    ).apply(i, NagaState::new));

    /** เลเวล 1=1 เศียร, 20=2, 40=3, 60=4, 80=7, 100=9 */
    public static int headsFor(int l) { return l >= 100 ? 9 : l >= 80 ? 7 : l >= 60 ? 4 : l >= 40 ? 3 : l >= 20 ? 2 : 1; }
    public int heads() { return headsFor(level); }
    public NagaState withTribe(int t) { return new NagaState(t, level, transformed, stash); }
    public NagaState withLevel(int l) { return new NagaState(tribe, Math.min(100, Math.max(1, l)), transformed, stash); }
    public NagaState with(boolean tr, List<ItemStack> st) { return new NagaState(tribe, level, tr, st); }
}
