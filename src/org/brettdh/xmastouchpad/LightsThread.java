package org.brettdh.xmastouchpad;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.graphics.Color;
import android.util.Log;

import edu.umich.xmaspi.client.Bulbs;
import edu.umich.xmaspi.client.Iterator;
import edu.umich.xmaspi.client.Remote;

public class LightsThread extends Thread {
    private class Message {
        int bulb;
        int color;
        boolean fade;
        boolean quit;
        String hostname;
        
        Message(String hostname) {
            this(Bulbs.COUNT, 0);
            this.hostname = hostname;
        }
        
        Message(int bulb, int color) {
            this.bulb = bulb;
            this.color = color;
            this.fade = false;
            this.quit = false;
            this.hostname = null;
        }
        
        // fade this bulb.
        Message(int bulb) {
            this(bulb, 0);
            this.fade = true;
        }
        
        Message() {
            this(-1, 0);
            this.quit = true;
        }
    }

    private static final String TAG = "LightsThread";
    
    private BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
    
    private Remote remote;
    private Bulbs bulbs;
    private Iterator lightsIter;
    
    private int[] colors;

    private boolean connected;
    
    private XmasTouchpad activity;

    private Timer timer;
    
    public LightsThread(XmasTouchpad activity) {
        this.activity = activity;
        colors = new int[Bulbs.COUNT];
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
        timer = new Timer();

        while (true) {
            Message m;
            try {
                m = queue.take();
            } catch (InterruptedException e) {
                continue;
            }
            
            if (m.quit) {
                break;
            } else if (m.hostname == null) {
                continue;
            }
            
            activity.updateStatus("Connecting to " + m.hostname);
            if (!startBulbControl(m.hostname)) {
                activity.updateStatus("Disconnected");
                continue;
            }
            
            setConnected(true);
            
            final String hostname = m.hostname;
            
            TimerTask updateTask = new TimerTask() {
                private int duration = 0;
                
                @Override
                public void run() {
                    String msg = String.format("Connected to " + hostname + " (%d seconds)", duration);
                    duration++;
                    activity.updateStatus(msg);
                }
            };
            timer.schedule(updateTask, 0, 1000);
            
            boolean keepGoing = true;
            while (keepGoing) {
                keepGoing = doBulbsUpdate();
            }
            queue.clear();
            setConnected(false);
            
            updateTask.cancel();
            
            activity.updateStatus("Disconnected");
        }
    }

    private synchronized void setConnected(boolean connected) {
        this.connected = connected;
    }

    private boolean startBulbControl(String hostname) {
        try {
            remote = new Remote("touchpad", hostname);
        } catch (IOException e) {
            Log.e(TAG, "Socket exception: " + e.getMessage());
            return false;
        }
        bulbs = new Bulbs(remote);
        lightsIter = new Iterator(bulbs);
        
        return true;
    }

    private boolean doBulbsUpdate() {
        try {
            lightsIter.render();
            
            Message m = queue.poll(10, TimeUnit.MILLISECONDS);
            if (m != null) {
                if (m.quit) {
                    return false;
                }
                
                if (m.fade) {
                    int[] color = getColor(colors[m.bulb]);
                    lightsIter.add(Iterator.makeFixedIterator(m.bulb), 
                                   Iterator.makeFaderIterator(color, Bulbs.BLACK.toArray(), 50));
                } else {
                    int[] color = getColor(m.color);
                    colors[m.bulb] = m.color;
                    lightsIter.add(Iterator.makeFixedIterator(m.bulb),
                                   Iterator.makeSolidIterator(color));
                }
            }
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    public void connect(String hostname) {
        queue.add(new Message(hostname));
    }
    
    int setCalls = 0;
    public void setColor(int bulb, int color) {
        setCalls++;
        Log.d("setColorCalls", String.valueOf(setCalls));
        
        queue.add(new Message(bulb, color));
    }
    
    public void fade(int bulb) {
        queue.add(new Message(bulb));
    }
    
    public void quit() {
        queue.add(new Message());
    }

    public synchronized boolean isConnected() {
        return connected;
    }
}
