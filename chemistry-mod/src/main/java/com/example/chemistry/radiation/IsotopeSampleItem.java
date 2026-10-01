package com.example.chemistry.radiation;
import net.minecraft.world.item.Item;
/** Sealed 5g educational isotope sample; nuclide identity is independent from ordinary element stock. */
public final class IsotopeSampleItem extends Item {
 private final String nuclide;
 public IsotopeSampleItem(Properties p,String nuclide){super(p);this.nuclide=nuclide;}
 public String nuclide(){return nuclide;}
}
