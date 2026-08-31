#!/usr/bin/env python3
"""Generate assets for the newly added substances:
alkanes (methane/ethane/propane/butane), cyanogen gas, carboxylic acids
(formic/acetic), hydrocyanic acid, formamide (liquid), amides and cyanide
salts (acetamide, NaCN, KCN, sodium formate/acetate).

Templates: existing 16x16 item textures (empty gas bottle, water bottles,
sodium carbonate wide-mouth bottles, loose powder pile). The content region
is recoloured with the substance's nominal colour from the Java data tables.
"""

import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "mchemistry"
TEX = ASSETS / "textures" / "item"
ITEMS = ASSETS / "items"
MODELS = ASSETS / "models" / "item"
NS = "mchemistry"


def load(name):
    return Image.open(TEX / f"{name}.png").convert("RGBA")


def save(im, name):
    im.save(TEX / f"{name}.png")


def content_mask(src_name, content_rgb):
    """Set of (x, y) whose RGB equals the template's nominal content colour."""
    im = load(src_name)
    px = im.load()
    return {(x, y) for y in range(16) for x in range(16)
            if px[x, y][3] > 0 and px[x, y][:3] == content_rgb}


def paint(src_name, dst_name, mask, color, alpha=None):
    im = load(src_name)
    px = im.load()
    for (x, y) in mask:
        r, g, b, a = px[x, y]
        px[x, y] = (*color, alpha if alpha is not None else a)
    save(im, dst_name)


def recolor_by_ratio(src_name, dst_name, src_nominal, dst_nominal):
    """Scale every opaque pixel's RGB by target/source nominal ratio."""
    im = load(src_name)
    px = im.load()
    sr, sg, sb = src_nominal
    dr, dg, db = dst_nominal
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (min(255, r * dr // sr), min(255, g * dg // sg),
                            min(255, b * db // sb), a)
    save(im, dst_name)


def bucket_tint(src_name, dst_name, color):
    """Vanilla-bucket style tint: shade = brightness of the base pixel."""
    im = load(src_name)
    px = im.load()
    cr, cg, cb = color
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            shade = 0.38 + 0.62 * (max(r, g, b) / 255.0)
            px[x, y] = (int(cr * shade), int(cg * shade), int(cb * shade), a)
    save(im, dst_name)


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_item_assets(item_id):
    write_json(ITEMS / f"{item_id}.json", {
        "model": {"type": "minecraft:model", "model": f"{NS}:item/{item_id}"},
    })
    write_json(MODELS / f"{item_id}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/{item_id}"},
    })


def merge_lang(locale, additions):
    path = ASSETS / "lang" / f"{locale}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(additions)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


# (id, color, zh, en) -- colours must match the Java data tables.
NEW_GASES = [
    ("methane",      0xA0C8E0, "甲烷", "Methane"),
    ("ethane",       0xA8C8D8, "乙烷", "Ethane"),
    ("propane",      0xB0C8D0, "丙烷", "Propane"),
    ("butane",       0xB8C8C8, "丁烷", "Butane"),
    ("cyanogen",     0xC0B8A8, "氰气", "Cyanogen"),
    ("ethylene",     0xB8D8E8, "乙烯", "Ethylene"),
    ("propylene",    0xC0D8E0, "丙烯", "Propylene"),
    ("butene",       0xC8D8D8, "丁烯", "Butene"),
    ("acetylene",    0xD8E0E8, "乙炔", "Acetylene"),
    ("propyne",      0xD8E0E0, "丙炔", "Propyne"),
    ("butyne",       0xE0E0E0, "丁炔", "Butyne"),
    ("fluorine",     0xE8F0A0, "氟气", "Fluorine"),
    ("chloromethane", 0xD0E0E8, "氯甲烷", "Chloromethane"),
    ("nitrous_oxide", 0xE0E8F0, "一氧化二氮（笑气）", "Nitrous Oxide"),
]
NEW_LIQUIDS = [
    ("formic_acid",      0xE8E8D8, "甲酸", "Formic Acid"),
    ("acetic_acid",      0xE8E8E0, "乙酸", "Acetic Acid"),
    ("hydrocyanic_acid", 0xD8E8E8, "氢氰酸", "Hydrocyanic Acid"),
    ("formamide",        0xE8F0E8, "甲酰胺", "Formamide"),
    ("cyanic_acid",      0xE0F0F0, "氰酸", "Cyanic Acid"),
    ("thiocyanic_acid",  0xF0F0E8, "硫氰酸", "Thiocyanic Acid"),
    ("mercury",          0xD0D8E0, "汞", "Mercury"),
    ("bromine",          0xC84A12, "液溴", "Bromine"),
    ("hydrofluoric_acid", 0xE8F0F0, "氢氟酸", "Hydrofluoric Acid"),
    ("hydrobromic_acid", 0xE8E8D8, "氢溴酸", "Hydrobromic Acid"),
    ("hydroiodic_acid",  0xE8D8A0, "氢碘酸", "Hydroiodic Acid"),
    ("ethanol",          0xE8E8E8, "无水乙醇", "Anhydrous Ethanol"),
    ("ethanol_95",       0xE8F0F0, "95%乙醇", "95% Ethanol"),
    ("ethanol_75",       0xE8F0E8, "75%乙醇", "75% Ethanol"),
    ("methanol",         0xE8E8E8, "甲醇", "Methanol"),
    ("dichloromethane",  0xE8E8E8, "二氯甲烷", "Dichloromethane"),
    ("chloroform",       0xE8E8E8, "氯仿", "Chloroform"),
    ("carbon_tetrachloride", 0xE8E8E8, "四氯化碳", "Carbon Tetrachloride"),
    ("chloroethane",     0xE8E8E8, "氯乙烷", "Chloroethane"),
    ("bromoethane",      0xE0D8C0, "溴乙烷", "Bromoethane"),
    ("iodomethane",      0xE0C8A0, "碘甲烷", "Iodomethane"),
    ("dibromoethane",    0xE0D8C0, "1,2-二溴乙烷", "1,2-Dibromoethane"),
    ("dibromoethylene",  0xE0D0B8, "1,2-二溴乙烯", "1,2-Dibromoethylene"),
    ("acetone",          0xE8E8E8, "丙酮", "Acetone"),
    ("diethyl_ether",    0xE8E8E8, "乙醚", "Diethyl Ether"),
    ("ethyl_acetate",    0xE8F0E8, "乙酸乙酯", "Ethyl Acetate"),
    ("benzene",          0xE8E8E0, "苯", "Benzene"),
    ("toluene",          0xE8E8E0, "甲苯", "Toluene"),
    ("acetic_anhydride", 0xE8E8E8, "乙酸酐", "Acetic Anhydride"),
    ("methyl_ethyl_ether", 0xE8E8E8, "甲乙醚", "Methyl Ethyl Ether"),
    ("hypochlorous_acid", 0xE8F0F0, "次氯酸", "Hypochlorous Acid"),
    ("hypobromous_acid", 0xE8E8D8, "次溴酸", "Hypobromous Acid"),
    ("hypoiodous_acid", 0xE8E0C8, "次碘酸", "Hypoiodous Acid"),
    ("chloric_acid", 0xE8F0E8, "氯酸", "Chloric Acid"),
    ("bromic_acid", 0xE8E8D8, "溴酸", "Bromic Acid"),
    ("iodic_acid", 0xE8E0C8, "碘酸", "Iodic Acid"),
    ("perchloric_acid", 0xE8F0E8, "高氯酸", "Perchloric Acid"),
    ("perbromic_acid", 0xE8E8D8, "高溴酸", "Perbromic Acid"),
    ("periodic_acid", 0xE8E0C8, "高碘酸", "Periodic Acid"),
    ("hydrazine", 0xE0E8E8, "肼", "Hydrazine"),
    ("methyl_formate", 0xE8E8E8, "甲酸甲酯", "Methyl Formate"),
    ("ethyl_formate", 0xE8E8E8, "甲酸乙酯", "Ethyl Formate"),
    ("methyl_acetate", 0xE8E8E8, "乙酸甲酯", "Methyl Acetate"),
    ("ethyl_benzoate", 0xE8E8E8, "苯甲酸乙酯", "Ethyl Benzoate"),
    ("ethyl_hydrogen_sulfate", 0xE8E8E0, "硫酸氢乙酯", "Ethyl Hydrogen Sulfate"),
    ("acetaldehyde", 0xE8E8E8, "乙醛", "Acetaldehyde"),
    ("glycerol", 0xE8F0F0, "甘油", "Glycerol"),
]
NEW_SOLIDS = [
    ("acetamide",        0xF0F0F0, "乙酰胺", "Acetamide"),
    ("sodium_cyanide",   0xE8E8F0, "氰化钠", "Sodium Cyanide"),
    ("potassium_cyanide", 0xF0E8F0, "氰化钾", "Potassium Cyanide"),
    ("sodium_formate",   0xE8E8E8, "甲酸钠", "Sodium Formate"),
    ("sodium_acetate",   0xE8E8E8, "乙酸钠", "Sodium Acetate"),
    ("calcium_cyanide",  0xF0F0F0, "氰化钙", "Calcium Cyanide"),
    ("zinc_cyanide",     0xF0F0F0, "氰化锌", "Zinc Cyanide"),
    ("copper_i_cyanide", 0xE8E8E8, "氰化亚铜", "Copper(I) Cyanide"),
    ("copper_ii_cyanide", 0xD0B060, "氰化铜", "Copper(II) Cyanide"),
    ("silver_cyanide",   0xF0F0F0, "氰化银", "Silver Cyanide"),
    ("ammonium_cyanide", 0xF0F0F0, "氰化铵", "Ammonium Cyanide"),
    ("potassium_ferrocyanide", 0xF0E8A0, "亚铁氰化钾（黄血盐）", "Potassium Ferrocyanide"),
    ("potassium_ferricyanide", 0xC84830, "铁氰化钾（赤血盐）", "Potassium Ferricyanide"),
    ("sodium_cyanate",   0xE8E8E8, "氰酸钠", "Sodium Cyanate"),
    ("potassium_cyanate", 0xF0F0EC, "氰酸钾", "Potassium Cyanate"),
    ("ammonium_cyanate", 0xF0F0F0, "氰酸铵", "Ammonium Cyanate"),
    ("sodium_thiocyanate", 0xE8E8E8, "硫氰酸钠", "Sodium Thiocyanate"),
    ("potassium_thiocyanate", 0xF0F0F0, "硫氰酸钾", "Potassium Thiocyanate"),
    ("ammonium_thiocyanate", 0xF0F0F0, "硫氰酸铵", "Ammonium Thiocyanate"),
    ("iron_iii_thiocyanate", 0xA02020, "硫氰酸铁", "Iron(III) Thiocyanate"),
    ("silver_thiocyanate", 0xF0F0F0, "硫氰酸银", "Silver Thiocyanate"),
    ("urea",             0xF0F0F0, "尿素", "Urea"),
    ("potassium_sulfate", 0xF0F0F0, "硫酸钾", "Potassium Sulfate"),
    ("prussian_blue",    0x1B2A6B, "普鲁士蓝", "Prussian Blue"),
    ("turnbull_blue",    0x24418F, "滕氏蓝", "Turnbull's Blue"),
    ("mercury_ii_oxide", 0xD84830, "氧化汞", "Mercury(II) Oxide"),
    ("mercury_i_chloride", 0xF0F0F0, "氯化亚汞（甘汞）", "Mercury(I) Chloride"),
    ("mercury_ii_chloride", 0xF0F0F0, "氯化汞（升汞）", "Mercury(II) Chloride"),
    ("mercury_ii_sulfide", 0xC82828, "硫化汞（朱砂）", "Mercury(II) Sulfide"),
    ("mercury_ii_nitrate", 0xE8E8E8, "硝酸汞", "Mercury(II) Nitrate"),
    ("mercury_ii_sulfate", 0xF0F0F0, "硫酸汞", "Mercury(II) Sulfate"),
    ("mercury_ii_cyanide", 0xF0F0F0, "氰化汞", "Mercury(II) Cyanide"),
    ("mercury_ii_iodide", 0xC82828, "碘化汞", "Mercury(II) Iodide"),
    ("sodium_fluoride", 0xF0F0F0, "氟化钠", "Sodium Fluoride"),
    ("potassium_fluoride", 0xF0F0F0, "氟化钾", "Potassium Fluoride"),
    ("calcium_fluoride", 0xF0F0F0, "氟化钙（萤石）", "Calcium Fluoride"),
    ("sodium_bromide", 0xF0F0F0, "溴化钠", "Sodium Bromide"),
    ("potassium_bromide", 0xF0F0F0, "溴化钾", "Potassium Bromide"),
    ("sodium_iodide", 0xF0F0F0, "碘化钠", "Sodium Iodide"),
    ("potassium_iodide", 0xF0F0F0, "碘化钾", "Potassium Iodide"),
    ("silver_bromide", 0xE8E0C8, "溴化银", "Silver Bromide"),
    ("silver_iodide", 0xE8E078, "碘化银", "Silver Iodide"),
    ("calcium_carbide", 0x989088, "碳化钙（电石）", "Calcium Carbide"),
    ("potassium_formate", 0xF0F0F0, "甲酸钾", "Potassium Formate"),
    ("potassium_acetate", 0xF0F0F0, "乙酸钾", "Potassium Acetate"),
    ("calcium_formate", 0xF0F0F0, "甲酸钙", "Calcium Formate"),
    ("calcium_acetate", 0xF0F0F0, "乙酸钙", "Calcium Acetate"),
    ("ammonium_formate", 0xF0F0F0, "甲酸铵", "Ammonium Formate"),
    ("ammonium_acetate", 0xF0F0F0, "乙酸铵", "Ammonium Acetate"),
    ("magnesium_acetate", 0xF0F0F0, "乙酸镁", "Magnesium Acetate"),
    ("copper_ii_acetate", 0x58A8B8, "乙酸铜", "Copper(II) Acetate"),
    ("iron_iii_acetate", 0xB05030, "乙酸铁", "Iron(III) Acetate"),
    ("silver_acetate", 0xF0F0F0, "乙酸银", "Silver Acetate"),
    ("oxalic_acid", 0xF0F0F0, "草酸", "Oxalic Acid"),
    ("sodium_oxalate", 0xF0F0F0, "草酸钠", "Sodium Oxalate"),
    ("potassium_oxalate", 0xF0F0F0, "草酸钾", "Potassium Oxalate"),
    ("calcium_oxalate", 0xF0F0F0, "草酸钙", "Calcium Oxalate"),
    ("manganese_sulfate", 0xF0E8F0, "硫酸锰", "Manganese Sulfate"),
    ("iodoform",        0xE8D848, "碘仿", "Iodoform"),
    ("benzoic_acid",    0xF0F0F0, "苯甲酸", "Benzoic Acid"),
    ("sodium_peroxide",  0xF0F0D8, "过氧化钠", "Sodium Peroxide"),
    ("potassium_peroxide", 0xF0F0D8, "过氧化钾", "Potassium Peroxide"),
    ("calcium_peroxide", 0xF0F0F0, "过氧化钙", "Calcium Peroxide"),
    ("barium_peroxide",  0xF0F0F0, "过氧化钡", "Barium Peroxide"),
    ("potassium_superoxide", 0xE8E090, "超氧化钾", "Potassium Superoxide"),
    ("potassium_bicarbonate", 0xF0F0F0, "碳酸氢钾", "Potassium Bicarbonate"),
    ("calcium_bicarbonate", 0xF0F0F0, "碳酸氢钙", "Calcium Bicarbonate"),
    ("magnesium_bicarbonate", 0xF0F0F0, "碳酸氢镁", "Magnesium Bicarbonate"),
    ("sodium_bisulfate", 0xF0F0F0, "硫酸氢钠", "Sodium Bisulfate"),
    ("potassium_bisulfate", 0xF0F0F0, "硫酸氢钾", "Potassium Bisulfate"),
    ("sodium_dihydrogen_phosphate", 0xF0F0F0, "磷酸二氢钠", "Sodium Dihydrogen Phosphate"),
    ("disodium_hydrogen_phosphate", 0xF0F0F0, "磷酸氢二钠", "Disodium Hydrogen Phosphate"),
    ("potassium_dihydrogen_phosphate", 0xF0F0F0, "磷酸二氢钾", "Potassium Dihydrogen Phosphate"),
    ("dipotassium_hydrogen_phosphate", 0xF0F0F0, "磷酸氢二钾", "Dipotassium Hydrogen Phosphate"),
    ("sodium_bisulfite", 0xF0F0F0, "亚硫酸氢钠", "Sodium Bisulfite"),
    ("calcium_bisulfite", 0xF0F0F0, "亚硫酸氢钙", "Calcium Bisulfite"),
    ("basic_copper_carbonate", 0x58B8A8, "碱式碳酸铜（铜绿）", "Basic Copper Carbonate"),
    ("basic_magnesium_carbonate", 0xF0F0F0, "碱式碳酸镁", "Basic Magnesium Carbonate"),
    ("sodium_sulfite", 0xF0F0F0, "亚硫酸钠", "Sodium Sulfite"),
    ("potassium_sulfite", 0xF0F0F0, "亚硫酸钾", "Potassium Sulfite"),
    ("calcium_sulfite", 0xF0F0F0, "亚硫酸钙", "Calcium Sulfite"),
    ("sodium_thiosulfate", 0xF0F0F0, "硫代硫酸钠（大苏打）", "Sodium Thiosulfate"),
    ("magnesium_carbonate", 0xF0F0F0, "碳酸镁", "Magnesium Carbonate"),
    ("sodium_sulfate", 0xF0F0F0, "硫酸钠", "Sodium Sulfate"),
    ("sodium_phosphate", 0xF0F0F0, "磷酸钠", "Sodium Phosphate"),
    ("sodium_tetrathionate", 0xF0F0F0, "连四硫酸钠", "Sodium Tetrathionate"),
    ("sodium_methoxide", 0xF0F0F0, "甲醇钠", "Sodium Methoxide"),
    ("sodium_ethoxide", 0xF0F0E8, "乙醇钠", "Sodium Ethoxide"),
    ("potassium_methoxide", 0xF0F0F0, "甲醇钾", "Potassium Methoxide"),
    ("potassium_ethoxide", 0xF0F0E8, "乙醇钾", "Potassium Ethoxide"),
    ("sodium_nitrate", 0xF0F0F0, "硝酸钠", "Sodium Nitrate"),
    ("calcium_nitrate", 0xF0F0F0, "硝酸钙", "Calcium Nitrate"),
    ("barium_nitrate", 0xF0F0F0, "硝酸钡", "Barium Nitrate"),
    ("magnesium_nitrate", 0xF0F0F0, "硝酸镁", "Magnesium Nitrate"),
    ("aluminium_nitrate", 0xF0F0F0, "硝酸铝", "Aluminium Nitrate"),
    ("iron_iii_nitrate", 0xD8A080, "硝酸铁", "Iron(III) Nitrate"),
    ("iron_ii_nitrate", 0xC8E0D0, "硝酸亚铁", "Iron(II) Nitrate"),
    ("copper_ii_nitrate", 0x58A8B8, "硝酸铜", "Copper(II) Nitrate"),
    ("ammonium_nitrate", 0xF0F0F0, "硝酸铵", "Ammonium Nitrate"),
    ("ammonium_sulfate", 0xF0F0F0, "硫酸铵", "Ammonium Sulfate"),
    ("potassium_phosphate", 0xF0F0F0, "磷酸钾", "Potassium Phosphate"),
    ("copper_ii_chloride", 0x58B8A8, "氯化铜", "Copper(II) Chloride"),
    ("ammonium_bromide", 0xF0F0F0, "溴化铵", "Ammonium Bromide"),
    ("ammonium_iodide", 0xF0F0F0, "碘化铵", "Ammonium Iodide"),
    ("sodium_nitrite", 0xF0F0F0, "亚硝酸钠", "Sodium Nitrite"),
    ("potassium_nitrite", 0xF0F0F0, "亚硝酸钾", "Potassium Nitrite"),
    ("barium_oxide", 0xF0F0E8, "氧化钡", "Barium Oxide"),
    ("calcium_nitrite", 0xF0F0F0, "亚硝酸钙", "Calcium Nitrite"),
    ("barium_nitrite", 0xF0F0F0, "亚硝酸钡", "Barium Nitrite"),
    ("ammonium_nitrite", 0xF0F0F0, "亚硝酸铵", "Ammonium Nitrite"),
    ("sodium_hypochlorite", 0xF0F0F0, "次氯酸钠", "Sodium Hypochlorite"),
    ("potassium_hypochlorite", 0xF0F0F0, "次氯酸钾", "Potassium Hypochlorite"),
    ("calcium_hypochlorite", 0xF0F0F0, "次氯酸钙（漂白粉）", "Calcium Hypochlorite"),
    ("sodium_hypobromite", 0xF0F0E8, "次溴酸钠", "Sodium Hypobromite"),
    ("sodium_hypoiodite", 0xF0F0E8, "次碘酸钠", "Sodium Hypoiodite"),
    ("sodium_chlorate", 0xF0F0F0, "氯酸钠", "Sodium Chlorate"),
    ("sodium_bromate", 0xF0F0F0, "溴酸钠", "Sodium Bromate"),
    ("potassium_bromate", 0xF0F0F0, "溴酸钾", "Potassium Bromate"),
    ("sodium_iodate", 0xF0F0F0, "碘酸钠", "Sodium Iodate"),
    ("potassium_iodate", 0xF0F0F0, "碘酸钾", "Potassium Iodate"),
    ("potassium_perchlorate", 0xF0F0F0, "高氯酸钾", "Potassium Perchlorate"),
    ("sodium_perchlorate", 0xF0F0F0, "高氯酸钠", "Sodium Perchlorate"),
    ("potassium_perbromate", 0xF0F0F0, "高溴酸钾", "Potassium Perbromate"),
    ("potassium_periodate", 0xF0F0F0, "高碘酸钾", "Potassium Periodate"),
    ("sodium_periodate", 0xF0F0F0, "高碘酸钠", "Sodium Periodate"),
    ("sodium_hydride", 0xE0E0E0, "氢化钠", "Sodium Hydride"),
    ("potassium_hydride", 0xE0E0E0, "氢化钾", "Potassium Hydride"),
    ("calcium_hydride", 0xD0D0D0, "氢化钙", "Calcium Hydride"),
    ("glyceryl_oxalate", 0xF0F0F0, "草酸甘油酯", "Glyceryl Oxalate"),
    ("chromium_iii_sulfate", 0x58A878, "硫酸铬", "Chromium(III) Sulfate"),
]


def main():
    zh, en = {}, {}

    # ---- Gases: empty bottle template + oxygen content region ----
    gas_mask_closed = content_mask("gas_collecting_bottle_oxygen", (143, 208, 240))
    gas_mask_open = content_mask("open_gas_collecting_bottle_oxygen", (143, 208, 240))
    for gid, color, zh_name, en_name in NEW_GASES:
        c = ((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF)
        closed = f"gas_collecting_bottle_{gid}"
        open_id = f"open_gas_collecting_bottle_{gid}"
        paint("empty_gas_collecting_bottle", closed, gas_mask_closed, c, 142)
        paint("open_gas_collecting_bottle_oxygen", open_id, gas_mask_open, c, 142)
        write_item_assets(closed)
        write_item_assets(open_id)
        zh[f"item.{NS}.{closed}"] = f"{zh_name}集气瓶"
        zh[f"item.{NS}.{open_id}"] = f"敞口的{zh_name}集气瓶"
        en[f"item.{NS}.{closed}"] = f"{en_name} Gas Bottle"
        en[f"item.{NS}.{open_id}"] = f"Open {en_name} Gas Bottle"

    # ---- Liquids: water bottle / dropper templates ----
    liquid_mask = content_mask("liquid_water", (143, 200, 232))
    open_liquid_mask = content_mask("open_liquid_water", (143, 200, 232))
    dropper_mask = content_mask("dropper_bottle_water", (143, 200, 232))
    for lid, color, zh_name, en_name in NEW_LIQUIDS:
        c = ((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF)
        closed = f"liquid_{lid}"
        open_id = f"open_liquid_{lid}"
        dropper = f"dropper_bottle_{lid}"
        bucket = f"liquid_{lid}_bucket"
        paint("liquid_water", closed, liquid_mask, c, 0x70)
        paint("open_liquid_water", open_id, open_liquid_mask, c, 0x70)
        paint("dropper_bottle_water", dropper, dropper_mask, c, 0x70)
        bucket_tint("liquid_water_bucket", bucket, c)
        for item_id in (closed, open_id, dropper, bucket):
            write_item_assets(item_id)
        zh[f"item.{NS}.{closed}"] = f"{zh_name}细口瓶"
        zh[f"item.{NS}.{open_id}"] = f"敞口的{zh_name}细口瓶"
        zh[f"item.{NS}.{dropper}"] = f"{zh_name}滴瓶"
        zh[f"item.{NS}.{bucket}"] = f"{zh_name}细口瓶桶"
        en[f"item.{NS}.{closed}"] = f"{en_name} Bottle"
        en[f"item.{NS}.{open_id}"] = f"Open {en_name} Bottle"
        en[f"item.{NS}.{dropper}"] = f"{en_name} Dropper Bottle"
        en[f"item.{NS}.{bucket}"] = f"{en_name} Bottle Bucket"

    # ---- Solids: sodium carbonate wide-mouth / loose powder templates ----
    solid_mask = content_mask("solid_sodium_carbonate", (240, 240, 232))
    open_solid_mask = content_mask("open_solid_sodium_carbonate", (240, 240, 232))
    for sid, color, zh_name, en_name in NEW_SOLIDS:
        c = ((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF)
        closed = f"solid_{sid}"
        open_id = f"open_solid_{sid}"
        loose = f"loose_{sid}"
        paint("solid_sodium_carbonate", closed, solid_mask, c)
        paint("open_solid_sodium_carbonate", open_id, open_solid_mask, c)
        recolor_by_ratio("loose_sodium_carbonate", loose, (240, 240, 232), c)
        for item_id in (closed, open_id, loose):
            write_item_assets(item_id)
        zh[f"item.{NS}.{closed}"] = f"{zh_name}（广口瓶）"
        zh[f"item.{NS}.{open_id}"] = f"敞口的{zh_name}（广口瓶）"
        zh[f"item.{NS}.{loose}"] = zh_name
        en[f"item.{NS}.{closed}"] = f"{en_name} (Wide-Mouth Bottle)"
        en[f"item.{NS}.{open_id}"] = f"Open {en_name} (Wide-Mouth Bottle)"
        en[f"item.{NS}.{loose}"] = en_name

    merge_lang("zh_cn", zh)
    merge_lang("en_us", en)
    print(f"OK: {len(NEW_GASES)} gases, {len(NEW_LIQUIDS)} liquids, "
          f"{len(NEW_SOLIDS)} solids -> assets + lang merged")


if __name__ == "__main__":
    main()
