package com.example.chemistry.client;
import com.example.chemistry.electrical.ElectroDeviceEntity;
import com.example.chemistry.item.LabVesselItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
public class ElectroDeviceRenderer extends EntityRenderer<ElectroDeviceEntity,ElectroDeviceRenderer.State> {
    public static class State extends EntityRenderState {boolean half,resistor,voltmeter,hasElectrode;double electrode;int electrodeColor;VesselVisualState visual;boolean trough,power,on,on2,left,right;float yaw;int color;net.minecraft.world.item.ItemStack[] clips=new net.minecraft.world.item.ItemStack[2],meshes=new net.minecraft.world.item.ItemStack[2],jars=new net.minecraft.world.item.ItemStack[2];double[] jarWater=new double[2];double liquid,leftMl,rightMl,voltage,current,voltage2,current2;}
    public ElectroDeviceRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ElectroDeviceEntity e,State s,float p){super.extractRenderState(e,s,p);s.half=e.isHalfCell();s.resistor=e.isResistor();s.voltmeter=e.isVoltmeter();s.visual=VesselVisualState.of(e.stack());var electrode=com.example.chemistry.electrical.TroughSystem.part(e,"mesh",0);s.hasElectrode=!electrode.isEmpty();s.electrode=com.example.chemistry.electrical.ElectrodePartItem.grams(electrode);s.electrodeColor=com.example.chemistry.electrical.GalvanicSystem.metal(e).equals("copper")?0xBB794D:0xA7AEB4;s.trough=e.isTrough();if(s.trough){for(int i=0;i<2;i++){s.clips[i]=com.example.chemistry.electrical.TroughSystem.part(e,"clip",i);s.meshes[i]=com.example.chemistry.electrical.TroughSystem.part(e,"mesh",i);s.jars[i]=com.example.chemistry.electrical.TroughSystem.part(e,"jar",i);s.jarWater[i]=e.number("jar_water_"+i,0);}s.color=LabVesselItem.contentsColor(e.stack());}
        s.power=e.isPower();s.on=e.channelOn(0);s.on2=e.channelOn(1);s.voltage2=e.channelNumber(1,"voltage",6);s.current2=e.channelCurrent(1);s.yaw=e.getYRot();s.left=e.flag("valve_0");s.right=e.flag("valve_1");s.leftMl=e.visualGasMl(0,p);s.rightMl=e.visualGasMl(1,p);s.liquid=LabVesselItem.usedVolume(e.stack());s.voltage=e.isVoltmeter()?e.number("measured_v",0):e.number("voltage",6);s.current=e.current();}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        if(s.half){
            pose.pushPose();pose.translate(-.5,0,-.5);var model=ModStandaloneModels.vessel(5);
            if(model!=null)out.submitBlockModel(pose,CabinetGlassLayer.TYPE,model,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);
            VesselContentRenderer.draw(pose,out,5,s.visual,s.lightCoords);
            if(s.hasElectrode){box(pose,out,s.lightCoords,8,.8,8,1.5,8.5*Math.clamp(s.electrode/10,.05,1),.25,s.electrodeColor,false);box(pose,out,s.lightCoords,8,9.3,8,1.7,.6,.6,s.electrodeColor,false);}pose.popPose();return;
        }
        if(s.resistor||s.voltmeter){
            pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-s.yaw));pose.scale(.65F,.65F,.65F);pose.translate(-.5,0,-.5);
            var instrument=ElectricalModels.get(s.resistor?"lab_resistor":"lab_voltmeter");
            if(instrument!=null)out.submitBlockModel(pose,RenderType.cutout(),instrument,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);
            if(s.voltmeter)digits(pose,out,s.lightCoords,String.format(java.util.Locale.ROOT,"%.2f",s.voltage),6,2.5);
            pose.popPose();return;
        }
        if(s.trough){TroughRenderer.draw(s,pose,out);return;}
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-s.yaw));pose.scale(ElectroDeviceEntity.MODEL_SCALE,ElectroDeviceEntity.MODEL_SCALE,ElectroDeviceEntity.MODEL_SCALE);pose.translate(-.5,0,-.5);
        if(!s.power&&s.liquid>0){
            int color=0x88BDE0;
            // The first 20 mL fill the common bottom passage, then the collection columns rise.
            double baseline=Math.clamp((s.liquid-20)/100.0,0,1);
            for(int side=0;side<2;side++){
                double h=Math.max(0,17*baseline-17*(side==0?s.leftMl:s.rightMl)/50);
                box(pose,out,s.lightCoords,side==0?3:13,7,8,1.25,h,1.25,color,true);
            }
            double bottomLevel=2.2+4.8*Math.min(1,s.liquid/20);
            double[][] points={{3,7},{3.38,5.24},{4.47,3.75},{6.08,2.75},{8,2.40},
                    {9.92,2.75},{11.53,3.75},{12.62,5.24},{13,7}};
            for(int i=0;i<points.length-1;i++)liquidBend(pose,out,s.lightCoords,points[i],points[i+1],bottomLevel,color);
            double center=s.liquid<20?Math.max(0,bottomLevel-2.4):
                    Math.min(23.4,4.6+17*baseline+(Math.max(0,s.liquid-120)+s.leftMl+s.rightMl)/130*1.8);
            box(pose,out,s.lightCoords,8,2.4,8,.8,center,.8,color,true);
        }
        double[][] geometry=s.power?ElectricalModelGeometry.BENCH_POWER_SUPPLY:ElectricalModelGeometry.HOFMANN_VOLTAMETER;
        String id=s.power?"bench_power_supply":"hofmann_voltameter";
        for(int i=0;i<geometry.length;i++){double[] g=geometry[i];var model=ElectricalModels.get(id+"/part_"+i);if(model==null)continue;pose.pushPose();
            if(g[9]>0){boolean open=g[9]==1?s.left:s.right;if(open){double x=g[9]==1?3:13;pose.translate(x/16,24.56/16,7.6/16);pose.mulPose(Axis.ZP.rotationDegrees(90));pose.translate(-x/16,-24.56/16,-7.6/16);}}
            pose.translate(g[0]/16,g[1]/16,g[2]/16);pose.mulPose(Axis.ZP.rotationDegrees((float)g[8]));pose.mulPose(Axis.YP.rotationDegrees((float)g[7]));pose.mulPose(Axis.XP.rotationDegrees((float)g[6]));pose.translate(g[3]/16,g[4]/16,g[5]/16);
            out.submitBlockModel(pose,s.power?RenderType.cutout():CabinetGlassLayer.TYPE,model,1,1,1,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
        if(s.power){
            box(pose,out,s.lightCoords,12,7.1,15.65,3.5,2.2,.35,0x65727A,false);
            box(pose,out,s.lightCoords,12,7.4,15.85,2.7,1.6,.25,0x1C2026,false);
            for(double x:new double[]{11.4,12.6})box(pose,out,s.lightCoords,x,7.7,16,.25,.7,.20,0xAAA591,false);
            box(pose,out,s.lightCoords,12,8.1,16,.25,.5,.20,0xAAA591,false);
        }
        if(s.power&&s.on){digits(pose,out,s.lightCoords,String.format(java.util.Locale.ROOT,"%.1f",s.voltage),10,7.1);digits(pose,out,s.lightCoords,String.format(java.util.Locale.ROOT,"%.2f",s.current),10,5.9);}
        if(s.power&&s.on2){digits(pose,out,s.lightCoords,String.format(java.util.Locale.ROOT,"%.1f",s.voltage2),2,7.1);digits(pose,out,s.lightCoords,String.format(java.util.Locale.ROOT,"%.2f",s.current2),2,5.9);}
        pose.popPose();
    }
    private static void liquidBend(PoseStack pose,SubmitNodeCollector out,int light,double[] a,double[] b,double top,int color){
        // Short horizontal slices follow the authored curved bore and keep a horizontal liquid surface.
        int steps=24;
        for(int i=0;i<steps;i++){
            double t=(i+.5)/steps,x=a[0]+(b[0]-a[0])*t,y=a[1]+(b[1]-a[1])*t;
            double low=y-.40,high=Math.min(top,y+.40);
            if(high>low)box(pose,out,light,x,low,8,.82,high-low,.80,color,true);
        }
    }
    private static void digits(PoseStack pose,SubmitNodeCollector out,int light,String text,double x,double y){int[] masks={0x3f,0x06,0x5b,0x4f,0x66,0x6d,0x7d,0x07,0x7f,0x6f};for(char c:text.toCharArray()){if(c=='-'){box(pose,out,light,x+.25,y+.45,2.32,.42,.07,.04,0x6CFFA5,false);x+=.65;continue;}if(c=='.'){box(pose,out,light,x,y,2.32,.08,.08,.04,0x6CFFA5,false);x+=.2;continue;}int m=masks[c-'0'];double[][] bars={{.25,.9,.42,.07},{.47,.47,.07,.42},{.47,.04,.07,.42},{.25,0,.42,.07},{.03,.04,.07,.42},{.03,.47,.07,.42},{.25,.45,.42,.07}};for(int i=0;i<7;i++)if((m&(1<<i))!=0)box(pose,out,light,x+bars[i][0],y+bars[i][1],2.32,bars[i][2],bars[i][3],.04,0x6CFFA5,false);x+=.65;}}
    public static void box(PoseStack pose,SubmitNodeCollector out,int light,double x,double y,double z,double width,double height,double depth,int color,boolean translucent){if(height<=0)return;var model=ElectricalModels.get("electrical_unit");if(model==null)return;pose.pushPose();pose.translate(x/16,y/16,z/16);pose.scale((float)width/16,(float)height/16,(float)depth/16);out.submitBlockModel(pose,translucent?CabinetGlassLayer.TYPE:RenderType.cutout(),model,((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,light,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
}
