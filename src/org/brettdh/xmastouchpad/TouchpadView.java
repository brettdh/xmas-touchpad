package org.brettdh.xmastouchpad;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

// starting from this code: http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html

public class TouchpadView extends View {
    // map pointer id to orb to show.
    private Map<Integer, Orb> orbs = new HashMap<Integer, Orb>();
    
    LightsThread lights;
    
    public TouchpadView(Context context) {
        this(context, null, 0);
    }
    
    public TouchpadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public TouchpadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        lights = new LightsThread((XmasTouchpad) getContext());
        lights.start();
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Orb orb : orbs.values()) {
            orb.draw(canvas);
        }
    }
    
    private void addOrb(int id, float x, float y) {
        orbs.put(id, new Orb(this, x, y));
    }
    
    private void removeOrb(int id) {
        Orb orb = orbs.get(id);
        orb.fadeBulb();
        orbs.remove(id);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                if (!lights.isConnected()) {
                    lights.connect("10.0.2.2");
                }
                
                // fall-through
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = ev.getActionIndex();
                int id = ev.getPointerId(index);
                addOrb(id, ev.getX(index), ev.getY(index));
                break;
            }
            
            case MotionEvent.ACTION_MOVE: {
                int pointerCount = ev.getPointerCount();
                for (int i = 0; i < pointerCount; ++i) {
                    int id = ev.getPointerId(i);
                    Orb orb = orbs.get(id);
                    orb.setPosition(ev.getX(i), ev.getY(i));
                }
                break;
            }
            
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                int index = ev.getActionIndex();
                int id = ev.getPointerId(index);
                removeOrb(id);
                break;
            }
        }
        invalidate();        
        
        return true;
    }

    private DashPathEffect dashEffect = new DashPathEffect(new float[]{10,10}, 0);
    
    public void setDrawStyle(Paint paint) {
        paint.setStyle(Style.FILL);
        if (!lights.isConnected()) {
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(10);
            paint.setPathEffect(dashEffect);
        }
    }
    
}
