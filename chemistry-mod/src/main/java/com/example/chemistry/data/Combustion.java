package com.example.chemistry.data;

import java.util.List;

/**
 * Combustion reactions of the combustion spoon (generated). The fuel
 * burns with oxygen from the air (or inside an oxygen jar).
 */
public final class Combustion {

    /** ignition: temperature (C) at which the fuel catches fire in open air. */
    public record Burn(String fuel, String product, boolean vent, int ignition,
            String equation, String airMessage, String oxygenMessage) {
    }

    public static final List<Burn> ALL = List.of(
            new Burn("charcoal", "", true, 350, "C + O₂ →(点燃) CO₂",
                    "木炭在空气中燃烧，发出红光", "木炭在氧气中剧烈燃烧，发出白光"),
            new Burn("sulfur", "", true, 250, "S + O₂ →(点燃) SO₂",
                    "硫在空气中燃烧，发出微弱的淡蓝色火焰", "硫在氧气中燃烧，发出明亮的蓝紫色火焰"),
            new Burn("red_phosphorus", "phosphorus_pentoxide", false, 260, "4P + 5O₂ →(点燃) 2P₂O₅",
                    "红磷燃烧，产生大量白烟", "红磷在氧气中剧烈燃烧，白烟更浓"),
            new Burn("white_phosphorus", "phosphorus_pentoxide", false, 30, "4P + 5O₂ →(点燃) 2P₂O₅",
                    "白磷剧烈燃烧，产生大量白烟", "白磷在氧气中剧烈燃烧，火焰明亮"),
            new Burn("magnesium", "magnesium_oxide", false, 470, "2Mg + O₂ →(点燃) 2MgO",
                    "镁剧烈燃烧，发出耀眼白光，生成白色固体", "镁在氧气中剧烈燃烧，白光更加刺眼"),
            new Burn("calcium", "calcium_oxide", false, 350, "2Ca + O₂ →(点燃) 2CaO",
                    "钙燃烧，火焰呈砖红色", "钙在氧气中剧烈燃烧，砖红色火焰更旺"),
            new Burn("iron", "iron_ii_iii_oxide", false, 700, "3Fe + 2O₂ →(点燃) Fe₃O₄",
                    "铁粉在空气中燃烧，火星四射", "铁在氧气中剧烈燃烧，火星四射，生成黑色固体"),
            new Burn("copper", "copper_ii_oxide", false, 600, "2Cu + O₂ →(点燃) 2CuO",
                    "铜在空气中加热，表面逐渐变黑", "铜在氧气中燃烧，生成黑色氧化铜"),
            new Burn("aluminium", "aluminium_oxide", false, 650, "4Al + 3O₂ →(点燃) 2Al₂O₃",
                    "铝燃烧，发出耀眼白光", "铝在氧气中剧烈燃烧，白光耀眼"),
            new Burn("silicon", "silicon_dioxide", false, 900, "Si + O₂ →(点燃) SiO₂",
                    "硅燃烧，发出白光", "硅在氧气中剧烈燃烧，白光明亮")
    );

    public static Burn byFuel(String fuel) {
        for (Burn burn : ALL) {
            if (burn.fuel().equals(fuel)) {
                return burn;
            }
        }
        return null;
    }

    private Combustion() {
    }
}
