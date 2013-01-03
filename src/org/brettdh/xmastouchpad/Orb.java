package org.brettdh.xmastouchpad;

import edu.umich.xmaspi.client.Bulbs;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.DisplayMetrics;
import android.view.Display;

class Orb {
    private float posX;
    private float posY;
    private float radius;
    private int viewWidth;
    private int viewHeight;
    private int color;
    private Paint paint;
    private TouchpadView parent;
    
    public Orb(TouchpadView parent, float x, float y) {
        this.parent = parent;
        this.viewWidth = parent.getWidth();
        this.viewHeight = parent.getHeight();
        
        Activity activity = (Activity) parent.getContext();
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        
        float diameterInches = 0.5f;
        this.radius = (dm.xdpi * diameterInches) / 2.0f;
        
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Style.FILL);
        
        setPosition(x, y);
    }
    
    private final int colors[] = new int[] {
        Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.argb(255, 255, 0, 255), Color.WHITE
    };
    
    // color interpolation from http://stackoverflow.com/a/7871291
    private float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }
    
    /** Returns an interpolated color, between a and b */
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
        float xPercent = posX / viewWidth;
        int index = (int) Math.floor(xPercent * (colors.length - 1));
        int leftColor = colors[index];
        int rightColor;
        if (index == colors.length - 1) {
            rightColor = leftColor;
        } else {
            rightColor = colors[index + 1];
        }
        
        int binWidth = viewWidth / (colors.length - 1);
        int binLeft = binWidth * index;
        int binRight = binLeft + binWidth;
        float proportion = ((float) posX - binLeft) / (binRight - binLeft);
        return interpolateColor(leftColor, rightColor, proportion);
    }
    
    public void setPosition(float x, float y) {
        int oldBulb = getBulb(posY);
        
        posX = x;
        posY = y;
        
        int bulb = getBulb(y);
        if (oldBulb != bulb) {
            fadeBulb(oldBulb);
        }
        
        int color = getColor();
        parent.lights.setColor(bulb, color);
    }
    
    public void fadeBulb() {
        fadeBulb(posY);
    }
    
    public void fadeBulb(float y) {
        int bulb = getBulb(y);
        parent.lights.fade(bulb);
    }

    private int getBulb(float y) {
        int bulb = (int) (y / (viewHeight / 100.0));
        bulb = Math.min(bulb, Bulbs.COUNT - 1);
        bulb = Math.max(bulb, 0);
        return bulb;
    }
    
    public void draw(Canvas canvas) {
        int color = getColor();
        paint.setColor(color);
        parent.setDrawStyle(paint);
        
        canvas.drawCircle(posX, posY, radius, paint);
    }
}