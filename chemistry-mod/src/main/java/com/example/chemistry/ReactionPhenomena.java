package com.example.chemistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Solids;
import com.example.chemistry.item.LabVesselItem;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Reaction phenomenon engine: when a reaction completes, spawns visible
 * effects at the vessel — bubbles for gas production, coloured falling dust
 * for precipitates, smoke for fuming gases, steam/flame for exothermic or
 * violent reactions. The particle count scales with the reaction speed.
 */
public final class ReactionPhenomena {

    public enum Type {
        BUBBLES, PRECIPITATE, SMOKE, STEAM, FLASH
    }

    public record Phenomenon(Type type, int color, int amount) {
    }

    private static final Set<String> FUMING_GASES = Set.of(
            "ammonia", "nitric_oxide", "nitrogen_dioxide",
            "sulfur_dioxide", "hydrogen_chloride", "chlorine");

    /** Determine which phenomena a completed reaction should show. */
    public static List<Phenomenon> detect(Reactions.Reaction reaction, ItemStack vessel) {
        List<Phenomenon> out = new ArrayList<>();
        boolean hasLiquid = LabVesselItem.getContents(vessel).stream()
                .anyMatch(e -> e.type().equals("liquid"));
        int amount = (int) Math.max(1, Math.min(8, Math.round(reaction.speed() * 2.0)));

        // Gas production -> bubbles rising through the liquid.
        boolean gas = reaction.products().stream()
                .anyMatch(p -> p.type().equals("vent") || p.type().equals("gas"));
        if (gas) {
            out.add(new Phenomenon(Type.BUBBLES, 0xFFFFFF, amount));
        }
        // A solid produced into a liquid -> coloured precipitate.
        for (Reactions.Product p : reaction.products()) {
            if (p.type().equals("solid") && hasLiquid) {
                int color = Solids.ALL.stream().filter(s -> s.id().equals(p.id()))
                        .map(Solids.Solid::color).findFirst().orElse(0xFFFFFF);
                out.add(new Phenomenon(Type.PRECIPITATE, color, amount));
                break;
            }
        }
        // Fuming / coloured gases -> smoke above the vessel.
        boolean fuming = reaction.products().stream()
                .anyMatch(p -> p.type().equals("gas") && FUMING_GASES.contains(p.id()));
        if (fuming || reaction.display().contains("白雾")
                || reaction.display().contains("红棕色")
                || reaction.display().contains("NO₂↑")
                || reaction.display().contains("NO↑")) {
            out.add(new Phenomenon(Type.SMOKE, 0xFFFFFF, amount));
        }
        // Exothermic / violent reactions -> steam + a brief flame flash.
        if (reaction.display().contains("放热") || reaction.display().contains("剧烈")) {
            out.add(new Phenomenon(Type.STEAM, 0xFFFFFF, amount));
            out.add(new Phenomenon(Type.FLASH, 0xFFB050, amount));
        }
        return out;
    }

    /** Spawn the phenomena around a vessel position. */
    public static void spawn(Level level, Vec3 pos, List<Phenomenon> phenomena) {
        if (!(level instanceof ServerLevel server) || phenomena.isEmpty()) {
            return;
        }
        RandomSource random = level.random;
        for (Phenomenon p : phenomena) {
            int n = 5 + p.amount() * 3;
            switch (p.type()) {
                case BUBBLES -> {
                    for (int i = 0; i < n; i++) {
                        server.sendParticles(ParticleTypes.BUBBLE,
                                pos.x + (random.nextDouble() - 0.5) * 0.35,
                                pos.y + 0.35 + random.nextDouble() * 0.25,
                                pos.z + (random.nextDouble() - 0.5) * 0.35,
                                1, 0, 0.03, 0, 0.01);
                    }
                }
                case PRECIPITATE -> {
                    for (int i = 0; i < n; i++) {
                        server.sendParticles(new DustParticleOptions(p.color, 1.2F),
                                pos.x + (random.nextDouble() - 0.5) * 0.3,
                                pos.y + 0.8 + random.nextDouble() * 0.2,
                                pos.z + (random.nextDouble() - 0.5) * 0.3,
                                1, 0, -0.02, 0, 0.0);
                    }
                }
                case SMOKE -> server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.x, pos.y + 0.9, pos.z, n, 0.1, 0.06, 0.1, 0.01);
                case STEAM -> server.sendParticles(ParticleTypes.CLOUD,
                        pos.x, pos.y + 0.9, pos.z, Math.max(1, n / 2), 0.1, 0.05, 0.1, 0.01);
                case FLASH -> server.sendParticles(ParticleTypes.FLAME,
                        pos.x, pos.y + 0.5, pos.z, Math.max(1, n / 2), 0.12, 0.1, 0.12, 0.02);
            }
        }
    }

    private ReactionPhenomena() {
    }
}
