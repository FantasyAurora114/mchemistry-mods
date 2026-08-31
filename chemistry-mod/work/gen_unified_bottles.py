#!/usr/bin/env python3
"""Generate select-based item models for the four unified reagent bottles.

Each unified item switches its texture by the `custom_model_data` string, which
the Java code sets to a "model key" (e.g. "liquid_water", "open_solid_iron").
The select cases simply point at the already-existing per-substance models, so
no textures are regenerated.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
ITEMS = os.path.join(ROOT, "src", "main", "resources", "assets", "mchemistry", "items")
# The addon MCI ships its ore-solid textures/models under the same namespace,
# so scan both model folders (deduplicated) to cover every substance.
MODEL_DIRS = [
    os.path.join(ROOT, "src", "main", "resources", "assets", "mchemistry", "models", "item"),
    os.path.join(ROOT, "..", "mci", "src", "main", "resources", "assets", "mchemistry", "models", "item"),
]


def model_case(name):
    return {"when": name, "model": {"type": "minecraft:model", "model": "mchemistry:item/" + name}}


def select(cases, fallback):
    return {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:custom_model_data",
            "cases": cases,
            "fallback": {"type": "minecraft:model", "model": "mchemistry:item/" + fallback},
        }
    }


def names_with_prefixes(prefixes, excludes=()):
    out = set()
    for models in MODEL_DIRS:
        if not os.path.isdir(models):
            continue
        for fn in os.listdir(models):
            if not fn.endswith(".json"):
                continue
            base = fn[: -len(".json")]
            if any(base.endswith(e) for e in excludes):
                continue
            if any(base.startswith(p) for p in prefixes):
                out.add(base)
    return sorted(out)


def write(item, prefixes, fallback, excludes=()):
    cases = [model_case(n) for n in names_with_prefixes(prefixes, excludes)]
    path = os.path.join(ITEMS, item + ".json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(select(cases, fallback), f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"{item}: {len(cases)} cases")


def main():
    write("liquid_bottle", ("liquid_", "open_liquid_"), "empty_narrow_bottle", ("_bucket",))
    write("solid_jar", ("solid_", "open_solid_"), "empty_narrow_bottle", ("solid_mixture",))
    write("gas_collecting_bottle",
          ("gas_collecting_bottle_", "open_gas_collecting_bottle_"),
          "empty_gas_collecting_bottle")
    write("dropper_bottle", ("dropper_bottle_",), "empty_dropper_bottle")


if __name__ == "__main__":
    main()
