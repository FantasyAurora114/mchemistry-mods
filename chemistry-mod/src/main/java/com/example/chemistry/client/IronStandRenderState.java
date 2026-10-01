package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class IronStandRenderState extends BlockEntityRenderState {
    public VesselVisualState vesselVisual = VesselVisualState.EMPTY;

    public Direction facing = Direction.SOUTH;
    public int rotation;
    public boolean dewar;
    public boolean hasTube;
    public boolean hasContents;
    public boolean hasStopper;
    public boolean hasLamp;
    public boolean lampLit;
    public boolean lampBlowtorch;
    public boolean lampCapped;
    public int contentsColor = 0xFFFFFF;
    /** 0 = none, 1 = straight, 2 = right-angle, 3 = dropper. */
    public int attached1;
    public int attached2;
    public int stopperHoles;
    /** Heating attachment: 0 = none, 1 = ring, 2 = asbestos gauze, 3 = clay gauze. */
    public int attachment;
    /** Vessel on the ring: 0 = none, 1 = flask, 2 = erlenmeyer, 3 = crucible, 4 = dish. */
    public int vesselType;
    public int vesselColor = 0xFFFFFF;
    public boolean hasCondenser;
    public boolean hasDistillationHead;
    public boolean headThermometer;
    public boolean hasReceiver;
    public boolean hasReceiverAdapter;
    public boolean receiverAdapterBent;
    public int receiverColor = 0xFFFFFF;
    public boolean vesselSealed;
    /** 三颈瓶玻璃塞位掩码。 */
    public int vesselStoppers;
    /** 三颈瓶每颈的橡胶塞孔数（0 = 无）。 */
    public int[] rubberHoles = new int[3];
}
