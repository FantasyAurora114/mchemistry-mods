package com.example.chemistry.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Breaking / hitting particles for invisible blocks (iron stand, rubber-tube
 * segments). The sprite is fetched from the block atlas directly so it never
 * falls back to the purple-black missing model.
 */
public class InvisibleBlockClientExtensions implements IClientBlockExtensions {

    private final ResourceLocation texture;

    public InvisibleBlockClientExtensions(ResourceLocation texture) {
        this.texture = texture;
    }

    private TextureAtlasSprite sprite() {
        return Minecraft.getInstance().getAtlasManager()
                .get(new Material(TextureAtlas.LOCATION_BLOCKS, texture));
    }

    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        if (level instanceof ClientLevel clientLevel) {
            TextureAtlasSprite sprite = sprite();
            RandomSource random = level.random;
            for (int i = 0; i < 12; i++) {
                manager.add(new SpriteTerrainParticle(clientLevel,
                        pos.getX() + random.nextDouble(),
                        pos.getY() + random.nextDouble() * 0.6,
                        pos.getZ() + random.nextDouble(),
                        random.nextGaussian() * 0.08, random.nextGaussian() * 0.08,
                        random.nextGaussian() * 0.08, state, pos, sprite));
            }
        }
        return true;
    }

    @Override
    public boolean addHitEffects(BlockState state, Level level, @Nullable HitResult target, ParticleEngine manager) {
        if (level instanceof ClientLevel clientLevel && target instanceof BlockHitResult blockHit) {
            manager.add(new SpriteTerrainParticle(clientLevel,
                    blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z,
                    0.0, 0.0, 0.0, state, blockHit.getBlockPos(), sprite()));
        }
        return true;
    }

    /** Small dust burst when an entity walks on the invisible block. */
    public static void spawnStepParticles(Level level, BlockPos pos, Entity entity, ResourceLocation texture) {
        if (!level.isClientSide() || !(entity instanceof LivingEntity)) {
            return;
        }
        if (level instanceof ClientLevel clientLevel) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                    .get(new Material(TextureAtlas.LOCATION_BLOCKS, texture));
            ParticleEngine manager = Minecraft.getInstance().particleEngine;
            RandomSource random = level.random;
            for (int i = 0; i < 2; i++) {
                manager.add(new SpriteTerrainParticle(clientLevel,
                        entity.getX() + (random.nextDouble() - 0.5) * 0.4,
                        pos.getY() + 0.1,
                        entity.getZ() + (random.nextDouble() - 0.5) * 0.4,
                        (random.nextDouble() - 0.5) * 0.1, 0.1, (random.nextDouble() - 0.5) * 0.1,
                        level.getBlockState(pos), pos, sprite));
            }
        }
    }

    private static class SpriteTerrainParticle extends TerrainParticle {
        SpriteTerrainParticle(ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz, BlockState state, BlockPos pos, TextureAtlasSprite sprite) {
            super(level, x, y, z, vx, vy, vz, state, pos);
            this.setSprite(sprite);
        }
    }
}
