package com.example.chemistry.client;
import com.example.chemistry.garden.ChemicalGarden;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import java.util.List;
/** Low-poly hollow precipitate tubes, bounded by stored mass and the beaker interior. */
public final class GardenRenderer {
    public static void draw(PoseStack pose,SubmitNodeCollector out,List<ChemicalGarden.Stem> stems,int type,int light,float fill){
        if(type!=5&&type!=8&&type!=9)return;
        double top=type==9?13.65:type==8?10.15:7.45;
        int plant=0;
        for(var stem:stems){
            double x=stem.salt().startsWith("copper")?7.2:9.6,z=8.3;int color=stem.salt().startsWith("copper")?0x489CAF:0xAE763B;
            if(stem.segments()<=0){double size=stem.age()<40?.45:.7;ElectroDeviceRenderer.box(pose,out,light,x,.85,z,size,size*.6,size,color,false);continue;}
            double radius=Math.clamp(Math.sqrt(stem.mass()/stem.segments())*3,.06,.24);
            Vec3 previous=new Vec3(x,.85,z);
            for(int i=1;i<=stem.segments();i++){
                double y=.85+(top-.85)*Math.min(.97,i*.018);
                Vec3 point=new Vec3(x+Math.sin(i*.55+plant)*.5,y,z+Math.sin(i*.37)*.45);
                tube(pose,out,previous,point,radius,color,light);
                if(i%7==0)tube(pose,out,point,point.add(i%2==0?.5:-.5,.25,.35),radius*.6,color,light);
                previous=point;
            }plant++;
        }
    }
    private static void tube(PoseStack pose,SubmitNodeCollector out,Vec3 a,Vec3 b,double radius,int color,int light){
        Vec3 d=b.subtract(a);double length=d.length();if(length<=0)return;pose.pushPose();pose.translate(a.x/16,a.y/16,a.z/16);
        pose.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(d.x,d.z))));pose.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(Math.acos(Math.clamp(d.y/length,-1,1)))));
        double wall=radius*.35;
        for(int i=0;i<4;i++){double x=i==0?-radius:i==1?radius:0,z=i==2?-radius:i==3?radius:0;
            ElectroDeviceRenderer.box(pose,out,light,x,0,z,i<2?wall:radius*2,length,i<2?radius*2:wall,color,false);}
        pose.popPose();
    }
    private GardenRenderer(){}
}
