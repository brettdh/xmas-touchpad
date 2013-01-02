package org.brettdh.xmastouchpad;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.graphics.Color;

import edu.umich.xmaspi.client.Bulbs;
import edu.umich.xmaspi.client.Iterator;
import edu.umich.xmaspi.client.Remote;

public class LightsThread extends Thread {
    private class Message {
        int bulb;
        int color;
        boolean fade;
        boolean quit;
        
        Message(int bulb, int color) {
            this.bulb = bulb;
            this.color = color;
            this.fade = false;
            this.quit = false;
        }
        
        // fade this bulb.
        Message(int bulb) {
            this.bulb = bulb;
            this.color = 0;
            this.fade = true;
            this.quit = false;
        }
        
        Message() {
            this.bulb = -1;
            this.color = 0;
            this.fade = false;
            this.quit = true;
        }
    }
    
    private BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
    
    private Remote remote;
    private Bulbs bulbs;
    private Iterator lightsIter;
    
    private String hostname;
    
    public LightsThread(String hostname) {
        this.hostname = hostname;
    }
    
    private int[] getColor(int color) {
        int[] bulbColor = new int[4];
        bulbColor[0] = (int) (Color.red(color) / 255.0 * 15);
        bulbColor[1] = (int) (Color.green(color) / 255.0 * 15);
        bulbColor[2] = (int) (Color.blue(color) / 255.0 * 15);
        bulbColor[3] = Color.alpha(color); // already in [0,255]
        return bulbColor;
    }
    
    @Override
    public void run() {
        try {
            remote = new Remote(hostname);
        } catch (IOException e) {
            return;
        }
        
        bulbs = new Bulbs(remote);
        lightsIter = new Iterator(bulbs);
        int[] colors = new int[Bulbs.COUNT];
        
        while (true) {
            try {
                lightsIter.render();
                
                Message m = queue.poll(10, TimeUnit.MILLISECONDS);
                if (m != null) {
                    if (m.quit) {
                        break;
                    }
                    
                    if (m.fade) {
                        int[] color = getColor(colors[m.bulb]);
                        lightsIter.add(Iterator.makeFixedIterator(m.bulb), 
                                       Iterator.makeFaderIterator(color, Bulbs.BLACK.toArray(), 100));
                    } else {
                        int[] color = getColor(m.color);
                        colors[m.bulb] = m.color;
                        lightsIter.add(Iterator.makeFixedIterator(m.bulb),
                                       Iterator.makeSolidIterator(color));
                    }
                }
            } catch (IOException e) {
                break;
            } catch (InterruptedException e) {
                break;
            }
        }
    }
        
    public void setColor(int bulb, int color) {
        queue.add(new Message(bulb, color));
    }
    
    public void fade(int bulb) {
        queue.add(new Message(bulb));
    }
    
    public void quit() {
        queue.add(new Message());
    }
}
