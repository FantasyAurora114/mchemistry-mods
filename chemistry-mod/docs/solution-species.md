# Solution species foundation (stage one)

## Ownership and units

`chem_contents` remains the only persistent inventory of material. It stores grams;
`SolutionSpecies.snapshot(stack)` produces an immutable mole-based `SpeciesInventory`
without mutating the stack or persisting a second copy. Entity save/load, network sync,
old item IDs and existing recipe storage therefore continue to use their current paths.

A snapshot normalizes a COPY of legacy aqueous records using the existing migration
rules. Repeated reads are idempotent. Invalid negative or non-finite mass is rejected
before migration. The existing ambiguity of old water-containing, unmarked solution
records is preserved; the view cannot reconstruct unknown historical concentrations.

## What is represented

- All 17 supported nonvolatile aqueous solutes have explicit elemental formulas and
  charge-balanced counterions. Formula units in grams are converted to species moles
  using one consistent atomic-weight table, avoiding inconsistent rounding between
  separate ion masses and legacy formula masses.
- Water is an independent solvent species. CaCO3 in cloudy limewater is suspended
  solid, not dissociated ions. Registered salt residues and their melts keep the
  same elemental formula across phase changes.
- Metal-ion roles and ligand roles are independent chemical metadata. NH3 and SCN−
  have definitions for subsequent coordination work. Their presence in the catalog
  does not create them in a vessel or enable a reaction.
- Explicit ammonia mass can be represented. Legacy ammonia-water bottles and other
  unmapped reagents retain opaque grams; their concentration is NOT guessed here.
- Gas-headspace records stored as mL are outside this gram-based ledger. Gas entries
  that already store grams may be mapped, but the headspace volume is never treated
  as mass. Whole-vessel gas conservation needs a pressure/temperature-aware adapter.

This stage uses complete dissociation of supported salts as a base model; it does
not calculate activities, acid/base equilibria, complex formation or binding.

## Conservation and atomic updates

`Conservation.compare(before, after)` reports element, mass, charge and unmapped
material differences. `fullyModelled=false` explicitly identifies partial elemental
coverage even when all opaque masses were preserved.

`SpeciesInventory.transform(reactants, products, extentMoles)` validates positive
integer stoichiometric coefficients, balance, finite nonnegative amounts and exact
reactant availability before returning a NEW inventory. Invalid operations cannot
partially change their input. It is a calculation API, not a write-back route to an
ItemStack: installing authoritative complex-species storage and its legacy-recipe
adapter belongs to the next stage.

`LabVesselItem.transferLiquids` now checks the combined source+target inventory before
committing either stack. Unknown reagents can still be poured, but their identity and
exact mass must stay unchanged. Existing chemistry recipes are not retroactively
rejected by this new checker; their independent stoichiometry is outside this stage.

## Player view and tests

Sneaking while viewing a placed vessel through chemistry goggles shows up to eight
solution species in mmol. Unknown components are explicitly identified as unparsed.

GameTests cover every mapped stock solution, counterion ratios, grams↔moles,
non-mutating legacy inspection, balanced and rejected transformations, invalid numeric
data, opaque-material preservation, real vessel save/load, clearing, mixture transfer,
and elemental/charge conservation through distillation and crystallization.

## Stage two update: iron–thiocyanate

See `coordination-iron-thiocyanate.md`. `snapshot` now returns reversible 1:1
iron–thiocyanate speciation, while `analyticalSnapshot` exposes the dissociated
component totals. `chem_contents` remains authoritative. Legacy interpretation
is now pure arithmetic rather than mutating a copied ItemStack, making the view
safe to use from tint updates. KSCN and FeCl3 dissolution have been connected.
This supersedes the stage-one statement that no binding is calculated; unrelated
acid/base, coordination and reaction-rate adapters remain future work.
