package com.example.chemistry.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class PlacedVesselEntityRenderState extends EntityRenderState {
    public java.util.List<com.example.chemistry.garden.ChemicalGarden.Stem> garden = java.util.List.of();
    public VesselVisualState vesselVisual = VesselVisualState.EMPTY;

    public int bathType;
    public boolean bathDewar;
    public VesselVisualState bathVisual=VesselVisualState.EMPTY;
    public boolean watchGlass;
    public int vesselType;
    public int color = 0xFFFFFF;
    public boolean vesselSealed;
    public int vesselStoppers;
    public int[] rubberHoles = new int[3];
    public int attached1;
    public int attached2;
    public float mountScale = 1.0F;
    public float mountOffX;
    public float mountOffY;
    public float mountOffZ;
    public float mountYaw;
}
