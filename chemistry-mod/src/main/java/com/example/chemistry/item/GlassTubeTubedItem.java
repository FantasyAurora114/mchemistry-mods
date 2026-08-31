package com.example.chemistry.item;

import com.example.chemistry.GlassTubeIgnition;
import com.example.chemistry.entity.RubberTubeEntity.Port;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 套着橡胶管的玻璃导管：由“副手玻璃导管 + 主手湿橡胶管”合成。右键目标
 * （导管头 / 瓶口 / 水槽导管 / 方块 / 实体）时，把橡胶管的自由端从手上
 * 的导管连到目标（一次右键完成），物品保留在手上；配火源可点燃导出的气体。
 */
public class GlassTubeTubedItem extends Item {

    public GlassTubeTubedItem(Properties properties) {
        super(properties);
    }

    /** 副手拿火源时右键（对空）发送使用数据包，由服务端点燃导管口气体。 */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (GlassTubeIgnition.isFireSource(player.getOffhandItem())) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Port point = Port.block(context.getClickedPos(), context.getClickedFace());
        RubberTubeItem.createTube(context.getLevel(), player, context.getItemInHand(),
                Port.entity(player.getUUID()), point);
        return InteractionResult.SUCCESS;
    }
}
