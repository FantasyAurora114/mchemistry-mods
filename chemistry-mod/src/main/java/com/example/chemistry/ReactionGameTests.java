package com.example.chemistry;

import java.util.List;
import java.util.Map;
import com.example.chemistry.solution.SpeciesInventory;
import com.example.chemistry.solution.SpeciesCatalog;
import com.example.chemistry.solution.SolutionSpecies;
import com.example.chemistry.solution.Conservation;
import java.util.function.Consumer;

import com.example.chemistry.data.Reactions;
import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.entity.DistillationPartEntity;
import com.example.chemistry.entity.PlacedVesselEntity;
import com.example.chemistry.entity.GasCollectingBottleEntity;
import com.example.chemistry.entity.GraduatedCylinderEntity;
import com.example.chemistry.entity.MagneticStirrerEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModEntities;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Small in-game checks for the shared vessel and reaction rules. */
public final class ReactionGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, ChemistryMod.MODID);

    static {
        TEST_FUNCTIONS.register("advanced_reactions", () -> com.example.chemistry.organic.AdvancedGameTests::reactions);
        TEST_FUNCTIONS.register("advanced_wf6", () -> com.example.chemistry.organic.AdvancedGameTests::wf6);
        TEST_FUNCTIONS.register("advanced_isotopes", () -> com.example.chemistry.organic.AdvancedGameTests::isotopes);
        TEST_FUNCTIONS.register("watch_glass_cover", () -> com.example.chemistry.organic.WatchGlassGameTests::cover);
        TEST_FUNCTIONS.register("watch_glass_interaction", () -> com.example.chemistry.organic.WatchGlassGameTests::interaction);
        TEST_FUNCTIONS.register("radio_registry", () -> com.example.chemistry.radiation.RadiationGameTests::registry);
        TEST_FUNCTIONS.register("radio_decay", () -> com.example.chemistry.radiation.RadiationGameTests::decay);
        TEST_FUNCTIONS.register("radio_transfer", () -> com.example.chemistry.radiation.RadiationGameTests::transfer);
        TEST_FUNCTIONS.register("radio_washing", () -> com.example.chemistry.radiation.RadiationGameTests::washing);
        TEST_FUNCTIONS.register("radio_shielding", () -> com.example.chemistry.radiation.RadiationGameTests::shielding);
        TEST_FUNCTIONS.register("radio_armor", () -> com.example.chemistry.radiation.RadiationGameTests::armor);
        TEST_FUNCTIONS.register("radio_contamination", () -> com.example.chemistry.radiation.RadiationGameTests::contamination);
        TEST_FUNCTIONS.register("future_formulas", () -> com.example.chemistry.solution.FutureGameTests::formulas);
        TEST_FUNCTIONS.register("future_reactions", () -> com.example.chemistry.solution.FutureGameTests::reactions);
        TEST_FUNCTIONS.register("future_sugars", () -> com.example.chemistry.solution.FutureGameTests::sugars);
        TEST_FUNCTIONS.register("future_ester", () -> com.example.chemistry.solution.FutureGameTests::ester);
        TEST_FUNCTIONS.register("batch_solubility", () -> com.example.chemistry.solution.BatchGameTests::solubility);
        TEST_FUNCTIONS.register("batch_formulas", () -> com.example.chemistry.solution.BatchGameTests::formulas);
        TEST_FUNCTIONS.register("batch_cobalt", () -> com.example.chemistry.solution.BatchGameTests::cobalt);
        TEST_FUNCTIONS.register("batch_nickel", () -> com.example.chemistry.solution.BatchGameTests::nickel);
        TEST_FUNCTIONS.register("batch_precipitation", () -> com.example.chemistry.solution.BatchGameTests::precipitation);
        TEST_FUNCTIONS.register("batch_amphoteric", () -> com.example.chemistry.solution.BatchGameTests::amphoteric);
        TEST_FUNCTIONS.register("batch_hydrates", () -> com.example.chemistry.solution.BatchGameTests::hydrates);
        TEST_FUNCTIONS.register("batch_crystallization", () -> com.example.chemistry.solution.BatchGameTests::crystallization);
        TEST_FUNCTIONS.register("batch_indicators", () -> com.example.chemistry.solution.BatchGameTests::indicators);
        TEST_FUNCTIONS.register("batch_chromate", () -> com.example.chemistry.solution.BatchGameTests::chromate);
        TEST_FUNCTIONS.register("hotfix_placement", () -> com.example.chemistry.utility.PlacementHotfixGameTests::placement);
        TEST_FUNCTIONS.register("hotfix_ports", () -> com.example.chemistry.utility.PlacementHotfixGameTests::ports);
        TEST_FUNCTIONS.register("hotfix_water_tank", () -> com.example.chemistry.utility.PlacementHotfixGameTests::waterTank);
        TEST_FUNCTIONS.register("hotfix_sink", () -> com.example.chemistry.utility.PlacementHotfixGameTests::sink);
        TEST_FUNCTIONS.register("utility_bath", () -> com.example.chemistry.utility.UtilityGameTests::bath);
        TEST_FUNCTIONS.register("utility_vacuum", () -> com.example.chemistry.utility.UtilityGameTests::vacuum);
        TEST_FUNCTIONS.register("utility_cooler", () -> com.example.chemistry.utility.UtilityGameTests::cooler);
        TEST_FUNCTIONS.register("utility_stand", () -> com.example.chemistry.utility.UtilityGameTests::stand);

        TEST_FUNCTIONS.register("garden_balance", () -> ExpansionGameTests::gardenBalance);
        TEST_FUNCTIONS.register("garden_lifecycle", () -> ExpansionGameTests::gardenLifecycle);
        TEST_FUNCTIONS.register("extraction_balance", () -> ExpansionGameTests::extractionBalance);
        TEST_FUNCTIONS.register("extraction_boundaries", () -> ExpansionGameTests::extractionBoundaries);
        TEST_FUNCTIONS.register("extraction_repeated", () -> ExpansionGameTests::repeatedExtraction);
        TEST_FUNCTIONS.register("organic_esterification", () -> ExpansionGameTests::esterification);
        TEST_FUNCTIONS.register("organic_alkaline_hydrolysis", () -> ExpansionGameTests::alkalineHydrolysis);
        TEST_FUNCTIONS.register("galvanic_balance", () -> ExpansionGameTests::galvanicBalance);
        TEST_FUNCTIONS.register("galvanic_open_meter", () -> ExpansionGameTests::galvanicOpenMeter);
        TEST_FUNCTIONS.register("galvanic_series", () -> ExpansionGameTests::galvanicSeries);
        TEST_FUNCTIONS.register("galvanic_boundaries", () -> ExpansionGameTests::galvanicBoundaries);
        TEST_FUNCTIONS.register("gas_supply", () -> GasEdtaGameTests::supply);
        TEST_FUNCTIONS.register("gas_burner", () -> GasEdtaGameTests::burner);
        TEST_FUNCTIONS.register("gas_lifecycle", () -> GasEdtaGameTests::lifecycle);
        TEST_FUNCTIONS.register("edta_competition", () -> GasEdtaGameTests::competition);
        TEST_FUNCTIONS.register("edta_salts", () -> GasEdtaGameTests::salts);
        TEST_FUNCTIONS.register("bench_storage_tubes", () -> com.example.chemistry.block.BenchGameTests::storageAndTubes);
        TEST_FUNCTIONS.register("bench_tap_water", () -> com.example.chemistry.block.BenchGameTests::tapWater);
        TEST_FUNCTIONS.register("trough_series", () -> com.example.chemistry.electrical.TroughGameTests::longSeries);
        TEST_FUNCTIONS.register("trough_water", () -> com.example.chemistry.electrical.TroughGameTests::bathWater);
        TEST_FUNCTIONS.register("trough_plating", () -> com.example.chemistry.electrical.TroughGameTests::plating);
        TEST_FUNCTIONS.register("trough_chlorine", () -> com.example.chemistry.electrical.TroughGameTests::chlorine);
        TEST_FUNCTIONS.register("trough_shallow", () -> com.example.chemistry.electrical.TroughGameTests::shallow);

        TEST_FUNCTIONS.register("separatory_flow", () -> com.example.chemistry.organic.SeparatoryGameTests::flow);
        TEST_FUNCTIONS.register("separatory_guards", () -> com.example.chemistry.organic.SeparatoryGameTests::guards);
        TEST_FUNCTIONS.register("separatory_lifecycle", () -> com.example.chemistry.organic.SeparatoryGameTests::lifecycle);
        TEST_FUNCTIONS.register("separatory_geometry", () -> com.example.chemistry.organic.SeparatoryGameTests::geometry);

        TEST_FUNCTIONS.register("organic_phases", () -> com.example.chemistry.organic.OrganicGameTests::phases);
        TEST_FUNCTIONS.register("organic_transfer", () -> com.example.chemistry.organic.OrganicGameTests::transfer);
        TEST_FUNCTIONS.register("organic_mixing", () -> com.example.chemistry.organic.OrganicGameTests::mixingPersistence);
        TEST_FUNCTIONS.register("organic_interaction", () -> com.example.chemistry.organic.OrganicGameTests::interaction);

        TEST_FUNCTIONS.register("burette_flow", () -> com.example.chemistry.titration.BuretteGameTests::flow);
        TEST_FUNCTIONS.register("burette_lifecycle", () -> com.example.chemistry.titration.BuretteGameTests::lifecycle);
        TEST_FUNCTIONS.register("burette_variants", () -> com.example.chemistry.titration.BuretteGameTests::variants);
        TEST_FUNCTIONS.register("thermal_calorimetry", () -> ThermalGameTests::calorimetry);
        TEST_FUNCTIONS.register("thermal_mixing", () -> ThermalGameTests::mixing);
        TEST_FUNCTIONS.register("thermal_latent", () -> ThermalGameTests::latent);

        TEST_FUNCTIONS.register("acid_base_solver", () -> AcidBaseGameTests::solver);
        TEST_FUNCTIONS.register("acid_base_vessels", () -> AcidBaseGameTests::vessels);
        TEST_FUNCTIONS.register("acid_base_titration", () -> AcidBaseGameTests::titration);
        TEST_FUNCTIONS.register("acid_base_sampling", () -> AcidBaseGameTests::sampling);
        TEST_FUNCTIONS.register("calcium_precipitation", () -> PrecipitationGameTests::calciumDynamics);
        TEST_FUNCTIONS.register("calcium_filtration", () -> PrecipitationGameTests::calciumFiltration);
        TEST_FUNCTIONS.register("soluble_legacy", () -> PrecipitationGameTests::solubleLegacy);
        TEST_FUNCTIONS.register("precipitation_solver", () -> PrecipitationGameTests::solver);
        TEST_FUNCTIONS.register("precipitation_vessels", () -> PrecipitationGameTests::vesselDynamics);
        TEST_FUNCTIONS.register("precipitation_filtration", () -> PrecipitationGameTests::filtration);
        TEST_FUNCTIONS.register("experiment_feedback", () -> PrecipitationGameTests::feedback);
        TEST_FUNCTIONS.register("visual_gas_placement", () -> LabVisualGameTests::gasPlacement);
        TEST_FUNCTIONS.register("visual_gas_ports", () -> LabVisualGameTests::gasPorts);
        TEST_FUNCTIONS.register("visual_dewar", () -> LabVisualGameTests::dewar);

        TEST_FUNCTIONS.register("bottle_quantity_liquid", () -> BottleQuantityGameTests::liquid);
        TEST_FUNCTIONS.register("bottle_quantity_solid", () -> BottleQuantityGameTests::solid);
        TEST_FUNCTIONS.register("lab_rack_bottles", () -> LabUpgradeGameTests::rackAndBottles);
        TEST_FUNCTIONS.register("lab_reaction_branches", () -> LabUpgradeGameTests::nitricAndChlorine);
        TEST_FUNCTIONS.register("lab_series", () -> LabUpgradeGameTests::series);
        TEST_FUNCTIONS.register("lab_sampling", () -> LabUpgradeGameTests::mixtureAndSampling);
        TEST_FUNCTIONS.register("lab_equilibrium_dynamics", () -> LabUpgradeGameTests::equilibriumDynamics);
        TEST_FUNCTIONS.register("lab_chemistry_hazards", () -> LabUpgradeGameTests::chemistryAndHazards);

        TEST_FUNCTIONS.register("electrical_gas_routing", () -> com.example.chemistry.electrical.ElectricalGameTests::gasRouting);
        TEST_FUNCTIONS.register("filtration_balance", () -> com.example.chemistry.filtration.FiltrationGameTests::balance);
        TEST_FUNCTIONS.register("filtration_lifecycle", () -> com.example.chemistry.filtration.FiltrationGameTests::lifecycle);
        TEST_FUNCTIONS.register("filtration_beakers", () -> com.example.chemistry.filtration.FiltrationGameTests::beakers);
        TEST_FUNCTIONS.register("electrical_vent_accounting", () -> com.example.chemistry.electrical.ElectricalGameTests::ventAccounting);
        TEST_FUNCTIONS.register("electrical_visual_geometry", () -> com.example.chemistry.electrical.ElectricalGameTests::visualGeometry);
        TEST_FUNCTIONS.register("electrical_brine", () -> com.example.chemistry.electrical.ElectricalGameTests::brine);
        TEST_FUNCTIONS.register("electrical_balance", () -> com.example.chemistry.electrical.ElectricalGameTests::balance);
        TEST_FUNCTIONS.register("electrical_wiring", () -> com.example.chemistry.electrical.ElectricalGameTests::wiring);
        TEST_FUNCTIONS.register("electrical_persistence", () -> com.example.chemistry.electrical.ElectricalGameTests::persistence);
        TEST_FUNCTIONS.register("electrical_modelsandhydrides", () -> com.example.chemistry.electrical.ElectricalGameTests::modelsAndHydrides);

        TEST_FUNCTIONS.register("sleeve_head_interaction", () -> ThermometerSleeveGameTests::headInteraction);
        TEST_FUNCTIONS.register("sleeve_sockets", () -> ThermometerSleeveGameTests::sockets);
        TEST_FUNCTIONS.register("sleeve_inventory", () -> ThermometerSleeveGameTests::inventory);
        TEST_FUNCTIONS.register("sleeve_lifecycle", () -> ThermometerSleeveGameTests::lifecycle);

        TEST_FUNCTIONS.register("trough_collision", () -> WorkshopGameTests::troughCollision);
        TEST_FUNCTIONS.register("copper_scraping", () -> WorkshopGameTests::scraping);
        TEST_FUNCTIONS.register("three_neck_geometry", () -> ReactionGameTests::threeNeckGeometry);
        TEST_FUNCTIONS.register("coordination_equilibrium", () -> CoordinationGameTests::equilibrium);
        TEST_FUNCTIONS.register("coordination_shifts", () -> CoordinationGameTests::shifts);
        TEST_FUNCTIONS.register("coordination_transfer", () -> CoordinationGameTests::transfer);

        TEST_FUNCTIONS.register("cabinet_placement", () -> CabinetGameTests::placement);
        TEST_FUNCTIONS.register("cabinet_inventory", () -> CabinetGameTests::inventory);
        TEST_FUNCTIONS.register("cabinet_persistence", () -> CabinetGameTests::persistence);
        TEST_FUNCTIONS.register("cabinet_halves", () -> CabinetGameTests::halves);

        TEST_FUNCTIONS.register("volume_and_headspace", () -> ReactionGameTests::volumeAndHeadspace);
        TEST_FUNCTIONS.register("crystal_plateau", () -> ReactionGameTests::crystalPlateau);
        TEST_FUNCTIONS.register("reaction_pause", () -> ReactionGameTests::reactionPause);
        TEST_FUNCTIONS.register("gas_scales_with_extent", () -> ReactionGameTests::gasScalesWithExtent);
        TEST_FUNCTIONS.register("receiver_capacity", () -> ReactionGameTests::receiverCapacity);
        TEST_FUNCTIONS.register("small_distillation_charge", () -> ReactionGameTests::smallDistillationCharge);
        TEST_FUNCTIONS.register("sodium_hydroxide_distillation", () -> ReactionGameTests::aqueousSolutionDistillation);
        TEST_FUNCTIONS.register("solution_migration_transfer", () -> ReactionGameTests::solutionMigrationAndTransfer);
        TEST_FUNCTIONS.register("species_catalog", () -> ReactionGameTests::speciesCatalog);
        TEST_FUNCTIONS.register("species_conservation", () -> ReactionGameTests::speciesConservation);
        TEST_FUNCTIONS.register("species_persistence_transfer", () -> ReactionGameTests::speciesPersistenceTransfer);
        TEST_FUNCTIONS.register("assembly_returns_attachments", () -> ReactionGameTests::assemblyReturnsAttachments);
        TEST_FUNCTIONS.register("sneak_detaches_distillation_tail", () -> ReactionGameTests::sneakDetachesDistillationTail);
        TEST_FUNCTIONS.register("apparatus_save_reload", () -> ReactionGameTests::apparatusSaveReload);
        TEST_FUNCTIONS.register("pressure_requirement", () -> ReactionGameTests::pressureRequirement);
        TEST_FUNCTIONS.register("temperature_solubility", () -> ReactionGameTests::temperatureSolubility);
        TEST_FUNCTIONS.register("dissolution_heat", () -> ReactionGameTests::dissolutionHeat);
    }

    private static void threeNeckGeometry(GameTestHelper h) {
        h.runAtTickTime(1, () -> {
            ItemStack flask = new ItemStack(ModItems.THREE_NECK_FLASK.get());
            var ports = GlassConnector.ports(flask);
            assertTrue(h, ports.size() == 3 && ports.get(0).y() == 10.4, "central distillation port drift");
            for (double yaw : new double[]{0,90,180,270}) {
                var necks = VesselHeating.neckWorldPositions(BlockPos.ZERO,.6,.2,.5,.1,yaw);
                for (int n=0;n<3;n++) {
                    assertTrue(h,VesselHeating.neckForClick(necks,necks[n])==n,"neck selection drift");
                    VesselHeating.sealNeck(flask,n,1);
                    var box=VesselHeating.attachedHeadBox(BlockPos.ZERO,flask,6,.6,.2,.5,.1,yaw,1,false,new ItemStack(ModItems.STRAIGHT_GLASS_TUBE.get()));
                    double a=Math.toRadians(n==0?35:n==2?-35:0),r=Math.toRadians(yaw);
                    var tip=necks[n].add(-.7*Math.sin(a)*Math.cos(r),.7*Math.cos(a),.7*Math.sin(a)*Math.sin(r));
                    assertTrue(h,box!=null && box.contains(tip),"tilted attachment cannot be selected");
                    VesselHeating.unsealNeck(flask,n);
                }
            }
            LabVesselItem.addMass(flask,"liquid","water",250);
            var model=flask.get(DataComponents.CUSTOM_MODEL_DATA);
            assertTrue(h,model!=null && model.strings().contains("filled_050"),"500 mL flask half-fill item model");
            var b=VesselHeating.vesselBounds(6);
            assertTrue(h,b[0][2]<5.1 && b[1][2]>11.9,"cubic body outside hit box");
            h.succeed();
        });
    }

    private static ItemStack vessel() {
        return new ItemStack(ModItems.ERLENMEYER_FLASK.get());
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, net.minecraft.network.chat.Component.literal(message));
    }

    private static void volumeAndHeadspace(GameTestHelper helper) {
        helper.succeedIf(() -> {
            ItemStack stack = vessel();
            assertTrue(helper, LabVesselItem.addLiquid(stack, "water", 25), "water must fit");
            assertTrue(helper, LabVesselItem.addSolid(stack, "copper"), "solid must fit");
            double occupied = LabVesselItem.usedVolume(stack);
            double free = VesselGasPhase.freeVolumeMl(stack);
            assertTrue(helper, Math.abs(occupied + free - 250.0) < 0.001,
                    "capacity and headspace disagree");
            assertTrue(helper, Math.abs(occupied - 26.0) < 0.001,
                    "5 g of solid should occupy 1 mL");
        });
    }

    private static void temperatureSolubility(GameTestHelper helper) {
        helper.succeedIf(() -> {
            ItemStack stack = vessel();
            LabVesselItem.addLiquid(stack, "water", 100);
            LabVesselItem.addMass(stack, "solid", "potassium_nitrate", 50);
            for (int i = 0; i < 120; i++) {
                TemperatureSystem.setTemp(stack, 80);
                PhaseSystem.dissolveAndCrystallize(stack, false);
            }
            assertTrue(helper, mass(stack, "solid", "potassium_nitrate") < 0.01,
                    "hot water should dissolve all 50 g of potassium nitrate");
            for (int i = 0; i < 120; i++) {
                TemperatureSystem.setTemp(stack, 20);
                PhaseSystem.dissolveAndCrystallize(stack, false);
            }
            double solid = mass(stack, "solid", "potassium_nitrate");
            double dissolved = mass(stack, "liquid", "potassium_nitrate_solution");
            assertTrue(helper, solid > 15 && dissolved < 35,
                    "cooling should crystallize the excess solute");
            assertTrue(helper, Math.abs(solid + dissolved - 50) < 0.01,
                    "dissolution and crystallization must conserve solute mass");
            TemperatureSystem.setTemp(stack, 110);
            PhaseSystem.tick(stack, 110);
            assertTrue(helper, Math.abs(mass(stack, "solid", "potassium_nitrate")
                    + mass(stack, "liquid", "potassium_nitrate_solution") - 50) < 0.01,
                    "boiling must not evaporate dissolved salt");
            ItemStack copper = vessel();
            LabVesselItem.addLiquid(copper, "water", 100);
            LabVesselItem.addMass(copper, "solid", "copper_sulfate_anhydrous", 5);
            PhaseSystem.dissolveAndCrystallize(copper, false);
            assertTrue(helper, mass(copper, "liquid", "copper_sulfate_solution") > 0,
                    "dissolved copper sulfate must use the existing recipe ID");
        });
    }

    private static void dissolutionHeat(GameTestHelper helper) {
        helper.succeedIf(() -> {
            ItemStack hot = vessel();
            LabVesselItem.addLiquid(hot, "water", 100);
            LabVesselItem.addMass(hot, "solid", "sodium_hydroxide", 5);
            PhaseSystem.dissolveAndCrystallize(hot, false);
            assertTrue(helper, TemperatureSystem.getTemp(hot) > 20,
                    "sodium hydroxide dissolution should release heat");
            ItemStack cold = vessel();
            LabVesselItem.addLiquid(cold, "water", 100);
            LabVesselItem.addMass(cold, "solid", "ammonium_nitrate", 5);
            PhaseSystem.dissolveAndCrystallize(cold, false);
            assertTrue(helper, TemperatureSystem.getTemp(cold) < 20,
                    "ammonium nitrate dissolution should absorb heat");
        });
    }

    private static void crystalPlateau(GameTestHelper helper) {
        helper.succeedIf(() -> {
            ItemStack stack = vessel();
            LabVesselItem.addSolid(stack, "copper");
            double initialHeadspace = VesselGasPhase.freeVolumeMl(stack);
            TemperatureSystem.setTemp(stack, 1083.0);
            TemperatureSystem.setTemp(stack, 1100.0);
            assertTrue(helper, Math.abs(TemperatureSystem.getTemp(stack) - 1084.0) < 0.001,
                    "melting must hold the temperature at the melting point");
            assertTrue(helper, mass(stack, "solid", "copper") > 0
                            && mass(stack, "liquid", "molten_copper") > 0,
                    "both phases should coexist during melting");
            assertTrue(helper, Math.abs(VesselGasPhase.freeVolumeMl(stack) - initialHeadspace) < 0.001,
                    "melting must not create extra headspace");
            for (int i = 0; i < 600; i++) {
                TemperatureSystem.setTemp(stack, 1100.0);
            }
            assertTrue(helper, mass(stack, "solid", "copper") < 0.001
                            && TemperatureSystem.getTemp(stack) > 1084.0,
                    "temperature should rise after melting finishes");
            double heatToMeltPoint=ThermalSystem.capacity(stack)*(TemperatureSystem.getTemp(stack)-1084);
            ThermalSystem.addHeat(stack,-heatToMeltPoint-2.5*ThermalSystem.fusionJPerGram("copper"),"test_cooling");
            assertTrue(helper, Math.abs(TemperatureSystem.getTemp(stack) - 1084.0) < 0.001
                            && mass(stack, "solid", "copper") > 0,
                    "freezing must hold the same temperature");
            ItemStack decomposing = vessel();
            LabVesselItem.addSolid(decomposing, "lithium_aluminium_hydride");
            TemperatureSystem.setTemp(decomposing, 200.0);
            assertTrue(helper, mass(decomposing, "liquid", "molten_lithium_aluminium_hydride") == 0,
                    "decomposition must not be treated as melting");
        });
    }

    private static void reactionPause(GameTestHelper helper) {
        helper.succeedIf(() -> {
            ItemStack stack = vessel();
            LabVesselItem.addSolid(stack, "copper");
            LabVesselItem.addLiquid(stack, "sulfuric_acid_concentrated", 25);
            TemperatureSystem.setTemp(stack,300.0);
            assertTrue(helper, ReactionEngine.checkAndStart(stack, null),
                    "heated copper and concentrated acid should start");
            TemperatureSystem.setTemp(stack, 20.0);
            for (int i = 0; i < 20; i++) {
                assertTrue(helper, ReactionEngine.tickResult(stack, null) == null,
                        "a cold reaction should not finish");
            }
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            assertTrue(helper, tag.getCompoundOrEmpty("chem_reaction").getIntOr("progress", -1) == 0,
                    "cooling must pause reaction progress");
            TemperatureSystem.setTemp(stack, 300.0);
            ReactionEngine.Completion completion = null;
            for (int i = 0; i < 100 && completion == null; i++) {
                completion = ReactionEngine.tickResult(stack, null);
            }
            assertTrue(helper, completion != null, "restoring heat should finish the reaction");
        });
    }

    private static void gasScalesWithExtent(GameTestHelper helper) {
        helper.succeedIf(() -> {
            Reactions.Reaction reaction = new Reactions.Reaction(
                    List.of(), List.of(new Reactions.Product("vent", "", 1)),
                    "CaCO₃ → CO₂↑", 1.0, 20, "", 0);
            ItemStack small = vessel();
            ItemStack large = vessel();
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            GasFlowEngine.enqueue(helper.getLevel(), pos, small,
                    new ReactionEngine.Completion(reaction, 0.01));
            GasFlowEngine.enqueue(helper.getLevel(), pos, large,
                    new ReactionEngine.Completion(reaction, 0.02));
            assertTrue(helper, queuedGasMl(small) == 240 && queuedGasMl(large) == 480,
                    "gas volume should scale with actual reacted moles");
        });
    }

    private static void receiverCapacity(GameTestHelper helper) {
        helper.succeedIf(() -> {
            ItemStack receiver = vessel();
            assertTrue(helper, LabVesselItem.addLiquid(receiver, "water", 249),
                    "receiver should accept its initial fill");
            double accepted = LabVesselItem.addLiquidMassUpToCapacity(receiver, "water", 2.0);
            assertTrue(helper, Math.abs(accepted - 1.0) < 0.001,
                    "only the remaining 1 mL should condense");
            assertTrue(helper, LabVesselItem.addLiquidMassUpToCapacity(receiver, "water", 1.0) == 0,
                    "a full receiver must reject further distillate");
        });
    }

    private static void smallDistillationCharge(GameTestHelper helper) {
        helper.succeedIf(() -> {
            ItemStack source = vessel();
            ItemStack receiver = vessel();
            assertTrue(helper, LabVesselItem.addLiquid(source, "water", 5),
                    "source should accept a 5 mL charge");
            TemperatureSystem.setTemp(source, 110);
            for (int i = 0; i < 40; i++) {
                if(mass(source,"liquid","water")>1e-9)ThermalSystem.addHeat(source,.2*ThermalSystem.WATER_VAPORIZATION,"test_heater");
                VesselHeating.tick(source, helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                        receiver, true, true, null);
                double collected = mass(receiver, "liquid", "water");
                double remaining = mass(source, "liquid", "water");
                assertTrue(helper, collected + remaining <= 5.001,
                        "distillation must not create liquid mass");
            }
            assertTrue(helper, LabVesselItem.usedVolume(receiver) < 5.01,
                    "a small charge cannot fill a 250 mL receiver");
            assertTrue(helper, Math.abs(LabVesselItem.usedVolume(receiver) - 5.0) < 0.01,
                    "a connected condenser should collect the charge without venting it first");
        });
    }

    private static void aqueousSolutionDistillation(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            for (String id : com.example.chemistry.data.Solutions.allIds()) {
                ItemStack source = vessel();
                assertTrue(helper, LabVesselItem.addLiquid(source, id, 5), "stock dose must fit: " + id);
                assertTrue(helper, Math.abs(LabVesselItem.usedVolume(source) - 5) < 0.001,
                        "stock volume changed: " + id);
                double water = mass(source, "liquid", "water");
                double solute = mass(source, "liquid", id);
                ItemStack receiver = vessel();
                for (int i = 0; i < 120 && mass(source, "liquid", "water") > 1e-8; i++) {
                    TemperatureSystem.setTemp(source, 110);
                    VesselHeating.tick(source, helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                            receiver, true, true, null);
                }
                PhaseSystem.dissolveAndCrystallize(source, false);
                for (int i = 0; i < 10; i++) LabVesselItem.normalizeSolutions(source);
                assertTrue(helper, Math.abs(mass(receiver, "liquid", "water") - water) < 0.02,
                        "wrong collected water: " + id);
                assertTrue(helper, Math.abs(LabVesselItem.totalMass(source) - solute) < 0.02,
                        "solute mass changed: " + id);
                assertTrue(helper, LabVesselItem.getContents(receiver).stream()
                                .allMatch(e -> e.id().equals("water")), "solute reached receiver: " + id);
                assertTrue(helper, LabVesselItem.getContents(source).stream()
                                .noneMatch(e -> e.id().startsWith("molten_")), "unexpected melt: " + id);
            }
            helper.succeed();
        });
    }

    private static ItemStack legacySolution(String id, double grams) {
        ItemStack stack = vessel();
        CompoundTag entry = new CompoundTag();
        entry.putString("type", "liquid"); entry.putString("id", id); entry.putDouble("amount", grams);
        net.minecraft.nbt.ListTag entries = new net.minecraft.nbt.ListTag(); entries.add(entry);
        CompoundTag tag = new CompoundTag(); tag.put("chem_contents", entries);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static void solutionMigrationAndTransfer(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            for (String id : com.example.chemistry.data.Solutions.SOLUTE.keySet()) {
                double total = 5 * com.example.chemistry.data.ChemicalInfoProvider.densityOfLiquid(id);
                ItemStack legacy = legacySolution(id, total);
                LabVesselItem.normalizeSolutions(legacy);
                double water = mass(legacy, "liquid", "water");
                assertTrue(helper, com.example.chemistry.data.Solutions.isStockReagent(id)
                                ? water > 0 : water == 0,
                        "stock/internal solute solvent interpretation failed: " + id);
                assertTrue(helper, Math.abs(LabVesselItem.totalMass(legacy) - total) < 1e-6,
                        "migration must conserve total mass: " + id);
                LabVesselItem.normalizeSolutions(legacy);
                assertTrue(helper, Math.abs(mass(legacy, "liquid", "water") - water) < 1e-6,
                        "migration ran twice: " + id);
                ItemStack target = vessel();
                assertTrue(helper, LabVesselItem.transferLiquids(legacy, target), "mixture must pour: " + id);
                assertTrue(helper, LabVesselItem.totalMass(legacy) == 0
                                && Math.abs(LabVesselItem.totalMass(target) - total) < 1e-6,
                        "pouring must preserve every component: " + id);
                LabVesselItem.normalizeSolutions(target);
                assertTrue(helper, Math.abs(mass(target, "liquid", "water") - water) < 1e-6,
                        "pouring must not dilute again: " + id);
                ItemStack product = vessel();
                LabVesselItem.addMass(product, "liquid", id, 2);
                LabVesselItem.normalizeSolutions(product);
                assertTrue(helper, mass(product, "liquid", "water") == 0,
                        "reaction solute created water: " + id);
                PhaseSystem.tick(product, 20);
                assertTrue(helper, Math.abs(mass(product, "solid",
                        com.example.chemistry.data.Solutions.soluteOf(id)) - 2) < 1e-6,
                        "dry residue must form without heating: " + id);
            }
            ItemStack source = vessel(); LabVesselItem.addLiquid(source, "potassium_hydroxide_solution", 25);
            ItemStack full = vessel(); LabVesselItem.addLiquid(full, "water", 249);
            double before = LabVesselItem.totalMass(source);
            assertTrue(helper, !LabVesselItem.transferLiquids(source, full)
                            && LabVesselItem.totalMass(source) == before
                            && Math.abs(LabVesselItem.usedVolume(full) - 249) < 1e-6,
                    "failed pour must leave both vessels unchanged");
            ItemStack mixed = vessel();
            LabVesselItem.addLiquid(mixed, "sodium_hydroxide_solution", 25);
            LabVesselItem.addLiquid(mixed, "potassium_hydroxide_solution", 25);
            double mixedWater = mass(mixed, "liquid", "water");
            LabVesselItem.normalizeSolutions(mixed);
            assertTrue(helper, Math.abs(LabVesselItem.usedVolume(mixed) - 50) < 1e-6
                            && Math.abs(mass(mixed, "liquid", "water") - mixedWater) < 1e-6
                            && mass(mixed, "liquid", "sodium_hydroxide_solution") > 0
                            && mass(mixed, "liquid", "potassium_hydroxide_solution") > 0,
                    "mixed solutions must keep independent solute identities and solvent mass");
            ItemStack oldNaoh = legacySolution("sodium_hydroxide_solution", 4);
            CompoundTag tag = oldNaoh.get(DataComponents.CUSTOM_DATA).copyTag();
            tag.putBoolean("chem_naoh_explicit_water", true);
            oldNaoh.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            LabVesselItem.normalizeSolutions(oldNaoh);
            assertTrue(helper, mass(oldNaoh, "liquid", "water") == 0,
                    "previous NaOH migration marker must remain valid");
            helper.succeed();
        });
    }

    private static void speciesCatalog(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            for (String id : com.example.chemistry.data.Solutions.allIds()) {
                ItemStack stack = vessel();
                LabVesselItem.addLiquid(stack, id, 5);
                var inventory = SolutionSpecies.snapshot(stack);
                assertTrue(helper, inventory.fullyModelled(), "unmapped known solution: " + id);
                assertTrue(helper, Math.abs(inventory.massGrams() - LabVesselItem.totalMass(stack)) < 1e-8,
                        "mass-to-moles round trip failed: " + id);
                assertTrue(helper, Math.abs(inventory.chargeMoles()) < 1e-10,
                        "missing counterion: " + id);
                assertTrue(helper, Math.abs(SolutionSpecies.analyticalSnapshot(stack).amount("water") * SpeciesCatalog.get("water").molarMass()
                        - mass(stack, "liquid", "water")) < 1e-8, "analytical water counted twice: " + id);
                assertTrue(helper, com.example.chemistry.solution.Conservation.compare(
                        SolutionSpecies.analyticalSnapshot(stack),inventory).conserved(),
                        "water ionization or hydrolysis lost matter: " + id);
            }
            ItemStack calcium = vessel();
            LabVesselItem.addLiquid(calcium, "limewater_clear", 5);
            var ions = SolutionSpecies.snapshot(calcium);
            assertTrue(helper, Math.abs(ions.amount("hydroxide") - 2 * ions.amount("calcium")) < 1e-12,
                    "Ca(OH)2 must yield two hydroxides per calcium");
            ItemStack copper = vessel();
            LabVesselItem.addLiquid(copper, "copper_sulfate_solution", 5);
            var cu = SolutionSpecies.snapshot(copper);
            assertTrue(helper, cu.amount("copper_ii") > 0
                            && Math.abs(cu.amount("copper_ii") - cu.amount("sulfate")) < 1e-12,
                    "copper sulfate must retain both copper and sulfate");
            helper.succeed();
        });
    }

    private static void speciesConservation(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var original = new SpeciesInventory(Map.of("ammonium", 0.02, "hydroxide", 0.02), Map.of());
            var products = original.transform(Map.of("ammonium", 1, "hydroxide", 1),
                    Map.of("ammonia", 1, "water", 1), 0.01);
            assertTrue(helper, Conservation.compare(original, products).conserved()
                            && Math.abs(products.amount("ammonia") - 0.01) < 1e-12
                            && original.amount("ammonia") == 0,
                    "balanced transform must conserve atoms and leave its input immutable");
            boolean overdraw = false;
            try { original.transform(Map.of("ammonium", 1, "hydroxide", 1),
                    Map.of("ammonia", 1, "water", 1), 0.03); }
            catch (IllegalArgumentException expected) { overdraw = true; }
            assertTrue(helper, overdraw && original.amount("ammonium") == 0.02,
                    "overdraw must reject the whole operation");
            boolean unbalanced = false;
            try { original.transform(Map.of("ammonium", 1), Map.of("ammonia", 1), 0.01); }
            catch (IllegalArgumentException expected) { unbalanced = true; }
            assertTrue(helper, unbalanced, "loss of atoms must be rejected");
            var metal = new SpeciesInventory(Map.of("solid:copper", 1.0), Map.of());
            var ion = new SpeciesInventory(Map.of("copper_ii", 1.0), Map.of());
            assertTrue(helper, !Conservation.compare(metal, ion).conserved()
                            && Conservation.compare(metal, ion).differences().contains("charge"),
                    "equal atoms and mass do not excuse missing charge");
            for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1}) {
                boolean rejected = false;
                try { new SpeciesInventory(Map.of("water", bad), Map.of()); }
                catch (IllegalArgumentException expected) { rejected = true; }
                assertTrue(helper, rejected, "non-finite and negative moles must be rejected");
            }
            helper.succeed();
        });
    }

    private static void speciesPersistenceTransfer(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
                ItemStack broken = legacySolution("potassium_hydroxide_solution", invalid);
                var data = broken.get(DataComponents.CUSTOM_DATA);
                boolean rejected = false;
                try { SolutionSpecies.snapshot(broken); }
                catch (IllegalArgumentException expected) { rejected = true; }
                assertTrue(helper, rejected && data.equals(broken.get(DataComponents.CUSTOM_DATA)),
                        "invalid legacy mass must not be silently converted or erased");
            }
            ItemStack old = legacySolution("potassium_hydroxide_solution", 5.75);
            var savedData = old.get(DataComponents.CUSTOM_DATA);
            var first = SolutionSpecies.snapshot(old);
            var second = SolutionSpecies.snapshot(old);
            assertTrue(helper, first.equals(second) && savedData.equals(old.get(DataComponents.CUSTOM_DATA)),
                    "species inspection must not mutate or repeatedly dilute an old save");
            ItemStack source = vessel();
            LabVesselItem.addLiquid(source, "copper_sulfate_solution", 5);
            LabVesselItem.addMass(source, "liquid", "custom_unknown_reagent", 2.34567);
            ItemStack target = vessel();
            var before = SolutionSpecies.snapshot(source).plus(SolutionSpecies.snapshot(target));
            assertTrue(helper, !before.fullyModelled()
                            && Math.abs(before.unmappedGrams().values().stream()
                                    .mapToDouble(Double::doubleValue).sum() - 2.34567) < 1e-8,
                    "unknown reagents must retain their exact mass and explicit unknown status");
            assertTrue(helper, LabVesselItem.transferLiquids(source, target), "mixture transfer failed");
            var after = SolutionSpecies.snapshot(source).plus(SolutionSpecies.snapshot(target));
            assertTrue(helper, Conservation.compare(before, after).conserved(),
                    "transfer duplicated metal, ligand or solvent");
            PlacedVesselEntity placed = new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(), helper.getLevel());
            PlacedVesselEntity loaded = new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(), helper.getLevel());
            placed.setVessel(target);
            copyThroughSave(helper, placed, loaded);
            assertTrue(helper, SolutionSpecies.snapshot(target).equals(SolutionSpecies.snapshot(loaded.getVessel())),
                    "species must survive the real vessel entity save/load path");
            LabVesselItem.clearContents(target);
            assertTrue(helper, SolutionSpecies.snapshot(target).massGrams() == 0,
                    "clearing contents must not leave ghost species");
            ItemStack hot = vessel();
            LabVesselItem.addLiquid(hot, "sodium_chloride_solution", 5);
            ItemStack receiver = vessel();
            var original = SolutionSpecies.snapshot(hot);
            for (int i = 0; i < 35; i++) {
                TemperatureSystem.setTemp(hot, 110);
                VesselHeating.tick(hot, helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                        receiver, true, true, null);
            }
            var distilled = SolutionSpecies.snapshot(hot).plus(SolutionSpecies.snapshot(receiver));
            assertTrue(helper, Conservation.compare(original, distilled).conserved(),
                    "distillation and crystallization must conserve elemental amounts and charge");
            helper.succeed();
        });
    }

    private static void assemblyReturnsAttachments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            IronStandEntity stand = new IronStandEntity(ModEntities.IRON_STAND.get(), helper.getLevel());
            stand.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            helper.getLevel().addFreshEntity(stand);
            PlacedVesselEntity receiver = new PlacedVesselEntity(
                    ModEntities.PLACED_VESSEL.get(), helper.getLevel());
            receiver.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ());
            receiver.setVessel(vessel());
            receiver.setReceiverStandId(stand.getUUID().toString());
            receiver.setAttached1(new ItemStack(ModItems.STRAIGHT_GLASS_TUBE.get()));
            receiver.setAttached2(new ItemStack(ModItems.THERMOMETER.get()));
            helper.getLevel().addFreshEntity(receiver);
            List<ItemStack> returned = DistillationAssembly.removeAll(stand);
            assertTrue(helper, returned.size() == 3 && receiver.isRemoved(),
                    "dismantling must return the receiver and both attachments");
            stand.discard();
        });
    }

    private static void sneakDetachesDistillationTail(GameTestHelper helper) {
        helper.succeedIf(() -> {
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            IronStandEntity stand = new IronStandEntity(ModEntities.IRON_STAND.get(), helper.getLevel());
            stand.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            helper.getLevel().addFreshEntity(stand);
            DistillationPartEntity head = null;
            for (int kind : new int[] {DistillationPartEntity.HEAD,
                    DistillationPartEntity.CONDENSER, DistillationPartEntity.ADAPTER_BENT,
                    DistillationPartEntity.THERMOMETER}) {
                DistillationPartEntity part = new DistillationPartEntity(
                        ModEntities.DISTILLATION_PART.get(), helper.getLevel());
                part.setKind(kind);
                part.setStand(stand);
                part.setPos(stand.getX(), stand.getY() + 1, stand.getZ());
                helper.getLevel().addFreshEntity(part);
                if (kind == DistillationPartEntity.HEAD) head = part;
            }
            PlacedVesselEntity receiver = new PlacedVesselEntity(
                    ModEntities.PLACED_VESSEL.get(), helper.getLevel());
            receiver.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ());
            ItemStack filled = vessel();
            LabVesselItem.addLiquid(filled, "water", 5);
            receiver.setVessel(filled);
            receiver.setReceiverStandId(stand.getUUID().toString());
            helper.getLevel().addFreshEntity(receiver);
            assertTrue(helper, !DistillationAssembly.canRemove(head),
                    "head should be blocked by the connected tail in ordinary removal");
            List<ItemStack> returned = DistillationAssembly.detachWithDownstream(head);
            assertTrue(helper, returned.size() == 5 && receiver.isRemoved(),
                    "sneak removal should return the receiver and all four mounted parts");
            assertTrue(helper, returned.stream().anyMatch(stack ->
                            stack.is(ModItems.ERLENMEYER_FLASK.get())
                                    && Math.abs(mass(stack, "liquid", "water") - 5) < 0.001),
                    "the receiver must retain its liquid when detached");
            assertTrue(helper, DistillationAssembly.part(stand, DistillationPartEntity.HEAD) == null
                            && DistillationAssembly.adapter(stand) == null,
                    "no disconnected tail parts should remain");
            stand.discard();
        });
    }

    private static void apparatusSaveReload(GameTestHelper helper) {
        helper.succeedIf(() -> {
            GasCollectingBottleEntity gas = new GasCollectingBottleEntity(
                    ModEntities.GAS_COLLECTING_BOTTLE.get(), helper.getLevel());
            gas.setInverted(true);
            gas.setHasPlate(true);
            gas.setGasParts(List.of(new GasCollectingBottleEntity.GasPart("hydrogen", 40)));
            gas.setPurity(0.75);
            GasCollectingBottleEntity loadedGas = new GasCollectingBottleEntity(
                    ModEntities.GAS_COLLECTING_BOTTLE.get(), helper.getLevel());
            copyThroughSave(helper, gas, loadedGas);
            assertTrue(helper, loadedGas.isInverted() && loadedGas.hasPlate()
                            && loadedGas.getFillMl() == 40
                            && Math.abs(loadedGas.getPurity() - 0.75) < 0.001,
                    "gas bottle contents must survive reload");

            GraduatedCylinderEntity cylinder = new GraduatedCylinderEntity(
                    ModEntities.GRADUATED_CYLINDER.get(), helper.getLevel());
            cylinder.setContents("water", 37.0);
            GraduatedCylinderEntity loadedCylinder = new GraduatedCylinderEntity(
                    ModEntities.GRADUATED_CYLINDER.get(), helper.getLevel());
            copyThroughSave(helper, cylinder, loadedCylinder);
            assertTrue(helper, "water".equals(loadedCylinder.getLiquid())
                            && Math.abs(loadedCylinder.getMl() - 37.0) < 0.001,
                    "cylinder contents must survive reload");

            MagneticStirrerEntity stirrer = new MagneticStirrerEntity(
                    ModEntities.MAGNETIC_STIRRER.get(), helper.getLevel());
            stirrer.setFlask(vessel());
            stirrer.setStirBar(true);
            MagneticStirrerEntity loadedStirrer = new MagneticStirrerEntity(
                    ModEntities.MAGNETIC_STIRRER.get(), helper.getLevel());
            copyThroughSave(helper, stirrer, loadedStirrer);
            assertTrue(helper, !loadedStirrer.getFlask().isEmpty() && loadedStirrer.hasStirBar(),
                    "stirrer attachments must survive reload");
        });
    }

    private static void pressureRequirement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            ItemStack stack = vessel();
            LabVesselItem.addMass(stack, "gas", "nitrogen", 28.01);
            LabVesselItem.addMass(stack, "gas", "hydrogen", 6.06);
            LabVesselItem.addSolid(stack, "iron");
            assertTrue(helper, !ReactionEngine.checkAndStart(stack, null),
                    "ammonia synthesis must not start at ambient pressure");
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putBoolean("chem_sealed", true);
            tag.putInt("chem_pressure", 20_000);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            assertTrue(helper, ReactionEngine.checkAndStart(stack, null),
                    "high pressure and catalyst should allow the reaction");
            tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt("chem_pressure", 0);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            ReactionEngine.tickResult(stack, null);
            tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            assertTrue(helper, tag.getCompoundOrEmpty("chem_reaction").getIntOr("progress", -1) == 0,
                    "loss of pressure must pause a pending reaction");
        });
    }

    private static void copyThroughSave(GameTestHelper helper,
            net.minecraft.world.entity.Entity original, net.minecraft.world.entity.Entity restored) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.saveWithoutId(output);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), output.buildResult()));
    }

    private static double mass(ItemStack stack, String type, String id) {
        return LabVesselItem.getContents(stack).stream()
                .filter(e -> e.type().equals(type) && e.id().equals(id))
                .mapToDouble(LabVesselItem.Entry::amount).sum();
    }

    private static int queuedGasMl(ItemStack stack) {
        int total = 0;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (Tag part : tag.getListOrEmpty("chem_gas_pending")) {
            if (part instanceof CompoundTag gas) {
                total += gas.getIntOr("ml", 0);
            }
        }
        return total;
    }

    private ReactionGameTests() {
    }
}
