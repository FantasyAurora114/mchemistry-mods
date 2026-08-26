package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class IronStandRenderState extends BlockEntityRenderState {

    public Direction facing = Direction.SOUTH;
    public int rotation;
    public boolean hasTube;
    public boolean hasContents;
    public boolean hasStopper;
    public boolean hasLamp;
    public boolean lampLit;
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
    public boolean hasReceiver;
    public int receiverColor = 0xFFFFFF;
    public boolean vesselSealed;
}
