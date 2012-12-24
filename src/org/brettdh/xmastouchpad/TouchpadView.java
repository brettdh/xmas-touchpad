package org.brettdh.xmastouchpad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Config;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

// starting from this code: http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html

public class TouchpadView extends View {
    private class Orb {
        private float posX;
        private float posY;
        private float radius;
        private int screenWidth;
        private int color;
        private Paint paint;
        private TouchpadView parent;
        
        public Orb(TouchpadView parent, float x, float y) {
            this.parent = parent;
            this.posX = x;
            this.posY = y;
            
            Activity activity = (Activity) parent.getContext();
            Display display = activity.getWindowManager().getDefaultDisplay();
            DisplayMetrics dm = new DisplayMetrics();
            display.getMetrics(dm);
            
            this.screenWidth = dm.widthPixels;
            
            float diameterInches = 0.5f;
            this.radius = (dm.xdpi * diameterInches) / 2.0f;
            
            paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Style.FILL);
        }
        
        private final int colors[] = new int[] {
            Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.argb(255, 255, 0, 255), Color.WHITE
        };
        
        // color interpolation from http://stackoverflow.com/a/7871291
        private float interpolate(float a, float b, float proportion) {
            return (a + ((b - a) * proportion));
        }
        
        /** Returns an interpoloated color, between a and b */
        private int interpolateColor(int a, int b, float proportion) {
            float[] hsva = new float[3];
            float[] hsvb = new float[3];
            Color.colorToHSV(a, hsva);
            Color.colorToHSV(b, hsvb);
            for (int i = 0; i < 3; i++) {
                hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
            }
            return Color.HSVToColor(hsvb);
        }
          
        private int getColor() {
            float xPercent = posX / screenWidth;
            int index = (int) Math.floor(xPercent * (colors.length - 1));
            int leftColor = colors[index];
            int rightColor;
            if (index == colors.length - 1) {
                rightColor = leftColor;
            } else {
                rightColor = colors[index + 1];
            }
            
            int binWidth = screenWidth / (colors.length - 1);
            int binLeft = binWidth * index;
            int binRight = binLeft + binWidth;
            float proportion = ((float) posX - binLeft) / (binRight - binLeft);
            return interpolateColor(leftColor, rightColor, proportion);
        }
        
        public void setPosition(float x, float y) {
            posX = x;
            posY = y;
        }
        
        public void draw(Canvas canvas) {
            int color = getColor();
            paint.setColor(color);
            
            canvas.drawCircle(posX, posY, radius, paint);
        }
    }
    
    // map pointer id to orb to show.
    private Map<Integer, Orb> orbs = new HashMap<Integer, Orb>();
    
    public TouchpadView(Context context) {
        this(context, null, 0);
    }
    
    public TouchpadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public TouchpadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
        orbs.remove(id);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = ev.getActionIndex();
                int id = ev.getPointerId(index);
                addOrb(0, ev.getX(index), ev.getY(index));
                break;
            }
            
            case MotionEvent.ACTION_MOVE: {
                int pointerCount = ev.getPointerCount();
                for (int i = 0; i < pointerCount; ++i) {
                    int id = ev.getPointerId(i);
                    Orb orb = orbs.get(i);
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
    
}
