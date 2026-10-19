package com.example.dishroulette;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/** Original authored profiles extruded into beveled solids, plus turned and swept metal geometry. */
final class SceneMesh {
    final FloatBuffer vertices;
    final int count;
    private SceneMesh(List<Float> data) {
        count=data.size()/8;
        vertices=ByteBuffer.allocateDirect(data.size()*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for(float v:data) vertices.put(v);
        vertices.position(0);
    }
    private static void vertex(List<Float>d,float x,float y,float z,float nx,float ny,float nz,float u,float v) {
        float n=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(n==0)n=1;
        d.add(x);d.add(y);d.add(z);d.add(nx/n);d.add(ny/n);d.add(nz/n);d.add(u);d.add(v);
    }
    private static void face(List<Float>d,float[] p,float nx,float ny,float nz) {
        int[] order={0,1,2,0,2,3};float[] uv={0,1,1,1,1,0,0,0};
        for(int i:order)vertex(d,p[i*3],p[i*3+1],p[i*3+2],nx,ny,nz,uv[i*2],uv[i*2+1]);
    }
    static SceneMesh plane() {
        List<Float>d=new ArrayList<>();face(d,new float[]{-.5f,-.5f,0,.5f,-.5f,0,.5f,.5f,0,-.5f,.5f,0},0,0,1);return new SceneMesh(d);
    }
    static SceneMesh box() {return profile(new float[]{-.5f,-.5f,.5f,-.5f,.5f,.5f,-.5f,.5f},1,.035f);}
    /** Concave profiles are ear-clipped, rather than filled with a fan across the trigger opening. */
    static SceneMesh profile(float[] p,float depth,float bevel) {
        int n=p.length/2;float cx=0,cy=0,area=0;
        for(int i=0;i<n;i++){cx+=p[i*2];cy+=p[i*2+1];int j=(i+1)%n;area+=p[i*2]*p[j*2+1]-p[j*2]*p[i*2+1];}
        cx/=n;cy/=n;float sign=area>=0?1:-1;
        float[] inner=new float[p.length];
        for(int i=0;i<n;i++){inner[2*i]=cx+(p[2*i]-cx)*.955f;inner[2*i+1]=cy+(p[2*i+1]-cy)*.955f;}
        List<Float>d=new ArrayList<>();List<Integer> indices=new ArrayList<>();for(int i=0;i<n;i++)indices.add(i);
        int tries=0;
        while(indices.size()>2 && tries++<n*n){
            boolean found=false;
            for(int k=0;k<indices.size();k++) {
                int a=indices.get((k+indices.size()-1)%indices.size()),b=indices.get(k),c=indices.get((k+1)%indices.size());
                if(cross(inner,a,b,c)*sign<=.000001f)continue;
                boolean contains=false;
                for(int q:indices)if(q!=a&&q!=b&&q!=c&&inTriangle(inner,q,a,b,c,sign)){contains=true;break;}
                if(contains)continue;
                for(int side=-1;side<=1;side+=2)for(int q:new int[]{a,b,c})vertex(d,inner[q*2],inner[q*2+1],side*depth/2,0,0,side,inner[q*2],inner[q*2+1]);
                indices.remove(k);found=true;break;
            }
            if(!found)throw new IllegalArgumentException("Non-simple mesh profile");
        }
        for(int i=0;i<n;i++) {
            int j=(i+1)%n;float x=p[i*2],y=p[i*2+1],xx=p[j*2],yy=p[j*2+1];float nx=(yy-y)*sign,ny=(x-xx)*sign;
            float z=depth/2-bevel;
            face(d,new float[]{x,y,-z,xx,yy,-z,xx,yy,z,x,y,z},nx,ny,0);
            for(int side=-1;side<=1;side+=2)face(d,new float[]{x,y,side*z,xx,yy,side*z,inner[j*2],inner[j*2+1],side*depth/2,inner[i*2],inner[i*2+1],side*depth/2},nx,ny,side*.24f);
        }
        return new SceneMesh(d);
    }
    private static float cross(float[]p,int a,int b,int c){return (p[b*2]-p[a*2])*(p[c*2+1]-p[a*2+1])-(p[b*2+1]-p[a*2+1])*(p[c*2]-p[a*2]);}
    private static boolean inTriangle(float[]p,int q,int a,int b,int c,float s){return cross(p,a,b,q)*s>=0&&cross(p,b,c,q)*s>=0&&cross(p,c,a,q)*s>=0;}
    /** Profile pairs are axial X and radius. Closed profiles support genuinely hollow mouths. */
    static SceneMesh lathe(float[]profile,int slices,boolean cutaway,boolean fluted) {
        List<Float>d=new ArrayList<>();
        for(int k=0;k<profile.length/2-1;k++)for(int i=0;i<slices;i++) {
            double a=2*Math.PI*i/slices,b=2*Math.PI*(i+1)/slices;
            float x=profile[k*2],r=profile[k*2+1],xx=profile[k*2+2],rr=profile[k*2+3];
            if(cutaway && x>-.44f && xx<.44f && Math.sin((a+b)/2)>.24)continue;
            float fa=fluted?1-.045f*(float)Math.pow(.5+.5*Math.cos(a*6),4):1;
            float fb=fluted?1-.045f*(float)Math.pow(.5+.5*Math.cos(b*6),4):1;
            float ay=(float)Math.cos(a),az=(float)Math.sin(a),by=(float)Math.cos(b),bz=(float)Math.sin(b);
            face(d,new float[]{x,r*ay*fa,r*az*fa,xx,rr*ay*fa,rr*az*fa,xx,rr*by*fb,rr*bz*fb,x,r*by*fb,r*bz*fb},r-rr,(xx-x)*(ay+by)/2,(xx-x)*(az+bz)/2);
        }
        return new SceneMesh(d);
    }
    static SceneMesh cylinder() {return lathe(new float[]{-.5f,0,-.5f,.94f,-.46f,1,.46f,1,.5f,.94f,.5f,0},40,false,false);}
    static SceneMesh hollow() {return lathe(new float[]{-.5f,.9f,-.47f,1,.47f,1,.5f,.9f,.5f,.73f,-.5f,.73f,-.5f,.9f},40,false,false);}
    static SceneMesh bullet() {return lathe(new float[]{-.5f,0,-.5f,1,-.18f,.96f,.20f,.68f,.45f,.20f,.5f,0},32,false,false);}
    /** A rounded tube following an XY path, used for a real trigger-guard opening and curved trigger. */
    static SceneMesh tube(float[]path,float radius,boolean closed) {
        List<Float>d=new ArrayList<>();int n=path.length/2;
        for(int k=0;k<(closed?n:n-1);k++) {
            int next=(k+1)%n;
            int prev=closed?(k+n-1)%n:Math.max(0,k-1),after=closed?(next+1)%n:Math.min(n-1,next+1);
            float dx=path[next*2]-path[prev*2],dy=path[next*2+1]-path[prev*2+1];
            float len=(float)Math.sqrt(dx*dx+dy*dy);float px=-dy/len,py=dx/len;
            float ex=path[after*2]-path[k*2],ey=path[after*2+1]-path[k*2+1];
            float el=(float)Math.sqrt(ex*ex+ey*ey);float qx=-ey/el,qy=ex/el;
            for(int j=0;j<10;j++) {
                double a=j*2*Math.PI/10,b=(j+1)*2*Math.PI/10;
                float ca=(float)Math.cos(a)*radius,sa=(float)Math.sin(a)*radius,cb=(float)Math.cos(b)*radius,sb=(float)Math.sin(b)*radius;
                face(d,new float[]{path[k*2]+px*ca,path[k*2+1]+py*ca,sa,path[next*2]+qx*ca,path[next*2+1]+qy*ca,sa,path[next*2]+qx*cb,path[next*2+1]+qy*cb,sb,path[k*2]+px*cb,path[k*2+1]+py*cb,sb},px*(ca+cb),py*(ca+cb),sa+sb);
            }
        }
        return new SceneMesh(d);
    }
}
