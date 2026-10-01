package com.example.chemistry.organic;
public final class SeparatoryGeometry {
 public static final double[][] TIERS={{4.8,5.6499999999999995,7.57,8.43,7.57,8.43},{5.6499999999999995,6.499999999999999,7.22,8.78,7.22,8.78},{6.5,7.35,6.87,9.13,6.87,9.13},{7.35,8.2,6.47,9.53,6.47,9.53},{8.2,9.049999999999999,6.12,9.88,6.12,9.88},{9.05,9.9,5.77,10.23,5.77,10.23},{9.899999999999999,10.749999999999998,5.47,10.53,5.47,10.53},{10.75,11.6,5.27,10.73,5.27,10.73},{11.6,12.45,5.57,10.43,5.57,10.43},{12.45,13.299999999999999,6.27,9.73,6.27,9.73}};
 public static final double VOLUME=121.27799999999996;
 public static double height(double fraction){double remaining=VOLUME*Math.clamp(fraction,0,1);for(var t:TIERS){double cross=(t[3]-t[2])*(t[5]-t[4]);double v=(t[1]-t[0])*cross;if(remaining<=v)return t[0]+remaining/cross;remaining-=v;}return TIERS[TIERS.length-1][1];}
 private SeparatoryGeometry(){}
}
