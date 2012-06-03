/*
 * Pomodoro - Copyright (c) 2010 Pablo Seminario
 * This software is distributed under the terms of the GNU General
 * Public License
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.*;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.midlet.MIDlet;

public class Pomodoro extends MIDlet {

    private Display display;
    private CanvasCounter canvas;
    private static int counter;
    // Pixel width deplacement between updates
    // this value is determined in layout.png
    private static final int PIXEL_WIDTH = 9;
    // Quantity of minutes to count
    // private static final int MINUTES = 25;
    // Total of pixels in X minutes
    // private static final int COUNTER_LIMIT = PIXEL_WIDTH * MINUTES;
    private static final int VIBRATION_TIME = 1500;

    public Pomodoro() {
        display = Display.getDisplay(this);
        canvas = new CanvasCounter(this);
        counter = PIXEL_WIDTH * canvas.getMinutes();
    }

    protected void startApp() {
        display.setCurrent(canvas);
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
        notifyDestroyed();
    }

    public void exitMIDlet() {
        destroyApp(true);
    }

    public void addCounter() {
        counter += PIXEL_WIDTH;
    }

    public void subCounter() {
        counter -= PIXEL_WIDTH;
    }

    public void resetCounter() {
        counter = PIXEL_WIDTH * canvas.getMinutes();
    }

    public int getCounter() {
        return counter;
    }

    public void signal() {
        boolean ok = Display.getDisplay(this).flashBacklight(VIBRATION_TIME);
        if(!ok) {
            // TODO
        }
    }

    public void showException(Exception e) {
        Alert a = new Alert("Exception", e.toString(), null, null);
        a.setTimeout(Alert.FOREVER);
        display.setCurrent(a);
    }
        
}

class CanvasCounter extends Canvas implements CommandListener {
    
    private final Command START_CMD = new Command("Start", Command.EXIT, 0);
    private final Command STOP_CMD = new Command("Stop", Command.EXIT, 0);
    private final Command EXIT_CMD = new Command("Exit", Command.SCREEN, 2);
    
    private final Command CIR_MINS_CMD = new Command("Change Mins.", Command.SCREEN, 0);
    
    private Pomodoro midlet;
    private Image image;
    private Image layout;
    private Image pointer;
    private Image pointer_stop;
    
    // Define interval in milliseconds between screen updates
    // 1 minute = 60000 milliseconds (You must know it!)
    private static final int INTERVAL = 60000;
    private Timer tm;
    private PomodoroTimer tt;
    private Player player;
    
    // default minutes to count
    private int minutes = 25;
    private boolean inPomodoro = false;

    public CanvasCounter(Pomodoro midlet) {
        this.midlet = midlet;

        try {
            image = Image.createImage("/background.png");
            layout = Image.createImage("/layout.png");
            pointer = Image.createImage("/pointer.png");
            pointer_stop = Image.createImage("/pointer_stop.png");
        } catch (Exception e) {
        }

        // Detect sound formats supported
        String[] types = Manager.getSupportedContentTypes(null);
        String soundfile = "/ring.mp3";
        String soundtype = "audio/mpeg";
        for (int i = 0; i < types.length; i++) {
            if ("audio/x-wav".equals(types[i])) {
                soundfile = "/ring.wav";
                soundtype = types[i];
            }
        }

        try {
            InputStream in = getClass().getResourceAsStream(soundfile);
            player = Manager.createPlayer(in, soundtype);
            player.realize();
        } catch (Exception e) {
            midlet.showException(e);
            return;
        }

        addCommand(START_CMD);
        addCommand(CIR_MINS_CMD);
        addCommand(EXIT_CMD);
        setCommandListener(this);
    }

    protected void keyPressed(int keyCode) {
        String keyValue = getKeyName(keyCode);        
        if(keyValue.toUpperCase().equals("SELECT") && this.isShown()) {
            if(!inPomodoro) {
                circulateMinutes();
                midlet.resetCounter();
                repaint();       
            }
        }
    }    
    
    protected void paint(Graphics g) {
        drawPomodoro(g);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == EXIT_CMD) {
            setCommandListener(null);
            midlet.exitMIDlet();
        } else if (c == CIR_MINS_CMD && !inPomodoro) {
            circulateMinutes();
            midlet.resetCounter();
            repaint();
        } else if (c == START_CMD) {
            startTimer();
            repaint();
        } else if (c == STOP_CMD) {
            stopTimer();
            repaint();
        }
    }

    private void startTimer() {
        tm = new Timer();
        tt = new PomodoroTimer();
        tm.schedule(tt, INTERVAL, INTERVAL);
        removeCommand(START_CMD);
        addCommand(STOP_CMD);
        setCommandListener(this);
        inPomodoro = true;
    }
    
    private void stopTimer(boolean normal) {
        tm.cancel();
        //
        // if stop normally
        //   reset timer for rest/work
        //
        if(normal == true) {
            if(this.minutes == 25) {
                this.minutes = 5;
            } else if(this.minutes == 5) {
                this.minutes = 25;
            }
        }                
        midlet.resetCounter();
        removeCommand(STOP_CMD);
        addCommand(START_CMD);
        setCommandListener(this);
        repaint();
        inPomodoro = false;
    }    

    private void stopTimer() {
        stopTimer(false);
    }

    private void drawPomodoro(Graphics g) {
        // Paint red background
        g.setColor(138, 8, 8);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw scale of numbers
        g.drawImage(layout, (getWidth() / 2) - (pointer.getWidth() / 2) - midlet.getCounter(),
                (getHeight() / 2) + (layout.getHeight() / 2), Graphics.BOTTOM | Graphics.LEFT);

        // Draw pointer
        g.drawImage(pointer, (getWidth() / 2) - (pointer.getWidth() / 2),
                (getHeight() / 2) + (layout.getHeight() / 2), Graphics.TOP | Graphics.LEFT);

        // Draw tomato
        g.drawImage(image, 0, 0, Graphics.TOP | Graphics.LEFT);
    }

    private void circulateMinutes() {
        if(minutes == 25) {
            minutes = 15;
        } else if(minutes == 15) {
            minutes = 5;
        } else if(minutes == 5) {
            minutes = 25;
        }
    }

    int getMinutes() {
        return this.minutes;
    }

    private class PomodoroTimer extends TimerTask {

        public final void run() {
            midlet.subCounter();
            repaint();

            if (midlet.getCounter() == 0) {
                try {
                    player.start();
                } catch (Exception e) {
                    midlet.showException(e);
                    return;
                }
                // midlet.signal();
                stopTimer(true);
            }
        }
    }
}
