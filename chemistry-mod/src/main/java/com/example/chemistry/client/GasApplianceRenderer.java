package com.example.chemistry.client;

import com.example.chemistry.blockentity.GasApplianceBlockEntity;
import com.example.chemistry.data.GasJars;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.*;
import org.joml.Matrix4f;
import java.util.*;

public final class GasApplianceRenderer implements BlockEntityRenderer<GasApplianceBlockEntity, GasApplianceRenderer.State> {
    private static final Map<String, StandaloneModelKey<BlockStateModel>> MODELS = new LinkedHashMap<>();
    static {
        for (String id : List.of("bunsen_burner_body", "bunsen_burner_collar", "bunsen_burner_valve", "bunsen_burner_blue", "bunsen_burner_yellow", "gas_cylinder_small_valve", "gas_cylinder_tall_valve")) key(id);
        for (String size : List.of("small", "tall")) {
            key("gas_cylinder_" + size + "_body");
            for (var gas : GasJars.ALL) key("gas_cylinder_" + gas.id() + "_" + size + "_body");
        }
    }
    private static void key(String name) { MODELS.put(name, new StandaloneModelKey<>(() -> "mchemistry:block/" + name)); }
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        MODELS.forEach((name, key) -> event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(ResourceLocation.fromNamespaceAndPath("mchemistry", "block/" + name))));
    }
    public GasApplianceRenderer(BlockEntityRendererProvider.Context context) { }
    public static final class State extends BlockEntityRenderState { List<CabinetLighting.Vertex> vertices = List.of(); }
    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public void extractRenderState(GasApplianceBlockEntity be, State state, float partial, Vec3 camera,
            @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partial, camera, overlay);
        Matrix4f facing = new Matrix4f().translate(.5f, 0, .5f).rotateY((float) Math.toRadians(be.rotation())).translate(-.5f, 0, -.5f);
        List<CabinetLighting.Vertex> vertices = new ArrayList<>();
        if (be.kind() == 0) {
            add(vertices, be, "bunsen_burner_body", facing);
            Matrix4f collar = new Matrix4f(facing).translate(.5f, .25f, .5f).rotateY(be.blue() ? 0 : (float) Math.PI / 2).translate(-.5f, -.25f, -.5f);
            add(vertices, be, "bunsen_burner_collar", collar);
            Matrix4f valve = new Matrix4f(facing).translate(.5f, .125f, 6.65f / 16).rotateZ(be.valve() ? (float) Math.PI / 2 : 0).translate(-.5f, -.125f, -6.65f / 16);
            add(vertices, be, "bunsen_burner_valve", valve);
            if (be.burning()) add(vertices, be, be.blue() ? "bunsen_burner_blue" : "bunsen_burner_yellow", facing);
        } else {
            String size = be.kind() == 2 ? "tall" : "small";
            String body = "gas_cylinder_" + (be.gas().isEmpty() ? "" : be.gas() + "_") + size + "_body";
            if (!MODELS.containsKey(body)) body = "gas_cylinder_" + size + "_body";
            add(vertices, be, body, facing);
            float y = be.kind() == 2 ? 29.85f / 16 : 7.422f / 16;
            Matrix4f valve = new Matrix4f(facing).translate(.5f, y, .5f).rotateY(be.valve() ? (float) Math.PI / 2 : 0).translate(-.5f, -y, -.5f);
            add(vertices, be, "gas_cylinder_" + size + "_valve", valve);
        }
        state.vertices = List.copyOf(vertices);
    }
    private static void add(List<CabinetLighting.Vertex> out, GasApplianceBlockEntity be, String name, Matrix4f matrix) {
        out.addAll(CabinetLighting.bake(be.getLevel(), be.getBlockPos(), be.getBlockState(), Minecraft.getInstance().getModelManager().getStandaloneModel(MODELS.get(name)), matrix));
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) { CabinetLighting.submit(state.vertices, pose, collector, RenderType.cutout()); }
}
