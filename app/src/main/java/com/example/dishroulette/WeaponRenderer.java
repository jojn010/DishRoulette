package com.example.dishroulette;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Side-profile weapon study: original beveled solids, turned steel, localized cutaways and two cameras. */
final class WeaponRenderer implements GLSurfaceView.Renderer {
    private static final String VERT =
            "attribute vec3 aPos;attribute vec3 aNormal;attribute vec2 aUv;uniform mat4 uMvp;uniform mat4 uModel;uniform mat4 uNormal;"+
            "varying vec3 n;varying vec3 pos;varying vec2 uv;void main(){pos=(uModel*vec4(aPos,1.)).xyz;n=normalize(mat3(uNormal)*aNormal);uv=aUv;gl_Position=uMvp*vec4(aPos,1.);}";
    private static final String FRAG =
            "precision mediump float;varying vec3 n;varying vec3 pos;varying vec2 uv;uniform vec4 uColor;uniform sampler2D uTex;uniform float uMaterial;uniform float uCutaway;uniform vec3 uEye;"+
            "float hash(vec3 p){return fract(sin(dot(p,vec3(12.98,78.23,37.17)))*43758.5);}"+
            "void main(){if(uCutaway>.5&&pos.z>-.03&&pos.x> -1.02&&pos.x<.55)discard;float m=uMaterial;vec3 N=normalize(n);vec3 L=normalize(vec3(-2.,4.,8.)-pos);vec3 V=normalize(uEye-pos);"+
            "float grain=hash(floor(pos*220.));float scratch=step(.986,fract(pos.x*81.+sin(pos.y*241.)*.2));"+
            "if(m>9.5){gl_FragColor=vec4(.006,.008,.005,uColor.a);return;}"+
            "if(m>8.5){vec4 t=texture2D(uTex,uv);if(t.a<.02)discard;gl_FragColor=t;return;}"+
            "if(m>5.5&&m<6.5){float d=length((uv-.5)*2.);float a=(1.-smoothstep(0.,1.,d))*uColor.a*(.7+.3*hash(vec3(floor(uv*90.),1.)));gl_FragColor=vec4(uColor.rgb,a);return;}"+
            "if(m>6.5&&m<7.5){float a=exp(-dot((uv-.5)*3.,(uv-.5)*3.))*uColor.a;gl_FragColor=vec4(uColor.rgb,a);return;}"+
            "vec3 base=uColor.rgb;float gloss=44.;float spec=.38;"+
            "if(m>.5&&m<1.5){float wood=sin(pos.y*71.+sin(pos.x*21.)*2.+sin(pos.y*11.)*3.);base*=.87+.08*wood+.08*grain;float check=step(.55,fract((pos.x+pos.y)*40.))*step(.55,fract((pos.x-pos.y)*40.));base*=1.-check*.2;gloss=18.;spec=.14;}"+
            "if(m>7.5&&m<8.5){base*=.52+.36*abs(sin(pos.x*105.)*sin(pos.y*105.));gloss=10.;spec=.08;}"+
            "if(m>3.5&&m<4.5){float wood=sin(pos.x*5.+sin(pos.y*14.)*.25);base*=.7+.12*wood+.2*grain;gloss=8.;spec=.025;}"+
            "float pool=exp(-dot(pos.xy-vec2(-.5,.7),pos.xy-vec2(-.5,.7))*.028);float key=max(dot(N,L),0.);float fill=max(dot(N,normalize(vec3(4.,-1.,5.))),0.);float shine=pow(max(dot(N,normalize(L+V)),0.),gloss);"+
            "vec3 c=base*(vec3(.28,.31,.25)+vec3(1.,.83,.58)*key*.95+vec3(.42,.53,.54)*fill*.3)*( .94+grain*.12)+vec3(1.,.86,.65)*shine*spec; c*=.58+.42*pool;"+
            "c+=scratch*key*.025;float rim=pow(1.-abs(dot(N,V)),3.);c+=vec3(.20,.23,.22)*rim*key;"+
            "if(m>4.5&&m<5.5)c=base*1.7; if(m>3.5&&m<4.5)c*=.83;float film=(hash(vec3(floor(gl_FragCoord.xy),1.))-.5)*.022;"+
            "float vignette=1.-.12*pow(length(gl_FragCoord.xy/vec2(600.,1000.)-.5),2.);gl_FragColor=vec4(max(vec3(.0),c*vignette+film),uColor.a);}";
    private int program,aPos,aNormal,aUv,uMvp,uModel,uNormal,uColor,uMaterial,uTex,uEye,uCutaway;
    private SceneMesh box,cylinder,hollow,quad,tip,body,grip,gripPlate,hammer,guard,trigger,cylinderSkin,chamberSkin,rifleStock,shotgunStock,receiver,pot;
    private final float[] projection=new float[16],view=new float[16],vp=new float[16],model=new float[16],mvp=new float[16],normal=new float[16],inverse=new float[16],root=new float[16];
    private final float[] projectPoint=new float[4],localPoint=new float[4],worldPoint=new float[4];
    private final int[] textures=new int[6];
    private final Bitmap labelBitmap=Bitmap.createBitmap(1024,256,Bitmap.Config.ARGB_8888);
    private final Canvas labelCanvas=new Canvas(labelBitmap);
    private final Paint labelPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String[] labels={"","","","","",""};
    private final float[] cursor=new float[6],hitX=new float[6],hitY=new float[6],hitW=new float[6],hitH=new float[6],hitZ=new float[6];
    private int active;
    private final String[] uploadedLabels=new String[6];
    private int uploadedActive=-1;
    private final float[] pickVp=new float[16],pickGun=new float[16],rayA=new float[4],rayB=new float[4],rayTemp=new float[4];
    private boolean cylinderPickEnabled;
    private float pickHalfX=5;
    float inspectYaw,inspectPitch,inspectZoom=1;
    boolean manualCylinder;
    private float shownYaw,shownPitch,shownZoom=1,trackedX=-.65f,trackedY=-.35f,trackedZ;
    private long frameTime;
    private float frameBlend=1;

    private boolean labelsDirty=true,inGun,shadowPass;
    private final float[] gunRoot=new float[16],shadowProjection=new float[16];
    private float angle,loadProgress=-1,aspect=1,halfX=5,halfY=5;
    private String goal="Balanced";
    float shot=-1,transition,focus;
    boolean waiting, discovery, reducedMotion;
    Runnable requestFrame;
    volatile boolean ovenVisible;
    private long origin;
    private float cameraX,cameraY,eyeX,eyeY,eyeZ,desiredTrackX,desiredTrackY;
    private static final float MUZZLE_X=-4.75f,MUZZLE_Y=.88f,POT_X=-7.15f,POT_Y=.88f;

    void setIngredients(String[] values,int selection){labels=values;active=selection;labelsDirty=true;}
    void setGoal(String value){goal=value;angle=0;loadProgress=-1;labelsDirty=true;java.util.Arrays.fill(uploadedLabels,null);}
    void setCylinderAngle(float value,boolean manipulating){angle=value;manualCylinder=manipulating;}
    void load(float t){loadProgress=t;}
    void settleLoad(){if(goal.equals("Balanced"))angle-=60;loadProgress=-1;}
    synchronized int hit(float x,float y){
        int best=-1;float closest=Float.MAX_VALUE;
        for(int i=0;i<6;i++){
            float dx=Math.abs(x-hitX[i]),dy=Math.abs(y-hitY[i]);
            float distance=dy+dx*.1f;
            if(dx<Math.max(hitW[i],.045f)&&dy<Math.max(hitH[i],.014f)&&distance<closest){closest=distance;best=i;}
        }
        return best;
    }
    synchronized float cylinderDegreesPerPixel(int width){return 120f/Math.max(40,.665f/Math.max(.82f,pickHalfX)*width);}
    synchronized boolean pickCylinder(float x,float y){
        if(!cylinderPickEnabled)return false;
        unprojectRay(x,y,-1,rayA);unprojectRay(x,y,1,rayB);
        float ox=rayA[0],oy=rayA[1]-.70f,oz=rayA[2],dx=rayB[0]-ox,dy=rayB[1]-rayA[1],dz=rayB[2]-rayA[2];
        float a=dy*dy+dz*dz,b=2*(oy*dy+oz*dz),c=oy*oy+oz*oz-.70f*.70f,discriminant=b*b-4*a*c;
        if(a>1e-8f&&discriminant>=0){float root=(float)Math.sqrt(discriminant);float t=(-b-root)/(2*a),tt=(-b+root)/(2*a);if(onCylinder(ox,dx,t)||onCylinder(ox,dx,tt))return true;}
        if(Math.abs(dx)>1e-8f)for(int end=0;end<2;end++){float t=((end==0?-1.18f:.73f)-ox)/dx;float yy=oy+t*dy,zz=oz+t*dz;if(t>=0&&t<=1&&yy*yy+zz*zz<=.49f)return true;}
        return false;
    }
    private boolean onCylinder(float ox,float dx,float t){float x=ox+t*dx;return t>=0&&t<=1&&x>=-1.18f&&x<=.73f;}
    private void unprojectRay(float x,float y,float depth,float[] output){
        rayTemp[0]=2*x-1;rayTemp[1]=1-2*y;rayTemp[2]=depth;rayTemp[3]=1;
        Matrix.multiplyMV(output,0,pickVp,0,rayTemp,0);float w=output[3];for(int i=0;i<3;i++)rayTemp[i]=output[i]/w;rayTemp[3]=1;
        Matrix.multiplyMV(output,0,pickGun,0,rayTemp,0);
    }
    @Override public void onSurfaceCreated(GL10 unused,EGLConfig config){
        program=GLES20.glCreateProgram();GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,VERT));GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,FRAG));GLES20.glLinkProgram(program);
        int[] ok=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
        aPos=GLES20.glGetAttribLocation(program,"aPos");aNormal=GLES20.glGetAttribLocation(program,"aNormal");aUv=GLES20.glGetAttribLocation(program,"aUv");uMvp=GLES20.glGetUniformLocation(program,"uMvp");uModel=GLES20.glGetUniformLocation(program,"uModel");uNormal=GLES20.glGetUniformLocation(program,"uNormal");uColor=GLES20.glGetUniformLocation(program,"uColor");uMaterial=GLES20.glGetUniformLocation(program,"uMaterial");uTex=GLES20.glGetUniformLocation(program,"uTex");uEye=GLES20.glGetUniformLocation(program,"uEye");uCutaway=GLES20.glGetUniformLocation(program,"uCutaway");
        buildMeshes();
        GLES20.glGenTextures(6,textures,0);
        for(int texture:textures){GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,labelBitmap,0);}
        labelsDirty=true;java.util.Arrays.fill(uploadedLabels,null);uploadedActive=-1;origin=System.nanoTime();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }
    private void buildMeshes(){
        box=SceneMesh.box();cylinder=SceneMesh.cylinder();hollow=SceneMesh.hollow();quad=SceneMesh.plane();tip=SceneMesh.bullet();
        // One closed, connected frame profile; its opening is occupied by the separate cylinder.
        body=SceneMesh.profile(new float[]{-1.35f,1.35f,-.95f,1.49f,.75f,1.49f,1.18f,1.28f,1.35f,.8f,1.47f,.30f,2.0f,-.12f,2.16f,-.85f,1.45f,-1.05f,1.08f,-.30f,.76f,-.06f,-1.13f,-.06f,-1.43f,.18f,-1.45f,.5f,-1.18f,.5f,-1.12f,.12f,.70f,.12f,.70f,1.24f,-1.35f,1.24f},.56f,.055f);
        grip=SceneMesh.profile(new float[]{1.35f,.28f,1.93f,-.02f,2.22f,-.53f,2.43f,-1.14f,2.74f,-2.30f,2.57f,-2.43f,1.28f,-2.43f,1.22f,-2.2f,1.42f,-1.37f,1.41f,-.91f,1.15f,-.38f},.62f,.08f);
        gripPlate=SceneMesh.profile(new float[]{1.53f,-.18f,1.89f,-.27f,2.12f,-.72f,2.5f,-2.18f,1.48f,-2.18f,1.59f,-1.33f,1.58f,-.82f,1.40f,-.46f},.09f,.025f);
        hammer=SceneMesh.profile(new float[]{.96f,1.12f,1.07f,1.60f,1.39f,1.79f,1.79f,1.88f,1.89f,1.78f,1.62f,1.60f,1.31f,1.16f},.21f,.018f);
        guard=SceneMesh.tube(new float[]{-.65f,.05f,-.72f,-.22f,-.61f,-.65f,-.37f,-.88f,.06f,-1.02f,.53f,-1.04f,.96f,-.86f,1.20f,-.55f,1.18f,-.23f,.98f,.02f},.055f,true);
        trigger=SceneMesh.tube(new float[]{.16f,.14f,.30f,-.05f,.40f,-.32f,.35f,-.55f,.21f,-.73f,.02f,-.80f},.047f,false);
        cylinderSkin=SceneMesh.lathe(new float[]{-.5f,.93f,-.47f,1,-.43f,1,-.30f,1,.30f,1,.43f,1,.47f,1,.5f,.93f},64,false,true);
        chamberSkin=SceneMesh.lathe(new float[]{-.5f,.92f,-.47f,1,-.43f,1,.43f,1,.47f,1,.5f,.92f},32,true,false);
        rifleStock=SceneMesh.profile(new float[]{.85f,.78f,1.65f,.75f,2.22f,.44f,3.88f,.36f,4.08f,.25f,4.13f,-1.16f,3.80f,-1.21f,2.46f,-.58f,1.78f,-.40f,1.40f,-.88f,1.05f,-.72f,1.10f,-.18f,.60f,.02f},.50f,.055f);
        shotgunStock=SceneMesh.profile(new float[]{.86f,.85f,1.62f,.75f,2.18f,.26f,3.86f,-.02f,4.02f,-.15f,4.12f,-1.44f,3.82f,-1.43f,2.30f,-.62f,1.71f,-.33f,1.37f,-.85f,.98f,-.68f,1.1f,-.08f,.65f,.04f},.56f,.06f);
        receiver=SceneMesh.profile(new float[]{-1.1f,.98f,.88f,1.06f,1.30f,.86f,1.32f,.21f,.93f,.01f,-.95f,.04f,-1.18f,.25f},.58f,.045f);
        pot=SceneMesh.lathe(new float[]{-.45f,0,-.45f,.66f,-.39f,.89f,-.12f,1.0f,.30f,1.03f,.39f,1.10f,.47f,1.07f,.47f,.94f,.36f,.94f,-.25f,.78f,-.30f,.0f},48,false,false);
    }
    @Override public void onSurfaceChanged(GL10 unused,int width,int height){GLES20.glViewport(0,0,width,height);aspect=(float)width/height;}
    private void updateLabels(){
        labelPaint.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
        // Compensate for the physical band's aspect ratio so glyphs retain their shape on each case.
        float caseLength=goal.equals("Bulking")?.59f:goal.equals("Cutting")?1.02f:1.48f;
        float caseRadius=goal.equals("Bulking")?.13f:goal.equals("Cutting")?.070f:.116f;
        labelPaint.setTextScaleX(4f*caseRadius*1.82f/(caseLength*.89f));
        for(int i=0;i<6;i++){
            if(labels[i].equals(uploadedLabels[i])&&(i==active)==(i==uploadedActive))continue;
            labelBitmap.eraseColor(Color.TRANSPARENT);
            String text=labels[i].trim();
            // Printed, high-contrast cartridge band. Its full height stays within the case diameter.
            if(!text.isEmpty()){
                labelPaint.setColor(0xaa1c190f);labelCanvas.drawRect(0,4,1024,252,labelPaint);
                labelPaint.setTextSize(172);
                boolean twoLines=labelPaint.measureText(text)>950;
                if(twoLines){
                    labelPaint.setTextSize(104);
                    int split=text.lastIndexOf(' ',text.length()/2+4);
                    if(split<1)split=Math.min(text.length(),Math.max(1,text.length()/2));
                    String first=text.substring(0,split).trim(),second=text.substring(split).trim();
                    drawLabelLine(first,106);drawLabelLine(second,224);cursor[i]=.46f;
                }else{drawLabelLine(text,192);cursor[i]=Math.min(.46f,-.5f+(40+labelPaint.measureText(text))/1024);}
            }else cursor[i]=-.42f;
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[i]);GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D,0,0,0,labelBitmap);
            uploadedLabels[i]=labels[i];
        }
        uploadedActive=active;labelsDirty=false;
    }
    private void drawLabelLine(String text,float baseline){
        // Keep a legible font floor; very long lines elide rather than becoming microscopic.
        float size=labelPaint.getTextSize(),width=labelPaint.measureText(text);
        if(width>950)labelPaint.setTextSize(Math.max(82,size*950/width));
        String fitted=android.text.TextUtils.ellipsize(text,new android.text.TextPaint(labelPaint),950,android.text.TextUtils.TruncateAt.END).toString();
        labelPaint.setColor(0xfffff1ce);labelPaint.setShadowLayer(3,0,2,0xff080906);
        labelCanvas.drawText(fitted,32,baseline,labelPaint);labelPaint.setTextSize(size);
    }
    private void ammunitionPosition(int index,float[] point){
        if(goal.equals("Balanced")){
            double theta=Math.toRadians(20+index*60+angle-(loadProgress<0?0:60*mechanical(loadProgress)));
            point[0]=-.18f;point[1]=.70f+(float)Math.sin(theta)*.405f;point[2]=(float)Math.cos(theta)*.405f-.1f;
        }else if(goal.equals("Bulking")){point[0]=.45f-index*.69f;point[1]=.42f;point[2]=.14f;}
        else{point[0]=-.12f;point[1]=.76f-index*.155f;point[2]=.24f;}
    }
    private void camera(){
        shownYaw+=(inspectYaw-shownYaw)*frameBlend;shownPitch+=(inspectPitch-shownPitch)*frameBlend;shownZoom+=(inspectZoom-shownZoom)*frameBlend;
        boolean cooking=shot>=0||waiting;
        float staged=cooking?(shot<0?1:smooth(Math.min(1,shot/.25f))):0; if(reducedMotion)staged=cooking?1:0;
        float normal=goal.equals("Balanced")?4.65f:5.4f;
        float zoom=(normal+(1.18f-normal)*focus)*shownZoom;
        halfX=Math.max(.82f,Math.min(9,zoom))+(6.75f-Math.max(.82f,Math.min(9,zoom)))*staged;
        halfX=Math.max(halfX,(.68f+staged*2.12f)*aspect);halfY=halfX/aspect;
        ammunitionPosition(active,localPoint);
        float desiredX=-.65f+(localPoint[0]+.65f)*focus;
        float desiredY=.10f+((manualCylinder?.70f:localPoint[1])-.10f)*focus;
        desiredTrackX=desiredX;desiredTrackY=desiredY;
        float desiredZ=localPoint[2]*focus;
        trackedX+=(desiredX-trackedX)*frameBlend;trackedY+=(desiredY-trackedY)*frameBlend;trackedZ+=(desiredZ-trackedZ)*frameBlend;
        cameraX=trackedX*(1-staged)-2.55f*staged;cameraY=trackedY*(1-staged)+.05f*staged;
        double yaw=Math.toRadians(shownYaw-12),pitch=Math.toRadians(shownPitch+14);
        float inspectionX=(float)(14*Math.sin(yaw)*Math.cos(pitch)),inspectionY=(float)(14*Math.sin(pitch)),inspectionZ=(float)(14*Math.cos(yaw)*Math.cos(pitch));
        eyeX=cameraX+inspectionX*(1-staged)-4.7f*staged;eyeY=cameraY+inspectionY*(1-staged)-6.5f*staged;eyeZ=trackedZ*(1-staged)+inspectionZ*(1-staged)+9*staged;
        Matrix.setLookAtM(view,0,eyeX,eyeY,eyeZ,cameraX,cameraY,trackedZ*(1-staged),0,1,0);
        Matrix.orthoM(projection,0,-halfX,halfX,-halfY,halfY,.3f,45);Matrix.multiplyMM(vp,0,projection,0,view,0);
        GLES20.glUniform3f(uEye,eyeX,eyeY,eyeZ);
    }
    @Override public void onDrawFrame(GL10 unused){
        GLES20.glClearColor(.015f,.017f,.014f,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program);
        long now=System.nanoTime();float elapsed=frameTime==0?.016f:Math.min(.05f,(now-frameTime)/1e9f);frameTime=now;frameBlend=1-(float)Math.exp(-elapsed*16);
        if(labelsDirty)updateLabels();camera();inGun=false;Matrix.setIdentityM(root,0);
        environment();
        float recoil=!reducedMotion&&shot>.29f&&shot<.43f?(float)Math.sin((shot-.29f)/.14f*Math.PI):0;
        Matrix.setIdentityM(gunRoot,0);Matrix.translateM(gunRoot,0,recoil*.22f,-transition*.15f,-.10f+recoil*.06f);Matrix.rotateM(gunRoot,0,recoil*3,0,1,0);
        synchronized(this){
            Matrix.invertM(pickVp,0,vp,0);Matrix.invertM(pickGun,0,gunRoot,0);
            cylinderPickEnabled=goal.equals("Balanced")&&shot<0&&!waiting;pickHalfX=halfX;
        }
        // Project the actual meshes onto the tabletop from the same overhead key light.
        float plane=.786f,dot=8+plane;
        for(int column=0;column<4;column++)for(int row=0;row<4;row++){
            float light=row==0?-2:row==1?4:row==2?8:1;
            float coefficient=column==2?1:column==3?plane:0;
            shadowProjection[column*4+row]=(column==row?dot:0)-light*coefficient;
        }
        Matrix.multiplyMM(root,0,shadowProjection,0,gunRoot,0);
        inGun=true;shadowPass=true;GLES20.glDepthMask(false);
        if(goal.equals("Balanced"))revolver();else longGun(goal.equals("Bulking"));
        GLES20.glDepthMask(true);shadowPass=false;System.arraycopy(gunRoot,0,root,0,16);
        if(goal.equals("Balanced"))revolver();else longGun(goal.equals("Bulking"));
        inGun=false;
        // The pot is not submitted to the renderer at all during ingredient entry.
        ovenVisible = (shot>=.16f)||waiting&&shot<0;
        if(ovenVisible){oven();if(shot>=0)effects();else steam(reducedMotion?.3f:((System.nanoTime()-origin)/1e9f)%2/2,.44f);}
        boolean moving=Math.abs(inspectYaw-shownYaw)+Math.abs(inspectPitch-shownPitch)+Math.abs(inspectZoom-shownZoom)+Math.abs(trackedX-desiredTrackX)+Math.abs(trackedY-desiredTrackY)>.006f;
        if(requestFrame!=null&&(moving||waiting&&!reducedMotion))requestFrame.run();
    }
    private void environment(){
        // An original olive butcher's workbench, inlaid steel edges and warm light pools.
        mesh(box,-1,0,-.98f,35,30,.24f,0,0,0,.14f,.155f,.105f,4,1);
        for(int i=-6;i<=6;i++)mesh(box,i*1.8f,0,-.85f,.012f,30,.018f,0,0,0,.037f,.048f,.03f,4,1);
        mesh(box,0,4.1f,-.76f,25,.20f,.16f,0,0,0,.15f,.16f,.13f,0,1);
        mesh(box,0,4.27f,-.75f,25,.045f,.20f,0,0,0,.31f,.29f,.21f,0,1);
        // Set dressing lives above the loadout, never over ingredient input.
        for(int i=0;i<4;i++){
            mesh(cylinder,2.9f+i*.31f,3.35f,-.56f,.22f,.10f,.10f,0,90,0,.30f,.26f,.15f,2,1);
            mesh(cylinder,2.9f+i*.31f,3.35f,-.40f,.12f,.085f,.085f,0,90,0,.095f,.12f,.08f,3,1);
        }
        mesh(box,-3.1f,3.05f,-.78f,2.10f,.74f,.04f,0,0,-6,.30f,.26f,.16f,4,1);
        for(int i=0;i<5;i++)mesh(box,-3.78f+i*.31f,3.03f,-.746f,.15f,.45f,.009f,0,0,-6,.11f,.13f,.09f,4,1);
        for(int i=0;i<48;i++)mesh(box,(i*43%135)/10f-7,(i*29%119)/10f-6,-.848f,.03f+i%4*.045f,.007f,.006f,0,0,i%3*21,.20f,.21f,.14f,4,.5f);
        GLES20.glDepthMask(false);
        mesh(quad,-.4f,-.15f,-.80f,8.7f,4.6f,1,0,0,0,0,0,0,7,.38f);
        GLES20.glDepthMask(true);
    }

    private void revolver(){
        // Cylinder 1.85 units / full gun 7.7 units = 24%, following the supplied side-profile reference.
        part(body,0,0,0,1,1,1,0,0,0,.075f,.09f,.10f,0,1);
        part(grip,0,0,-.02f,1,1,1,0,0,0,.26f,.125f,.055f,1,1);
        part(gripPlate,0,0,.345f,1,1,1,0,0,0,.16f,.075f,.032f,8,1);
        // Turned barrel and ejector shroud join the cylinder housing at X=-1.15.
        part(cylinder,-2.94f,.88f,0,3.56f,.255f,.255f,0,0,0,.14f,.16f,.17f,0,1);
        part(cylinder,-2.90f,.49f,-.015f,3.46f,.235f,.235f,0,0,0,.10f,.12f,.13f,0,1);
        part(hollow,MUZZLE_X,.88f,0,.11f,.264f,.264f,0,0,0,.19f,.20f,.20f,0,1);
        part(box,-3.00f,1.15f,-.03f,3.46f,.12f,.28f,0,0,0,.12f,.14f,.15f,0,1);
        part(box,-4.35f,1.28f,0,.28f,.20f,.105f,0,0,-8,.13f,.14f,.15f,0,1);
        part(box,.45f,1.57f,0,.40f,.10f,.24f,0,0,0,.19f,.20f,.19f,0,1);
        part(hammer,0,0,-.02f,1,1,1,0,0,0,.23f,.25f,.25f,0,1);
        part(guard,0,0,.02f,1,1,1,0,0,0,.20f,.22f,.22f,0,1);
        part(trigger,0,0,.04f,1,1,1,0,0,0,.29f,.30f,.28f,0,1);
        // Six physically scaled chambers around the barrel's X axis, not a vertical rack.
        float rotation=angle-(loadProgress<0?0:60*mechanical(loadProgress));
        part(cylinderSkin,-.23f,.70f,0,1.85f,.665f,.665f,rotation,0,0,.23f,.25f,.25f,0,1);
        part(hollow,-1.13f,.70f,0,.08f,.655f,.655f,0,0,0,.23f,.25f,.24f,0,1);
        part(hollow,.68f,.70f,0,.10f,.655f,.655f,0,0,0,.25f,.26f,.25f,0,1);
        part(cylinder,-.25f,.70f,0,2.04f,.075f,.075f,0,0,0,.22f,.24f,.23f,0,1);
        for(int i=0;i<6;i++){
            double theta=Math.toRadians(20+i*60+rotation);float y=.70f+(float)Math.sin(theta)*.405f,z=(float)Math.cos(theta)*.405f;
            part(chamberSkin,-.23f,y,z,1.72f,.144f,.144f,0,0,0,.085f,.10f,.105f,0,1);
            ammunition(i,-.18f,y,z,1.48f,.116f,false);
        }
        // The selected case remains exposed through the localized cutaway, including a rear chamber.
        double selectedTheta=Math.toRadians(20+active*60+rotation);
        if(!shadowPass&&focus>.05f)GLES20.glDepthRangef(0,.90f);
        ammunition(active,-.18f,.70f+(float)Math.sin(selectedTheta)*.405f,(float)Math.cos(selectedTheta)*.405f,1.48f,.116f,false);
        GLES20.glDepthRangef(0,1);
        // Barrel collars, rear frame plate and individual grip fasteners.
        part(hollow,-1.25f,.88f,0,.10f,.28f,.28f,0,0,0,.25f,.27f,.25f,0,1);
        part(hollow,-4.12f,.88f,0,.06f,.265f,.265f,0,0,0,.21f,.23f,.22f,0,1);
        part(box,.87f,.70f,-.33f,.16f,1.30f,.07f,0,0,0,.16f,.18f,.18f,0,1);
        part(gripPlate,0,0,-.36f,1,1,1,0,0,0,.18f,.084f,.034f,8,1);
        // Small screws, checkered grip inset and cylinder latch finish the silhouette.
        screw(1.35f,.62f,.313f,.09f);screw(1.89f,-1.40f,.415f,.085f);screw(.96f,.10f,.315f,.055f);
        part(box,1.13f,.82f,.32f,.32f,.16f,.07f,0,0,15,.18f,.20f,.20f,0,1);
        for(int i=0;i<5;i++)part(box,1.58f+i*.043f,1.76f,.12f,.023f,.11f,.03f,0,0,-35,.08f,.09f,.09f,0,1);
    }
    private void longGun(boolean shotgun){
        float movement=loadProgress<0?0:(float)Math.sin(loadProgress*Math.PI);
        part(shotgun?shotgunStock:rifleStock,0,0,0,1,1,1,0,0,0,.23f,.105f,.043f,1,1);
        part(receiver,0,0,0,1,1,1,0,0,0,.13f,.155f,.16f,0,1);
        part(cylinder,-2.94f,.88f,0,3.88f,shotgun?.18f:.115f,shotgun?.18f:.115f,0,0,0,.19f,.22f,.22f,0,1);
        part(hollow,-4.88f,.88f,0,.13f,shotgun?.19f:.13f,shotgun?.19f:.13f,0,0,0,.25f,.26f,.24f,0,1);
        part(guard,.75f,-.18f,0,.65f,.62f,1,0,0,0,.20f,.22f,.21f,0,1);
        part(trigger,.80f,-.11f,0,.62f,.63f,1,0,0,0,.30f,.30f,.28f,0,1);
        if(shotgun){
            part(cylinder,-2.15f,.42f,-.04f,4.38f,.158f,.158f,0,0,0,.16f,.18f,.18f,0,1);
            part(cylinder,-2.85f+movement*.36f,.46f,-.02f,1.14f,.29f,.29f,0,0,0,.22f,.10f,.038f,1,1);
            for(int i=0;i<9;i++)part(hollow,-3.30f+i*.11f+movement*.36f,.46f,-.02f,.025f,.297f,.297f,0,0,0,.085f,.055f,.03f,1,1);
            part(box,-4.30f,1.09f,0,.15f,.16f,.07f,0,0,0,.16f,.18f,.17f,0,1);
            for(int i=0;i<6;i++)ammunition(i,.45f-i*.69f-(i==active?movement*.18f:0),.42f,.24f+(i==active?movement*-.13f:0),.59f,.13f,true);
        }else{
            // Scope mounts, optical bells, magazine, and a separate operating bolt.
            part(box,-.25f,1.17f,0,.25f,.20f,.21f,0,0,0,.19f,.21f,.20f,0,1);
            part(box,.95f,1.17f,0,.25f,.20f,.21f,0,0,0,.19f,.21f,.20f,0,1);
            part(cylinder,.20f,1.46f,0,2.20f,.16f,.16f,0,0,0,.10f,.13f,.14f,0,1);
            part(cylinder,-.96f,1.46f,0,.46f,.25f,.25f,0,0,0,.14f,.17f,.17f,0,1);
            part(cylinder,1.35f,1.46f,0,.32f,.22f,.22f,0,0,0,.13f,.16f,.16f,0,1);
            part(cylinder,.72f+movement*.40f,.83f,.32f,.9f,.095f,.095f,0,0,0,.40f,.43f,.42f,0,1);
            part(cylinder,1.02f+movement*.40f,.64f,.43f,.40f,.055f,.055f,0,0,65,.35f,.38f,.37f,0,1);
            part(cylinder,1.12f+movement*.40f,.47f,.43f,.16f,.12f,.12f,0,0,0,.15f,.17f,.16f,0,1);
            part(box,-.12f,-.22f,-.12f,.91f,.65f,.36f,0,0,0,.15f,.18f,.17f,0,1);
            for(int i=0;i<6;i++)ammunition(i,-.12f+(i==active?movement*.30f:0),.76f-i*.155f,.34f,1.02f,.070f,false);
        }
        part(box,3.99f,-.43f,0,.12f,shotgun?1.8f:1.5f,.60f,0,0,-3,.035f,.04f,.035f,8,1);
        part(box,-1.13f,.98f,0,.35f,.12f,.54f,0,0,0,.22f,.24f,.22f,0,1);
        for(int i=0;i<4;i++)part(box,.10f+i*.16f,.20f,.301f,.012f,.37f,.025f,0,0,0,.07f,.08f,.07f,0,1);
        screw(.92f,.37f,.32f,.065f);screw(2.81f,-.17f,.29f,.07f);
    }
    private void ammunition(int i,float x,float y,float z,float length,float radius,boolean shell){
        boolean filled=discovery||!labels[i].trim().isEmpty();boolean selected=i==active;
        if(filled||selected){
            float a=filled?1:.32f;
            part(cylinder,x,y,z,length,radius,radius,0,0,0,shell?.37f:.63f,shell?.055f:.41f,shell?.03f:.15f,shell?0:2,a);
            part(cylinder,x+length/2-.045f,y,z,.09f,radius*1.10f,radius*1.10f,0,0,0,.62f,.43f,.18f,2,a);
            if(!shell)part(tip,x-length/2-.10f,y,z,.25f,radius*.92f,radius*.92f,0,180,0,.42f,.23f,.11f,2,a);
        }
        if(shadowPass)return;
        if(shot>=0||waiting||discovery)return;
        localPoint[0]=x;localPoint[1]=y;localPoint[2]=z;localPoint[3]=1;
        Matrix.multiplyMV(worldPoint,0,root,0,localPoint,0);
        float wx=worldPoint[0],wy=worldPoint[1],wz=worldPoint[2];
        float dy=eyeY-wy,dz=eyeZ-wz,radial=(float)Math.sqrt(dy*dy+dz*dz);
        if(radial>0){wy+=dy/radial*(radius+.008f);wz+=dz/radial*(radius+.008f);}
        float bandWidth=length*.89f,bandHeight=radius*1.82f;
        localPoint[0]=wx;localPoint[1]=wy;localPoint[2]=wz;localPoint[3]=1;
        Matrix.multiplyMV(projectPoint,0,vp,0,localPoint,0);float w=projectPoint[3];
        synchronized(this){hitX[i]=.5f+projectPoint[0]/w*.5f;hitY[i]=.5f-projectPoint[1]/w*.5f;hitZ[i]=projectPoint[2]/w;hitW[i]=bandWidth/(4*halfX);hitH[i]=bandHeight/(4*halfY);}
        if((filled||selected)&&focus>.07f){
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            billboard(wx,wy,wz,bandWidth,bandHeight,textures[i]);
            if(selected&&loadProgress<0){
                float cx=wx+view[0]*bandWidth*cursor[i],cy=wy+view[4]*bandWidth*cursor[i],cz=wz+view[8]*bandWidth*cursor[i];
                // A cursor is drawn into the same local billboard basis as its cartridge band.
                billboardCursor(cx,cy,cz,.012f,bandHeight*.66f);
            }
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
    }
    private void billboardBasis(float x,float y,float z,float width,float height){
        Matrix.setIdentityM(model,0);
        model[0]=view[0]*width;model[1]=view[4]*width;model[2]=view[8]*width;
        model[4]=view[1]*height;model[5]=view[5]*height;model[6]=view[9]*height;
        model[8]=view[2];model[9]=view[6];model[10]=view[10];model[12]=x;model[13]=y;model[14]=z;
    }
    private void billboard(float x,float y,float z,float width,float height,int texture){billboardBasis(x,y,z,width,height);drawPrepared(quad,1,1,1,9,1,texture);}
    private void billboardCursor(float x,float y,float z,float width,float height){billboardBasis(x,y,z,width,height);drawPrepared(quad,.95f,.84f,.58f,5,1,0);}
    private void screw(float x,float y,float z,float radius){
        part(cylinder,x,y,z,.04f,radius,radius,0,90,0,.32f,.34f,.31f,0,1);
        part(box,x,y,z+.022f,radius*1.35f,.018f,.012f,0,0,30,.04f,.05f,.045f,0,1);
    }
    private void oven(){
        // It stands on the same tabletop directly ahead of the -X-facing muzzle. Its mouth is +Z.
        GLES20.glDepthMask(false);mesh(quad,POT_X,POT_Y,-.73f,3.5f,2.9f,1,0,0,0,0,0,0,7,.7f);GLES20.glDepthMask(true);
        mesh(pot,POT_X,POT_Y,-.32f,1,1,1,0,-90,0,.16f,.18f,.17f,3,1);
        mesh(hollow,POT_X,POT_Y,.12f,.075f,1.085f,1.085f,0,-90,0,.27f,.28f,.25f,0,1);
        mesh(cylinder,POT_X,POT_Y,-.59f,.025f,.76f,.76f,0,-90,0,.018f,.025f,.018f,3,1);
        mesh(hollow,POT_X,POT_Y-1.19f,-.10f,.12f,.27f,.39f,0,-90,0,.24f,.25f,.22f,0,1);
        mesh(hollow,POT_X,POT_Y+1.19f,-.10f,.12f,.27f,.39f,0,-90,0,.24f,.25f,.22f,0,1);
        mesh(cylinder,POT_X-.18f,POT_Y-2.05f,-.735f,.13f,1.02f,1.02f,0,-90,0,.20f,.22f,.19f,3,1);
        mesh(cylinder,POT_X-.18f,POT_Y-2.05f,-.59f,.16f,.19f,.19f,0,-90,0,.17f,.19f,.16f,3,1);
    }
    private void effects(){
        float muzzle=goal.equals("Balanced")?MUZZLE_X:-4.88f;
        if(!reducedMotion&&shot>=.29f&&shot<.36f){
            mesh(tip,muzzle-.28f,MUZZLE_Y,0,.65f,.23f,.23f,0,180,0,1,.56f,.13f,5,.9f);
            mesh(cylinder,muzzle-.12f,MUZZLE_Y,.01f,.30f,.11f,.11f,0,0,0,1,.91f,.52f,5,1);
        }
        if(!reducedMotion&&shot>.30f&&shot<.76f)for(int i=0;i<6;i++){
            float t=clamp((shot-.30f-i*.016f)/.30f);if(t<=0||t>=1)continue;
            float x=muzzle+(POT_X-muzzle)*t;
            float z=.03f+1.7f*t*(1-t)-.06f*t;
            mesh(cylinder,x,MUZZLE_Y+(float)Math.sin(i)*.025f,z,.10f,.045f,.045f,0,i*41+t*400,0,.68f,.43f,.16f,2,1);
        }
        if(shot>.36f)steam((shot-.36f)/.64f,.72f);
    }
    private void steam(float t,float opacity){
        GLES20.glDepthMask(false);
        for(int i=0;i<20;i++){
            float phase=(t+i*.043f)%1;
            mesh(quad,POT_X+(float)Math.sin(i*9+phase*3)*phase*.6f,POT_Y+(float)Math.cos(i*4)*phase*.4f,.16f+phase*2.2f,.35f+phase*1.6f,.35f+phase*1.6f,1,35,-27,i*37,.42f,.43f,.37f,6,opacity*(1-phase)*.8f);
        }
        GLES20.glDepthMask(true);
    }
    private void project(float x,float y,float z,float[] transform){localPoint[0]=x;localPoint[1]=y;localPoint[2]=z;localPoint[3]=1;Matrix.multiplyMV(worldPoint,0,transform,0,localPoint,0);Matrix.multiplyMV(projectPoint,0,vp,0,worldPoint,0);float w=projectPoint[3];projectPoint[0]=.5f+projectPoint[0]/w*.5f;projectPoint[1]=.5f-projectPoint[1]/w*.5f;projectPoint[2]/=w;}
    private float mechanical(float t){return t<.82f?1.025f*smooth(t/.82f):1+.025f*(1-smooth((t-.82f)/.18f));}
    private float clamp(float t){return Math.max(0,Math.min(1,t));}
    private float smooth(float t){return t*t*(3-2*t);}
    private void part(SceneMesh mesh,float x,float y,float z,float sx,float sy,float sz,float rx,float ry,float rz,float r,float g,float b,float material,float alpha){mesh(mesh,x,y,z,sx,sy,sz,rx,ry,rz,r,g,b,material,alpha,0);}
    private void part(SceneMesh mesh,float x,float y,float z,float sx,float sy,float sz,float rx,float ry,float rz,float r,float g,float b,float material,float alpha,int texture){mesh(mesh,x,y,z,sx,sy,sz,rx,ry,rz,r,g,b,material,alpha,texture);}
    private void mesh(SceneMesh mesh,float x,float y,float z,float sx,float sy,float sz,float rx,float ry,float rz,float r,float g,float b,float material,float alpha){mesh(mesh,x,y,z,sx,sy,sz,rx,ry,rz,r,g,b,material,alpha,0);}
    private void mesh(SceneMesh mesh,float x,float y,float z,float sx,float sy,float sz,float rx,float ry,float rz,float r,float g,float b,float material,float alpha,int texture){
        if(inGun)System.arraycopy(root,0,model,0,16);else Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);if(rx!=0)Matrix.rotateM(model,0,rx,1,0,0);if(ry!=0)Matrix.rotateM(model,0,ry,0,1,0);if(rz!=0)Matrix.rotateM(model,0,rz,0,0,1);Matrix.scaleM(model,0,sx,sy,sz);
        drawPrepared(mesh,r,g,b,material,alpha,texture);
    }
    private void drawPrepared(SceneMesh mesh,float r,float g,float b,float material,float alpha,int texture){
        Matrix.multiplyMM(mvp,0,vp,0,model,0);
        if(!shadowPass){Matrix.invertM(inverse,0,model,0);Matrix.transposeM(normal,0,inverse,0);}
        else {material=10;alpha=.22f;}
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniformMatrix4fv(uNormal,1,false,normal,0);GLES20.glUniform4f(uColor,r,g,b,alpha);GLES20.glUniform1f(uMaterial,material);GLES20.glUniform1f(uCutaway,mesh==cylinderSkin&&!shadowPass&&focus>.05f?1:0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture==0?textures[0]:texture);GLES20.glUniform1i(uTex,0);
        mesh.vertices.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,32,mesh.vertices);GLES20.glEnableVertexAttribArray(aPos);mesh.vertices.position(3);GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,32,mesh.vertices);GLES20.glEnableVertexAttribArray(aNormal);mesh.vertices.position(6);GLES20.glVertexAttribPointer(aUv,2,GLES20.GL_FLOAT,false,32,mesh.vertices);GLES20.glEnableVertexAttribArray(aUv);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,mesh.count);
    }
    private static int shader(int type,String source){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,source);GLES20.glCompileShader(s);int[]ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(s));return s;}
}
