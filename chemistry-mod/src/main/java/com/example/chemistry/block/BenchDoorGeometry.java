package com.example.chemistry.block;
public final class BenchDoorGeometry {
 public static float sign(String name){return switch(name){case "left_outer_door" -> 1;case "left_inner_door" -> -1;case "right_inner_door" -> 1;case "right_outer_door" -> -1;case "door_left_outer" -> 1;case "door_left_inner" -> -1;case "door_right_inner" -> 1;case "door_right_outer" -> -1;case "sink_left_door" -> 1;case "sink_right_door" -> -1;case "side_left_door" -> 1;case "side_right_door" -> -1;default -> 1;};}
 private BenchDoorGeometry(){}
}
