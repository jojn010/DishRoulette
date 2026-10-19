package com.example.dishroulette;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Static optical framing. No timers, touch interception, or texture assets. */
public final class SceneHudView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public SceneHudView(Context c, AttributeSet attrs) { super(c, attrs); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    @Override protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density, w = getWidth(), h = getHeight();
        paint.setShader(new RadialGradient(w*.48f,h*.49f,Math.max(w,h)*.68f,
                new int[]{0x00101310,0x00101310,0xB8101310},new float[]{0,.40f,1}, Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,paint); paint.setShader(null);
        paint.setStrokeWidth(d); paint.setColor(0x805D6750);
        float x=16*d,y=12*d,l=15*d;
        for(int side=0;side<2;side++)for(int row=0;row<2;row++){
            float xx=side==0?x:w-x, yy=row==0?y:h-70*d;
            c.drawLine(xx,yy,xx+(side==0?l:-l),yy,paint);
            c.drawLine(xx,yy,xx,yy+(row==0?l:-l),paint);
        }
        paint.setColor(0x504D5748);
        for(int i=0;i<11;i++){float xx=w*.29f+i*w*.042f;c.drawLine(xx,h-76*d,xx,h-(i%5==0?81:78)*d,paint);}
        paint.setColor(0xFF92A47D);c.drawCircle(w-24*d,28*d,2*d,paint);
    }
}
