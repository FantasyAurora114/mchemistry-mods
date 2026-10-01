package com.example.chemistry.radiation;
import com.example.chemistry.ChemistryMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.network.chat.Component;
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class RadiationTooltips {
 @SubscribeEvent public static void tooltip(ItemTooltipEvent e){var s=e.getItemStack();var roots=RadioLedger.carriers(s);if(!roots.isEmpty()){
  e.getToolTip().add(Component.literal((RadioLedger.activity(s)>0?"☢ 放射性样品　活度约 ":"稳定同位素　活度 ")+String.format(java.util.Locale.ROOT,"%.3g Bq",RadioLedger.activity(s))));
  for(var r:roots.entrySet()){var n=RadioLedger.NUCLIDES.get(r.getKey());e.getToolTip().add(Component.literal("同位素 "+n.id()+"；半衰期 "+(Double.isFinite(n.halfSeconds())?halfLife(n.halfSeconds()):"稳定")+"；"+mode(n.mode())));var f=RadioLedger.fractions(s,n.id());for(var v:f.entrySet())if(!v.getKey().equals(n.id())&&!v.getKey().equals("electrons")&&v.getValue()>1e-8)e.getToolTip().add(Component.literal("  核素账本："+v.getKey()+" "+String.format(java.util.Locale.ROOT,"%.3g mol",r.getValue()*v.getValue())));}
  e.getToolTip().add(Component.literal("瓶塞防泄漏；屏蔽能力取决于辐射类型"));
 }
 if(NuclearInstability.points(s)>0)e.getToolTip().add(Component.literal(String.format(java.util.Locale.ROOT,"聚集点数 %.2f；失稳阈值100（纯游戏参数）",NuclearInstability.points(s))));
 if(RadiationGear.isSuit(s)){e.getToolTip().add(Component.literal("皮革护甲强度；每件移速 -2%"));e.getToolTip().add(Component.literal("降低沾染风险；对γ射线防护很有限"));}
 if(s.is(com.example.chemistry.registry.ModItems.GEIGER_COUNTER.get()))e.getToolTip().add(Component.literal("手持自动检测；右键查看累计剂量（游戏单位）"));
 if(s.is(com.example.chemistry.registry.ModItems.RADIOACTIVE_WASTE_BOTTLE.get()))e.getToolTip().add(Component.literal("副手持瓶，潜行右键运行中的洗涤池，收集主手仪器的放射性残留"));
 }
 private static String mode(String v){return switch(v){case "alpha"->"α";case "beta"->"β";case "gamma"->"γ";case "beta_gamma"->"β+γ";default->"稳定";};}
 private static String halfLife(double s){if(s>=RadioLedger.YEAR)return String.format(java.util.Locale.ROOT,"%.4g 年",s/RadioLedger.YEAR);if(s>=86400)return String.format(java.util.Locale.ROOT,"%.4g 天",s/86400);return String.format(java.util.Locale.ROOT,"%.4g 秒",s);}
 private RadiationTooltips(){}
}
