"""Generate the ore system: 26 ores (block + deepslate + raw solid + chemical
processing chain), 12 intermediate oxides/salts, 14 missing metal solids.
Emits textures, item/block models, blockstates, loot tables, worldgen JSON,
lang entries, and appends to Solids/ChemicalInfoProvider/Reactions + Ores.java.
"""
import json
import colorsys
import random
import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mchemistry"
DATA = Path(__file__).resolve().parent.parent / "src/main/resources/data/mchemistry"
JAVA = Path(__file__).resolve().parent.parent / "src/main/java/com/example/chemistry"
MC_TEX = Path("/tmp/mc_tex/assets/minecraft/textures/block")
TEX = ROOT / "textures"
ITEM_TEX = TEX / "item"
BLOCK_TEX = TEX / "block"

random.seed(20260825)


def C(x):
    return ((x >> 16) & 255, (x >> 8) & 255, x & 255)


def hexc(x):
    return f"0x{x:06X}"


# ---------------------------------------------------------------- ore table
# (id, chinese, english, formula, color, deep color, hardness, minY, maxY,
#  count, vein size, biome tag, tool level 0=stone 1=iron 2=diamond, molar, bp)
ORES = [
    ("bauxite", "铝土矿", "Bauxite", "Al2O3", 0xC07A58, 0x905036, 1.5, 8, 96, 10, 8, "is_overworld", 0, 101.96, 2050),
    ("magnesite", "菱镁矿", "Magnesite", "MgCO3", 0xE0D8C8, 0xB8B09C, 1.5, 8, 96, 8, 6, "is_overworld", 0, 84.31, 1600),
    ("sphalerite", "闪锌矿", "Sphalerite", "ZnS", 0xC8A868, 0x9A7C42, 1.5, 0, 64, 9, 7, "is_overworld", 0, 97.44, 1850),
    ("cassiterite", "锡石", "Cassiterite", "SnO2", 0x989078, 0x6E6856, 1.5, 0, 64, 8, 6, "is_overworld", 0, 150.71, 1900),
    ("galena", "方铅矿", "Galena", "PbS", 0x9098A0, 0x666E76, 2.0, -32, 48, 7, 6, "is_overworld", 1, 239.27, 2000),
    ("pentlandite", "镍黄铁矿", "Pentlandite", "FeNi9S8", 0xC8B878, 0x968A52, 1.5, -16, 48, 8, 6, "is_overworld", 0, 769.2, 2000),
    ("cobaltite", "辉钴矿", "Cobaltite", "CoAsS", 0xB8A8C0, 0x867890, 2.0, -48, 16, 5, 5, "is_overworld", 1, 165.91, 2100),
    ("chromite", "铬铁矿", "Chromite", "FeCr2O4", 0x605850, 0x403A34, 3.0, -64, 8, 5, 5, "is_overworld", 2, 223.84, 2200),
    ("pyrolusite", "软锰矿", "Pyrolusite", "MnO2", 0x504850, 0x383038, 2.0, -32, 32, 6, 5, "is_overworld", 1, 86.94, 2100),
    ("ilmenite", "钛铁矿", "Ilmenite", "FeTiO3", 0x584840, 0x3E3028, 3.0, -64, 0, 4, 4, "is_overworld", 2, 151.71, 2300),
    ("molybdenite", "辉钼矿", "Molybdenite", "MoS2", 0x8898A8, 0x606E7C, 2.5, -48, 8, 4, 5, "is_overworld", 2, 160.07, 2200),
    ("wolframite", "黑钨矿", "Wolframite", "FeWO4", 0x504858, 0x36323E, 3.5, -64, -8, 3, 4, "is_overworld", 2, 303.69, 2400),
    ("argentite", "辉银矿", "Argentite", "Ag2S", 0x98A0A8, 0x6E767C, 2.0, -32, 16, 4, 4, "is_overworld", 1, 247.80, 2000),
    ("sperrylite", "砷铂矿", "Sperrylite", "PtAs2", 0xC8C8D8, 0x9292A0, 3.5, -64, -16, 2, 3, "is_overworld", 2, 324.92, 2500),
    ("spodumene", "锂辉石", "Spodumene", "LiAlSi2O6", 0xD8C8B8, 0xA09284, 2.0, -48, 0, 4, 4, "is_overworld", 1, 186.09, 2100),
    ("barite", "重晶石", "Barite", "BaSO4", 0xE0E0D8, 0xA8A8A0, 1.5, -16, 48, 6, 5, "is_overworld", 0, 233.39, 1900),
    ("bismuthinite", "辉铋矿", "Bismuthinite", "Bi2S3", 0xA8A8C0, 0x78788C, 2.5, -48, 0, 4, 4, "is_overworld", 2, 514.14, 2200),
    ("greenockite", "硫镉矿", "Greenockite", "CdS", 0xE8C040, 0xB29222, 2.5, -48, 0, 4, 4, "is_overworld", 2, 144.48, 2000),
    ("stibnite", "辉锑矿", "Stibnite", "Sb2S3", 0x889098, 0x5E6670, 2.5, -48, 0, 4, 4, "is_overworld", 2, 339.70, 2100),
    ("borax", "硼砂", "Borax", "Na2B4O7", 0xE0E8E8, 0xA8B4B4, 1.5, 0, 64, 6, 5, "is_overworld", 0, 381.37, 1800),
    ("sulfur_ore", "硫磺矿", "Sulfur Ore", "S", 0xD8C830, 0xA89A1C, 1.0, 8, 64, 10, 8, "is_badlands", 0, 32.06, 445),
    ("apatite", "磷灰石", "Apatite", "Ca5(PO4)3F", 0x90C8A8, 0x68967A, 2.0, -32, 32, 6, 5, "is_overworld", 1, 504.31, 2100),
    ("fluorite", "萤石", "Fluorite", "CaF2", 0xB8D0E8, 0x849CB0, 1.5, -16, 48, 6, 5, "is_overworld", 0, 78.07, 1900),
    ("sylvite", "钾石盐", "Sylvite", "KCl", 0xD8A0A0, 0xA26E6E, 1.0, 8, 80, 7, 6, "is_badlands", 0, 74.55, 1500),
    ("halite", "岩盐", "Halite", "NaCl", 0xF0E8E0, 0xB8AEA4, 1.0, 8, 80, 8, 7, "is_badlands", 0, 58.44, 1465),
    ("limestone", "石灰石", "Limestone", "CaCO3", 0xC8C8C0, 0x92928A, 1.0, 0, 80, 10, 8, "is_overworld", 0, 100.09, 1600),
]


def r(*args):
    return args


# Vanilla iron/copper/gold ores also get the chemical treatment: their blocks
# drop loose_<id> (no new blocks/worldgen) and smelting is replaced by a
# high-temperature reduction reaction.
# (id, chinese, english, formula, color, molar, bp, [block ids], [reactions])
VANILLA_ORES = [
    ("iron_ore", "铁矿石", "Iron Ore", "Fe2O3", 0xC87A5A, 159.69, 1800,
     ["iron_ore", "deepslate_iron_ore"],
     [r([("solid", "iron_ore", 2), ("solid", "charcoal", 3)],
        [("solid", "iron", 2), ("vent", "", 3)],
        "铁矿石 + 碳 → 铁 + 二氧化碳↑（高温）", 0.5, 1000, "", 0)]),
    ("copper_ore", "铜矿石", "Copper Ore", "Cu2O", 0xE8A858, 143.09, 1800,
     ["copper_ore", "deepslate_copper_ore"],
     [r([("solid", "copper_ore", 1), ("solid", "charcoal", 1)],
        [("solid", "copper", 2), ("vent", "", 1)],
        "铜矿石 + 碳 → 铜 + 二氧化碳↑（高温）", 0.5, 900, "", 0)]),
    ("gold_ore", "金矿石", "Gold Ore", "Au", 0xF0D060, 196.97, 2000,
     ["gold_ore", "deepslate_gold_ore"],
     [r([("solid", "gold_ore", 1)], [("solid", "gold", 1)],
        "金矿石 → 金（高温）", 0.5, 600, "", 0)]),
]

# missing metal solids: (id, chinese, english, formula, color, molar, bp)
METALS = [
    ("tin", "锡", "Tin", "Sn", 0xD0D8D8, 118.71, 2602),
    ("lead", "铅", "Lead", "Pb", 0x9098A0, 207.20, 1749),
    ("nickel", "镍", "Nickel", "Ni", 0xC8D0D8, 58.69, 2913),
    ("cobalt", "钴", "Cobalt", "Co", 0x98A8D0, 58.93, 2927),
    ("chromium", "铬", "Chromium", "Cr", 0xD0D8E8, 52.00, 2671),
    ("manganese", "锰", "Manganese", "Mn", 0xB8A8A0, 54.94, 2061),
    ("titanium", "钛", "Titanium", "Ti", 0xB8C0C8, 47.87, 3287),
    ("molybdenum", "钼", "Molybdenum", "Mo", 0x98A0A8, 95.94, 4612),
    ("tungsten", "钨", "Tungsten", "W", 0x808890, 183.84, 5555),
    ("platinum", "铂", "Platinum", "Pt", 0xD0D8E0, 195.08, 3825),
    ("bismuth", "铋", "Bismuth", "Bi", 0xC8A8C0, 208.98, 1564),
    ("cadmium", "镉", "Cadmium", "Cd", 0xD8D8B8, 112.41, 767),
    ("antimony", "锑", "Antimony", "Sb", 0xA8B0B8, 121.76, 1587),
    ("boron", "硼", "Boron", "B", 0xA08868, 10.81, 4000),
]

# intermediate oxides/salts: (id, chinese, english, formula, color, molar, bp)
INTERMEDIATES = [
    ("zinc_oxide", "氧化锌", "Zinc Oxide", "ZnO", 0xE8E8E0, 81.38, 1800),
    ("lead_oxide", "氧化铅", "Lead(II) Oxide", "PbO", 0xE0C050, 223.20, 1535),
    ("nickel_oxide", "氧化镍", "Nickel(II) Oxide", "NiO", 0x90C8A0, 74.69, 1950),
    ("cobalt_oxide", "氧化钴", "Cobalt(II) Oxide", "CoO", 0x8090A8, 74.93, 2000),
    ("chromium_oxide", "三氧化二铬", "Chromium(III) Oxide", "Cr2O3", 0x70A070, 151.99, 3000),
    ("molybdenum_oxide", "三氧化钼", "Molybdenum(VI) Oxide", "MoO3", 0xD8E8F0, 143.94, 1155),
    ("tungsten_oxide", "三氧化钨", "Tungsten(VI) Oxide", "WO3", 0xE8E8A0, 231.84, 1700),
    ("bismuth_oxide", "三氧化二铋", "Bismuth(III) Oxide", "Bi2O3", 0xE8D8A0, 465.96, 1890),
    ("cadmium_oxide", "氧化镉", "Cadmium Oxide", "CdO", 0xB88048, 128.41, 1559),
    ("antimony_oxide", "三氧化二锑", "Antimony(III) Oxide", "Sb2O3", 0xF0F0F0, 291.52, 1550),
    ("barium_sulfide", "硫化钡", "Barium Sulfide", "BaS", 0xE0E0D8, 169.39, 1700),
    ("manganese_chloride", "氯化锰", "Manganese(II) Chloride", "MnCl2", 0xF0B8D8, 125.84, 1190),
]


# ----------------------------------------------------------------- reactions
# (reactants, products, display, speed, temp, catalyst, pressure)
def r(*args):
    return args


CHAINS = {
    "bauxite": [r([("solid", "bauxite", 2), ("solid", "charcoal", 3)],
                  [("solid", "aluminium", 2), ("vent", "", 3)],
                  "铝土矿 + 碳 → 铝 + 二氧化碳↑（高温）", 0.4, 1200, "", 0)],
    "magnesite": [r([("solid", "magnesite", 1)], [("solid", "magnesium_oxide", 1), ("vent", "", 1)],
                    "菱镁矿 → 氧化镁 + 二氧化碳↑（高温）", 0.6, 700, "", 0),
                  r([("solid", "magnesium_oxide", 1), ("solid", "charcoal", 1)],
                    [("solid", "magnesium", 1), ("vent", "", 1)],
                    "氧化镁 + 碳 → 镁 + 二氧化碳↑（高温）", 0.4, 1600, "", 0)],
    "sphalerite": [r([("solid", "sphalerite", 1)], [("solid", "zinc_oxide", 1), ("vent", "", 1)],
                     "闪锌矿 → 氧化锌 + 二氧化硫↑（高温）", 0.6, 900, "", 0),
                   r([("solid", "zinc_oxide", 1), ("solid", "charcoal", 1)],
                     [("solid", "zinc", 1), ("vent", "", 1)],
                     "氧化锌 + 碳 → 锌 + 二氧化碳↑（高温）", 0.5, 1200, "", 0)],
    "cassiterite": [r([("solid", "cassiterite", 1), ("solid", "charcoal", 1)],
                      [("solid", "tin", 1), ("vent", "", 1)],
                      "锡石 + 碳 → 锡 + 二氧化碳↑（高温）", 0.6, 1000, "", 0)],
    "galena": [r([("solid", "galena", 1)], [("solid", "lead_oxide", 1), ("vent", "", 1)],
                 "方铅矿 → 氧化铅 + 二氧化硫↑（高温）", 0.6, 900, "", 0),
               r([("solid", "lead_oxide", 1), ("solid", "charcoal", 1)],
                 [("solid", "lead", 1), ("vent", "", 1)],
                 "氧化铅 + 碳 → 铅 + 二氧化碳↑（高温）", 0.5, 1100, "", 0)],
    "pentlandite": [r([("solid", "pentlandite", 1)], [("solid", "nickel_oxide", 9), ("vent", "", 8)],
                      "镍黄铁矿 → 氧化镍 + 二氧化硫↑（高温）", 0.6, 900, "", 0),
                    r([("solid", "nickel_oxide", 1), ("solid", "charcoal", 1)],
                      [("solid", "nickel", 1), ("vent", "", 1)],
                      "氧化镍 + 碳 → 镍 + 二氧化碳↑（高温）", 0.5, 1100, "", 0)],
    "cobaltite": [r([("solid", "cobaltite", 1)], [("solid", "cobalt_oxide", 1), ("vent", "", 2)],
                    "辉钴矿 → 氧化钴 + 二氧化硫↑ + 砷（高温）", 0.5, 900, "", 0),
                  r([("solid", "cobalt_oxide", 1), ("solid", "charcoal", 1)],
                    [("solid", "cobalt", 1), ("vent", "", 1)],
                    "氧化钴 + 碳 → 钴 + 二氧化碳↑（高温）", 0.5, 1100, "", 0)],
    "chromite": [r([("solid", "chromite", 2), ("solid", "charcoal", 3)],
                   [("solid", "chromium", 2), ("solid", "iron", 1), ("vent", "", 3)],
                   "铬铁矿 + 碳 → 铬 + 铁 + 二氧化碳↑（高温）", 0.4, 1500, "", 0)],
    "pyrolusite": [r([("solid", "pyrolusite", 1), ("liquid", "hydrochloric_acid", 4)],
                     [("solid", "manganese_chloride", 1), ("vent", "", 1), ("liquid", "water", 2)],
                     "软锰矿 + 盐酸 → 氯化锰 + 氯气↑ + 水", 1.0, 20, "", 0),
                   r([("solid", "manganese_chloride", 1), ("solid", "sodium", 2)],
                     [("solid", "manganese", 1), ("solid", "sodium_chloride", 2)],
                     "氯化锰 + 钠 → 锰 + 氯化钠（高温）", 0.5, 800, "", 0)],
    "ilmenite": [r([("solid", "ilmenite", 2), ("solid", "charcoal", 3)],
                   [("solid", "titanium", 2), ("solid", "iron", 1), ("vent", "", 3)],
                   "钛铁矿 + 碳 → 钛 + 铁 + 二氧化碳↑（高温）", 0.4, 1600, "", 0)],
    "molybdenite": [r([("solid", "molybdenite", 1)], [("solid", "molybdenum_oxide", 1), ("vent", "", 2)],
                      "辉钼矿 → 三氧化钼 + 二氧化硫↑（高温）", 0.5, 700, "", 0),
                    r([("solid", "molybdenum_oxide", 1), ("solid", "charcoal", 1)],
                      [("solid", "molybdenum", 1), ("vent", "", 1)],
                      "三氧化钼 + 碳 → 钼 + 二氧化碳↑（高温）", 0.5, 1200, "", 0)],
    "wolframite": [r([("solid", "wolframite", 1), ("solid", "charcoal", 3)],
                     [("solid", "tungsten", 1), ("solid", "iron", 1), ("vent", "", 3)],
                     "黑钨矿 + 碳 → 钨 + 铁 + 二氧化碳↑（高温）", 0.3, 1600, "", 0)],
    "argentite": [r([("solid", "argentite", 1)], [("solid", "silver", 2), ("vent", "", 1)],
                    "辉银矿 → 银 + 二氧化硫↑（高温）", 0.6, 800, "", 0)],
    "sperrylite": [r([("solid", "sperrylite", 1)], [("solid", "platinum", 1), ("vent", "", 2)],
                     "砷铂矿 → 铂 + 砷（高温）", 0.5, 1000, "", 0)],
    "spodumene": [r([("solid", "spodumene", 2), ("solid", "charcoal", 2)],
                    [("solid", "lithium", 2), ("solid", "aluminium_oxide", 1),
                     ("solid", "silicon_dioxide", 4), ("vent", "", 2)],
                    "锂辉石 + 碳 → 锂 + 氧化铝 + 二氧化硅 + 二氧化碳↑（高温）", 0.4, 1500, "", 0)],
    "barite": [r([("solid", "barite", 1), ("solid", "charcoal", 4)],
                 [("solid", "barium_sulfide", 1), ("vent", "", 4)],
                 "重晶石 + 碳 → 硫化钡 + 二氧化碳↑（高温）", 0.5, 1000, "", 0),
               r([("solid", "barium_sulfide", 1), ("liquid", "hydrochloric_acid", 2)],
                 [("solid", "barium_chloride", 1), ("vent", "", 1)],
                 "硫化钡 + 盐酸 → 氯化钡 + 硫化氢↑", 1.0, 20, "", 0)],
    "bismuthinite": [r([("solid", "bismuthinite", 1)], [("solid", "bismuth_oxide", 1), ("vent", "", 3)],
                       "辉铋矿 → 氧化铋 + 二氧化硫↑（高温）", 0.5, 800, "", 0),
                     r([("solid", "bismuth_oxide", 1), ("solid", "charcoal", 1)],
                       [("solid", "bismuth", 2), ("vent", "", 1)],
                       "氧化铋 + 碳 → 铋 + 二氧化碳↑（高温）", 0.5, 1000, "", 0)],
    "greenockite": [r([("solid", "greenockite", 1)], [("solid", "cadmium_oxide", 1), ("vent", "", 1)],
                      "硫镉矿 → 氧化镉 + 二氧化硫↑（高温）", 0.5, 800, "", 0),
                    r([("solid", "cadmium_oxide", 1), ("solid", "charcoal", 1)],
                      [("solid", "cadmium", 1), ("vent", "", 1)],
                      "氧化镉 + 碳 → 镉 + 二氧化碳↑（高温）", 0.5, 1000, "", 0)],
    "stibnite": [r([("solid", "stibnite", 1)], [("solid", "antimony_oxide", 1), ("vent", "", 3)],
                   "辉锑矿 → 氧化锑 + 二氧化硫↑（高温）", 0.5, 800, "", 0),
                 r([("solid", "antimony_oxide", 1), ("solid", "charcoal", 1)],
                   [("solid", "antimony", 2), ("vent", "", 1)],
                   "氧化锑 + 碳 → 锑 + 二氧化碳↑（高温）", 0.5, 1000, "", 0)],
    "borax": [r([("solid", "borax", 1), ("liquid", "hydrochloric_acid", 2)],
                [("solid", "boric_acid", 4), ("liquid", "sodium_chloride_solution", 2)],
                "硼砂 + 盐酸 → 硼酸 + 氯化钠溶液", 1.0, 20, "", 0),
              r([("solid", "boric_acid", 2), ("solid", "magnesium", 3)],
                [("solid", "boron", 2), ("solid", "magnesium_oxide", 3)],
                "硼酸 + 镁 → 硼 + 氧化镁（高温）", 0.5, 900, "", 0)],
    "sulfur_ore": [r([("solid", "sulfur_ore", 1)], [("solid", "sulfur", 1)],
                     "硫磺矿 → 硫（高温升华提纯）", 1.0, 400, "", 0)],
    "apatite": [r([("solid", "apatite", 1), ("liquid", "sulfuric_acid_concentrated", 3)],
                  [("liquid", "phosphoric_acid", 1), ("solid", "calcium_sulfate", 1), ("vent", "", 1)],
                  "磷灰石 + 浓硫酸 → 磷酸 + 硫酸钙 + 氟化氢↑（微热）", 0.8, 80, "", 0),
                r([("liquid", "phosphoric_acid", 2), ("solid", "charcoal", 1)],
                  [("solid", "white_phosphorus", 1), ("vent", "", 1)],
                  "磷酸 + 碳 → 白磷 + 一氧化碳↑（高温）", 0.4, 1500, "", 0)],
    "fluorite": [r([("solid", "fluorite", 1), ("liquid", "sulfuric_acid_concentrated", 1)],
                   [("solid", "calcium_sulfate", 1), ("vent", "", 2)],
                   "萤石 + 浓硫酸 → 硫酸钙 + 氟化氢↑（高温）", 0.6, 200, "", 0)],
    "sylvite": [r([("solid", "sylvite", 2)], [("solid", "potassium", 2), ("vent", "", 1)],
                  "钾石盐 → 钾 + 氯气↑（高温电解）", 0.5, 800, "", 0)],
    "halite": [r([("solid", "halite", 2)], [("solid", "sodium", 2), ("vent", "", 1)],
                 "岩盐 → 钠 + 氯气↑（高温电解）", 0.5, 800, "", 0)],
    "limestone": [r([("solid", "limestone", 1)], [("solid", "calcium_oxide", 1), ("vent", "", 1)],
                    "石灰石 → 生石灰 + 二氧化碳↑（高温）", 0.6, 900, "", 0)],
}


# --------------------------------------------------------------- textures
def shade(c, delta):
    return (max(0, min(255, c[0] + delta)), max(0, min(255, c[1] + delta)),
            max(0, min(255, c[2] + delta)))


def recolor_ore_spots(im, target):
    """Keep the vanilla iron-ore spot SHAPE, scale the spot brightness onto the
    target ore colour: gray stone pixels stay untouched, coloured spot pixels
    become target * (spot brightness / average spot brightness).
    """
    px = im.load()
    grays = []
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            if max(r, g, b) - min(r, g, b) < 24:
                continue
            grays.append((r + g + b) / 3)
    if not grays:
        return
    avg = sum(grays) / len(grays)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a or max(r, g, b) - min(r, g, b) < 24:
                continue
            f = ((r + g + b) / 3) / avg
            px[x, y] = (min(255, int(target[0] * f)), min(255, int(target[1] * f)),
                        min(255, int(target[2] * f)), a)


def gen_ore_block_textures():
    # Vanilla templates: stone + iron-ore spots / deepslate + deepslate iron spots.
    iron = Image.open(MC_TEX / "iron_ore.png").convert("RGBA")
    deep_iron = Image.open(MC_TEX / "deepslate_iron_ore.png").convert("RGBA")
    BLOCK_TEX.mkdir(parents=True, exist_ok=True)
    for ore_id, _cn, _en, _f, color, deep_color, *_rest in ORES:
        a = iron.copy()
        recolor_ore_spots(a, C(color))
        a.save(BLOCK_TEX / f"ore_{ore_id}.png")
        b = deep_iron.copy()
        recolor_ore_spots(b, C(deep_color))
        b.save(BLOCK_TEX / f"deepslate_ore_{ore_id}.png")


GLASS = {(0x5D, 0x8F, 0xC2), (0x8B, 0xAD, 0xD0), (0xD4, 0xE5, 0xF7), (0xB3, 0xCF, 0xEC)}


def tint_image(im, src_nominal, dst_nominal, keep_glass=False):
    px = im.load()
    sr, sg, sb = src_nominal
    dr, dg, db = dst_nominal
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            if keep_glass and (r, g, b) in GLASS:
                continue
            px[x, y] = (min(255, r * dr // max(1, sr)), min(255, g * dg // max(1, sg)),
                        min(255, b * db // max(1, sb)), a)


def gen_solid_textures():
    ITEM_TEX.mkdir(parents=True, exist_ok=True)
    lump_solid = Image.open(ITEM_TEX / "solid_iron.png").convert("RGBA")
    lump_open = Image.open(ITEM_TEX / "open_solid_iron.png").convert("RGBA")
    lump_loose = Image.open(ITEM_TEX / "loose_iron.png").convert("RGBA")
    pow_solid = Image.open(ITEM_TEX / "solid_sodium_carbonate.png").convert("RGBA")
    pow_open = Image.open(ITEM_TEX / "open_solid_sodium_carbonate.png").convert("RGBA")
    pow_loose = Image.open(ITEM_TEX / "loose_sodium_carbonate.png").convert("RGBA")
    IRON = (0x68, 0x68, 0x6C)
    LOOSE_IRON = (0x7E, 0x7E, 0x83)
    POW = (0xF0, 0xF0, 0xE8)
    LOOSE_POW = (0x81, 0x81, 0x7C)

    def emit(sid, color, form):
        if form == "LUMP":
            s, o, l, sn, ln = lump_solid, lump_open, lump_loose, IRON, LOOSE_IRON
        else:
            s, o, l, sn, ln = pow_solid, pow_open, pow_loose, POW, LOOSE_POW
        c = C(color)
        a = s.copy(); tint_image(a, sn, c, keep_glass=True); a.save(ITEM_TEX / f"solid_{sid}.png")
        b = o.copy(); tint_image(b, sn, c, keep_glass=True); b.save(ITEM_TEX / f"open_solid_{sid}.png")
        d = l.copy(); tint_image(d, ln, c); d.save(ITEM_TEX / f"loose_{sid}.png")

    for ore_id, _cn, _en, _f, color, _dc, *_rest in ORES:
        emit(ore_id, color, "LUMP")
    for v in VANILLA_ORES:
        emit(v[0], v[4], "LUMP")
    for mid in INTERMEDIATES:
        emit(mid[0], mid[4], "POWDER")
    for met in METALS:
        emit(met[0], met[4], "LUMP")


# ------------------------------------------------------------- JSON writers
def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2))


def item_model_ref(model):
    return {"model": {"type": "minecraft:model", "model": model}}


def gen_item_assets():
    all_solids = [(o[0], "LUMP") for o in ORES] + \
                 [(v[0], "LUMP") for v in VANILLA_ORES] + \
                 [(m[0], "POWDER") for m in INTERMEDIATES] + \
                 [(m[0], "LUMP") for m in METALS]
    for sid, _form in all_solids:
        for prefix in ("solid", "open_solid", "loose"):
            item_id = f"{prefix}_{sid}" if prefix != "loose" else f"loose_{sid}"
            write(ROOT / "items" / f"{item_id}.json", item_model_ref(f"mchemistry:item/{item_id}"))
            write(ROOT / "models" / "item" / f"{item_id}.json",
                  {"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"mchemistry:item/{item_id}"}})

    for ore_id, _cn, _en, _f, _c, _dc, *_rest in ORES:
        for base in (f"ore_{ore_id}", f"deepslate_ore_{ore_id}"):
            write(ROOT / "items" / f"{base}.json", item_model_ref(f"mchemistry:item/{base}"))
            write(ROOT / "models" / "item" / f"{base}.json", {"parent": f"mchemistry:block/{base}"})
            write(ROOT / "models" / "block" / f"{base}.json",
                  {"parent": "minecraft:block/cube_all", "textures": {"all": f"mchemistry:block/{base}"}})
            write(ROOT / "blockstates" / f"{base}.json", {"variants": {"": {"model": f"mchemistry:block/{base}"}}})
            write(DATA / "loot_table" / "blocks" / f"{base}.json",
                  {"type": "minecraft:block", "pools": [{
                      "rolls": 1,
                      "entries": [{"type": "minecraft:item", "name": f"mchemistry:loose_{ore_id}"}],
                      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def gen_worldgen():
    for ore_id, _cn, _en, _f, _c, _dc, _hard, min_y, max_y, count, size, biome, _tool, _m, _bp in ORES:
        cfg = {
            "type": "minecraft:ore",
            "config": {
                "discard_chance_on_air_exposure": 0.0,
                "size": size,
                "targets": [
                    {"target": {"predicate_type": "minecraft:tag_match",
                                "tag": "minecraft:stone_ore_replaceables"},
                     "state": {"Name": f"mchemistry:ore_{ore_id}"}},
                    {"target": {"predicate_type": "minecraft:tag_match",
                                "tag": "minecraft:deepslate_ore_replaceables"},
                     "state": {"Name": f"mchemistry:deepslate_ore_{ore_id}"}},
                ],
            },
        }
        write(DATA / "worldgen" / "configured_feature" / f"ore_{ore_id}.json", cfg)
        placed = {
            "feature": f"mchemistry:ore_{ore_id}",
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range",
                 "height": {"type": "minecraft:uniform",
                            "min_inclusive": {"absolute": min_y},
                            "max_inclusive": {"absolute": max_y}}},
                {"type": "minecraft:biome"},
            ],
        }
        write(DATA / "worldgen" / "placed_feature" / f"ore_{ore_id}.json", placed)
        write(DATA / "neoforge" / "biome_modifier" / f"ore_{ore_id}.json",
              {"type": "neoforge:add_features",
               "biomes": f"#minecraft:{biome}",
               "features": f"mchemistry:ore_{ore_id}",
               "step": "underground_ores"})


def gen_block_tags():
    mineable = {"replace": False, "values": []}
    iron = {"replace": False, "values": []}
    diamond = {"replace": False, "values": []}
    for ore_id, _cn, _en, _f, _c, _dc, _hard, _my, _xy, _co, _si, _bio, tool, _m, _bp in ORES:
        for base in (f"ore_{ore_id}", f"deepslate_ore_{ore_id}"):
            mineable["values"].append(f"mchemistry:{base}")
            if tool >= 1:
                iron["values"].append(f"mchemistry:{base}")
            if tool >= 2:
                diamond["values"].append(f"mchemistry:{base}")
    # These are MINECRAFT's block tags (required by requiresCorrectToolForDrops),
    # so they must live under data/minecraft/tags/block/, not the mod namespace.
    minecraft_tags = DATA.parent / "minecraft" / "tags" / "block"
    write(minecraft_tags / "mineable" / "pickaxe.json", mineable)
    write(minecraft_tags / "needs_iron_tool.json", iron)
    write(minecraft_tags / "needs_diamond_tool.json", diamond)


# ------------------------------------------------------------ Java writers
def append_to_list_java(path, anchor_regex, items, sep=",\n"):
    src = path.read_text()
    # The anchor regex must match up to the END of the anchor line (no newline),
    # so the separator comma is glued to the anchor's last token, not left on
    # its own line. List.of anchors have no trailing comma of their own.
    m = re.search(anchor_regex, src)
    assert m, f"anchor not found in {path}"
    # Java method calls do not allow a trailing comma: drop it from the last item.
    joined = "\n".join(items)
    if joined.rstrip().endswith(","):
        joined = joined.rstrip()[:-1]
    insert = sep + joined + "\n"
    new_src = src[:m.end()] + insert + src[m.end():]
    path.write_text(new_src)


def java_solid_line(rec):
    sid, cn, en, formula, color, form = rec
    return f'            new Solid("{sid}", "{formula}", "{en}", "{cn}", {hexc(color)}, SolidForm.{form}, ""),'


def gen_java():
    # --- Solids.java / ChemicalInfoProvider: two idempotent batches ---
    ore_solids = [(o[0], o[1], o[2], o[3], o[4], "LUMP") for o in ORES] + \
                 [(m[0], m[1], m[2], m[3], m[4], "POWDER") for m in INTERMEDIATES] + \
                 [(m[0], m[1], m[2], m[3], m[4], "LUMP") for m in METALS]
    vanilla_solids = [(v[0], v[1], v[2], v[3], v[4], "LUMP") for v in VANILLA_ORES]
    all_meta = ([(o[0], o[1], o[2], o[3], o[4], o[13], o[14]) for o in ORES] +
                [(v[0], v[1], v[2], v[3], v[4], v[5], v[6]) for v in VANILLA_ORES] +
                [(m[0], m[1], m[2], m[3], m[4], m[5], m[6]) for m in INTERMEDIATES] +
                [(m[0], m[1], m[2], m[3], m[4], m[5], m[6]) for m in METALS])

    def solid_lines(batch):
        return [java_solid_line(s) for s in batch]

    def info_lines(batch):
        chem, mass = [], []
        for rec in batch:
            sid, cn, en, formula, color, form = rec
            molar = next(x[5] for x in all_meta if x[0] == sid)
            bp = next(x[6] for x in all_meta if x[0] == sid)
            chem.append(
                f'        CHEMICALS.put("solid_{sid}", new ChemicalInfo("{formula}", "{molar:.2f}", '
                f'"low", "none", "none", "n/a|neutral", "-", "{cn}", "无味", "{bp}°C", "{bp}°C"));')
            mass.append(f'        MOLAR_MASS.put("solid_{sid}", {molar});')
            mass.append(f'        BOILING_POINT.put("solid_{sid}", {bp});')
        return chem, mass

    solids_src = (JAVA / "data" / "Solids.java").read_text()
    if 'new Solid("bauxite",' not in solids_src:
        append_to_list_java(JAVA / "data" / "Solids.java",
                            r'new Solid\("calcium_phosphate",[^\n]*', solid_lines(ore_solids))
    if 'new Solid("iron_ore",' not in solids_src:
        append_to_list_java(JAVA / "data" / "Solids.java",
                            r'new Solid\("boron",[^\n]*', solid_lines(vanilla_solids))

    provider_src = (JAVA / "data" / "ChemicalInfoProvider.java").read_text()
    chem_ore, mass_ore = info_lines(ore_solids)
    chem_vanilla, mass_vanilla = info_lines(vanilla_solids)
    if 'CHEMICALS.put("solid_bauxite"' not in provider_src:
        append_to_list_java(JAVA / "data" / "ChemicalInfoProvider.java",
                            r'CHEMICALS.put\("solid_calcium_phosphate",[^\n]*', chem_ore, sep="\n")
    if 'CHEMICALS.put("solid_iron_ore"' not in provider_src:
        append_to_list_java(JAVA / "data" / "ChemicalInfoProvider.java",
                            r'CHEMICALS.put\("solid_calcium_phosphate",[^\n]*', chem_vanilla, sep="\n")
    if 'MOLAR_MASS.put("solid_bauxite"' not in provider_src:
        append_to_list_java(JAVA / "data" / "ChemicalInfoProvider.java",
                            r'BOILING_POINT.put\("solid_calcium_phosphate", 1600\);', mass_ore, sep="\n")
    if 'MOLAR_MASS.put("solid_iron_ore"' not in provider_src:
        append_to_list_java(JAVA / "data" / "ChemicalInfoProvider.java",
                            r'BOILING_POINT.put\("solid_calcium_phosphate", 1600\);', mass_vanilla, sep="\n")

    # --- Reactions.java: two idempotent batches ---
    def reaction_lines(batch):
        out = []
        for ore_id, chain_list in batch:
            for chain in chain_list:
                reactants, products, display, speed, temp, catalyst, pressure = chain
                ing = ", ".join(f'new Ingredient("{t}", "{i}", {n})' for t, i, n in reactants)
                prod = ", ".join(f'new Product("{t}", "{i}", {n})' for t, i, n in products)
                out.append(
                    f'            new Reaction(List.of({ing}), List.of({prod}), "{display}", '
                    f'{speed}, {temp}, "{catalyst}", {pressure}),')
        return out

    reactions_src = (JAVA / "data" / "Reactions.java").read_text()
    ore_reactions = reaction_lines([(o[0], CHAINS[o[0]]) for o in ORES])
    vanilla_reactions = reaction_lines([(v[0], v[8]) for v in VANILLA_ORES])
    if "铝土矿 + 碳" not in reactions_src:
        append_to_list_java(JAVA / "data" / "Reactions.java",
                            r'new Reaction\([^\n]*氮气 \+ 氧气 → 一氧化氮（高温）[^\n]*', ore_reactions)
    if "铁矿石 + 碳" not in reactions_src:
        append_to_list_java(JAVA / "data" / "Reactions.java",
                            r'new Reaction\([^\n]*石灰石 → 生石灰[^\n]*', vanilla_reactions)

    # Ores.java (registration data)
    # Only what the runtime registration needs: id + hardness. Generation-time
    # data (colours, worldgen params) lives in the Python table, not the Java
    # record, so it never ships in the jar.
    ore_lines = []
    for o in ORES:
        ore_id, _cn, _en, _f, _c, _dc, hard, *_rest = o
        ore_lines.append(f'        new OreDef("{ore_id}", {hard}F),')
    joined_ores = "\n".join(ore_lines)
    if joined_ores.rstrip().endswith(","):
        joined_ores = joined_ores.rstrip()[:-1]
    ores_java = f"""package com.example.chemistry.data;

import java.util.List;

/** Generated ore table (see work/gen_ores.py). */
public final class Ores {{

    public record OreDef(String id, float hardness) {{
    }}

    public static final List<OreDef> ALL = List.of(
{joined_ores}
    );

    private Ores() {{
    }}
}}
"""
    (JAVA / "data" / "Ores.java").write_text(ores_java)


def gen_lang():
    zh = json.loads((ROOT / "lang" / "zh_cn.json").read_text())
    en = json.loads((ROOT / "lang" / "en_us.json").read_text())
    for ore_id, cn, en_name, *_ in ORES:
        # The mineral name already ends in 矿 for most ores (砷铂矿/闪锌矿...),
        # and names like 锡石/重晶石/石灰石 are fine on their own — never add
        # another 矿 suffix.
        zh[f"block.mchemistry.ore_{ore_id}"] = cn
        zh[f"block.mchemistry.deepslate_ore_{ore_id}"] = "深层" + cn
        en[f"block.mchemistry.ore_{ore_id}"] = en_name + " Ore"
        en[f"block.mchemistry.deepslate_ore_{ore_id}"] = "Deepslate " + en_name + " Ore"
        zh[f"item.mchemistry.solid_{ore_id}"] = cn + "（广口瓶）"
        zh[f"item.mchemistry.open_solid_{ore_id}"] = "敞口的" + cn + "（广口瓶）"
        zh[f"item.mchemistry.loose_{ore_id}"] = cn
        en[f"item.mchemistry.solid_{ore_id}"] = en_name + " (Wide-Mouth Bottle)"
        en[f"item.mchemistry.open_solid_{ore_id}"] = "Open " + en_name + " (Wide-Mouth Bottle)"
        en[f"item.mchemistry.loose_{ore_id}"] = en_name
    for mid in INTERMEDIATES:
        mid_id, cn, en_name, *_ = mid
        for prefix, zlabel, elabel in (("solid", cn + "（广口瓶）", en_name + " (Wide-Mouth Bottle)"),
                                       ("open_solid", "敞口的" + cn + "（广口瓶）", "Open " + en_name + " (Wide-Mouth Bottle)"),
                                       ("loose", cn, en_name)):
            zh[f"item.mchemistry.{prefix}_{mid_id}"] = zlabel
            en[f"item.mchemistry.{prefix}_{mid_id}"] = elabel
    for met in METALS:
        mid, cn, en_name, *_ = met
        for prefix, zlabel, elabel in (("solid", cn + "块（广口瓶）", en_name + " Block (Wide-Mouth Bottle)"),
                                       ("open_solid", "敞口的" + cn + "块（广口瓶）", "Open " + en_name + " Block (Wide-Mouth Bottle)"),
                                       ("loose", cn + "块", en_name + " Block")):
            zh[f"item.mchemistry.{prefix}_{mid}"] = zlabel
            en[f"item.mchemistry.{prefix}_{mid}"] = elabel
    for v in VANILLA_ORES:
        vid, cn, en_name, *_ = v
        for prefix, zlabel, elabel in (("solid", cn + "（广口瓶）", en_name + " (Wide-Mouth Bottle)"),
                                       ("open_solid", "敞口的" + cn + "（广口瓶）", "Open " + en_name + " (Wide-Mouth Bottle)"),
                                       ("loose", cn, en_name)):
            zh[f"item.mchemistry.{prefix}_{vid}"] = zlabel
            en[f"item.mchemistry.{prefix}_{vid}"] = elabel
    (ROOT / "lang" / "zh_cn.json").write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n")
    (ROOT / "lang" / "en_us.json").write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n")


def gen_vanilla_ore_loot():
    """Override vanilla iron/copper/gold ore loot: mining drops loose_<id>
    (1-3, fortune scaled) instead of raw iron/copper/gold, so the metal can
    only be obtained through the chemical reduction chain."""
    mc_loot = DATA.parent / "minecraft" / "loot_table" / "blocks"
    for v in VANILLA_ORES:
        vid = v[0]
        for block in v[7]:
            write(mc_loot / f"{block}.json", {
                "type": "minecraft:block",
                "pools": [{
                    "rolls": 1,
                    "entries": [{
                        "type": "minecraft:item",
                        "name": f"mchemistry:loose_{vid}",
                        "functions": [
                            {"function": "minecraft:set_count",
                             "count": {"type": "minecraft:uniform", "min": 1.0, "max": 3.0},
                             "add": False},
                            {"function": "minecraft:apply_bonus",
                             "enchantment": "minecraft:fortune",
                             "formula": "minecraft:uniform_bonus_count",
                             "parameters": {"bonusMultiplier": 1}},
                        ],
                    }],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }],
            })


if __name__ == "__main__":
    gen_ore_block_textures()
    gen_solid_textures()
    gen_item_assets()
    gen_worldgen()
    gen_block_tags()
    gen_vanilla_ore_loot()
    gen_java()
    gen_lang()
    print("OK: ores generated",
          len(ORES), "ores,", len(INTERMEDIATES), "intermediates,", len(METALS), "metals")
