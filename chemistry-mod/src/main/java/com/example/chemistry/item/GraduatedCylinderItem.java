package com.example.chemistry.item;

import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.registry.ModEntities;
import com.example.chemistry.entity.GraduatedCylinderEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 量筒：可装液体（默认 100mL），右键放下，左键倒出 1mL。 */
public class GraduatedCylinderItem extends Item {

    public static final double CAPACITY = 100.0;
    private static final String KEY_LIQUID = "chem_cyl_liquid";
    private static final String KEY_ML = "chem_cyl_ml";

    public GraduatedCylinderItem(Properties properties) {
        super(properties);
    }

    public static String getLiquid(ItemStack stack) {
        String s = tag(stack).getStringOr(KEY_LIQUID, "");
        return s.isEmpty() ? null : s;
    }

    public static double getMl(ItemStack stack) {
        return tag(stack).getDoubleOr(KEY_ML, 0.0);
    }

    public static void set(ItemStack stack, String liquid, double ml) {
        CompoundTag t = tag(stack);
        if (liquid == null || ml <= 0.01) {
            t.remove(KEY_LIQUID);
            t.remove(KEY_ML);
        } else {
            t.putString(KEY_LIQUID, liquid);
            t.putDouble(KEY_ML, Math.min(CAPACITY, ml));
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** 加入液体，返回实际加入量。 */
    public static double add(ItemStack stack, String liquid, double ml) {
        String cur = getLiquid(stack);
        if (cur != null && !cur.equals(liquid)) {
            return 0;
        }
        double have = getMl(stack);
        double add = Math.min(ml, CAPACITY - have);
        set(stack, liquid, have + add);
        return add;
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** 右键放下量筒（生成技术性实体，带虚拟命中框）。 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        GraduatedCylinderEntity entity =
                new GraduatedCylinderEntity(ModEntities.GRADUATED_CYLINDER.get(), level);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        String liquid = GraduatedCylinderItem.getLiquid(context.getItemInHand());
        entity.setContents(liquid, liquid == null ? 0.0 : GraduatedCylinderItem.getMl(context.getItemInHand()));
        level.addFreshEntity(entity);
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
