package com.example.dishroulette;

/** UI-thread inspection pose, deliberately independent of ingredient storage. */
final class InspectionState {
    float yaw, pitch, zoom=1, cylinderAngle;
    void orbit(float horizontal,float vertical){yaw=clamp(yaw+horizontal,-82,82);pitch=clamp(pitch+vertical,-58,58);}
    void scale(float factor){if(Float.isFinite(factor)&&factor>0)zoom=clamp(zoom/factor,.24f,1.8f);}
    void resetCamera(){yaw=0;pitch=0;zoom=1;}
    static float restingAngle(float angle,float velocity){
        float momentum=Math.abs(velocity)>240?clamp(velocity,-720,720)*.09f:0;
        return Math.round((angle+momentum)/60f)*60f;
    }
    static int chamber(float restingAngle){return Math.floorMod(-Math.round(restingAngle/60f),6);}
    static float alignedAngle(float current,int chamber){
        float target=-chamber*60f;
        return target+Math.round((current-target)/360f)*360f;
    }
    private static float clamp(float n,float min,float max){return Math.max(min,Math.min(max,n));}
}
