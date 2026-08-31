#!/usr/bin/env python3
"""Remove the per-reagent item-definition JSONs that the unified bottles
replaced. Model files (models/item/*.json) and textures are KEPT because the
unified select models still reference them; buckets, loose solids, elements and
vessels are also kept."""
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
ITEMS = os.path.join(ROOT, "src", "main", "resources", "assets", "mchemistry", "items")


def should_remove(name):
    if not name.endswith(".json"):
        return False
    base = name[: -len(".json")]
    # The four unified bottle definitions stay.
    if base in ("liquid_bottle", "solid_jar", "gas_collecting_bottle", "dropper_bottle"):
        return False
    if base.startswith("liquid_") and base.endswith("_bucket"):
        return False  # buckets stay
    if base.startswith("loose_"):
        return False  # loose solids stay
    if base in ("empty_narrow_bottle", "empty_dropper_bottle",
                "empty_gas_collecting_bottle", "empty_brown_dropper_bottle",
                "brown_narrow_mouth_bottle", "brown_wide_mouth_bottle",
                "gas_collecting_bottle_water"):
        return True
    return (base.startswith("liquid_")
            or base.startswith("open_liquid_")
            or base.startswith("solid_")
            or base.startswith("open_solid_")
            or base.startswith("gas_collecting_bottle_")
            or base.startswith("open_gas_collecting_bottle_")
            or base.startswith("dropper_bottle_"))


def main():
    removed = 0
    for fn in sorted(os.listdir(ITEMS)):
        if should_remove(fn):
            os.remove(os.path.join(ITEMS, fn))
            removed += 1
    print(f"removed {removed} old bottle item definitions")


if __name__ == "__main__":
    main()
