package de.spas.silverball;

import android.content.Context;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.SoundEffectConstants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.spas.silverball.model.Level;
import de.spas.silverball.model.Trap;


/**
 * Created by uwe on 23.09.13.
 */
public class GameEngine implements SensorEventListener, Runnable {

    private static final float BOUNCE_FACTOR = 0.25f;
    private static final long MS_PER_FRAME = 20;
    private static final float BOUNCE_SOUND_THRESHOLD = 500f;
    private static final double BOUNCE_ANGLE_SOUND_THRESHOLD = 30.0*Math.PI/180;
    public static final String LOG_TAG = "GameEngine";
    private static final float HALT_BALL_THRESHOLD = 1f;
    private final SoundPool soundPool;
    private final int successSoundId, lavaSoundId;


    interface OnBallInHoleListener {
        void onBallInHole(int score);
    }
    interface OnGameOverListener {
        void onGameOver();
    }

    private final static float ACCELERATION_SCALE=400f;
    private float ballX, ballY;
    private float ballVX, ballVY;
    private float ballAX, ballAY;
    private float holeX, holeY;
    private float holeRadius;
    private float minX, minY, maxX, maxY, hd, vd;
    private RectF collisionRect = new RectF();
    private GameTextureView gameView;
    private SensorManager sensorManager;
    private ScheduledExecutorService service;
    private Handler handler = new Handler();
    private OnBallInHoleListener onBallInHoleListener;
    private OnGameOverListener onGameOverListener;
    private final Level level;
    private int pointsStart;
    private int points;
    private int time;
    private long deadline;
    private final AudioManager audioManager;

    public GameEngine(Context context, SensorManager sensorManager, GameTextureView gameView, OnBallInHoleListener onBallInHoleListener, OnGameOverListener onGameOverListener, Level level) {
        this.sensorManager = sensorManager;
        this.gameView = gameView;
        this.onBallInHoleListener = onBallInHoleListener;
        this.onGameOverListener = onGameOverListener;
        this.level = level;
        audioManager = (AudioManager) ((context.getSystemService(Context.AUDIO_SERVICE)));

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        successSoundId = soundPool.load(context,R.raw.success,1);
        lavaSoundId = soundPool.load(context, R.raw.lava,1);
    }

    public void setRegion(float minX,float minY, float maxX, float maxY) {
        this.minX=minX;
        this.minY=minY;
        this.maxX=maxX;
        this.maxY=maxY;
    }

    public void setBallPosition(float x, float y) {
        ballX = x;
        ballY = y;
    }

    public void setHolePosition(float x, float y, float radius) {
        holeX = x;
        holeY = y;
        holeRadius = radius;
    }


    public void start() {
        service = Executors.newSingleThreadScheduledExecutor();
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        ballVX=0;
        ballVY=0;
        ballAX=0;
        ballAY=0;
        time = level.getTime();
        deadline = System.currentTimeMillis()+time*1000;

        float ballRadius = gameView.getBaseDimension()/2;
        hd = gameView.getHorizontalBaseDimension();
        vd = gameView.getVerticalBaseDimension();
        setRegion(ballRadius, ballRadius ,gameView.getWidth()- ballRadius ,gameView.getHeight()- ballRadius );
        setBallPosition(level.getBall().getStartx() * hd, level.getBall().getStarty() * vd);
        setHolePosition(level.getHole().getX() * hd, level.getHole().getY() * vd, ballRadius);
        points = pointsStart = level.getPoints();
        gameView.setTraps(level.getTraps());
        gameView.setLevel(Integer.toString(level.getNumber()));
        gameView.setBallPosition(ballX, ballY);
        gameView.setHolePosition(holeX,holeY);
        service.scheduleAtFixedRate(this,MS_PER_FRAME,MS_PER_FRAME, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        service.shutdown();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        ballAX = sensorEvent.values[1]*ACCELERATION_SCALE;
        ballAY = sensorEvent.values[0]*ACCELERATION_SCALE;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // not needed
    }

    @Override
    public void run() {

        if(System.currentTimeMillis() > deadline) {
            stop();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onGameOverListener.onGameOver();
                }
            });
            return;
        }

        ballVX+=ballAX*MS_PER_FRAME/1000;
        ballVY+=ballAY*MS_PER_FRAME/1000;
        ballX+=ballVX*MS_PER_FRAME/1000;
        ballY+=ballVY*MS_PER_FRAME/1000;

        points = Math.round((deadline-System.currentTimeMillis()) * pointsStart / time / 1000 );
        gameView.setPoints(points);

        if (ballX < minX) {
            if (ballVX < -BOUNCE_SOUND_THRESHOLD) playBounceSound();
            ballX = minX;
            ballVX = -ballVX * BOUNCE_FACTOR;
        }
        if (ballY < minY) {
            if (ballVY < -BOUNCE_SOUND_THRESHOLD) playBounceSound();
            ballY = minY;
            ballVY = -ballVY * BOUNCE_FACTOR;
        }
        if (ballX > maxX) {
            if (ballVX > BOUNCE_SOUND_THRESHOLD) playBounceSound();
            ballX = maxX;
            ballVX = -ballVX * BOUNCE_FACTOR;
        }
        if (ballY > maxY) {
            if (ballVY > BOUNCE_SOUND_THRESHOLD) playBounceSound();
            ballY = maxY;
            ballVY = -ballVY * BOUNCE_FACTOR;
        }

        for(Trap trap : level.getTraps()) {
            collisionRect.set(trap.getX()*hd, trap.getY()*vd,(trap.getX()+trap.getW())*hd-1,(trap.getY()+trap.getH())*vd-1);
            if(collisionRect.contains(ballX,ballY)) {
                hitTrap();
                return;
            }
        }

        if(Math.sqrt((ballX-holeX)*(ballX-holeX) + (ballY-holeY)*(ballY-holeY)) < holeRadius ) {
            stop();
            playSuccessSound();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onBallInHoleListener.onBallInHole(points);
                }
            });
        }

        gameView.setBallPosition(ballX,ballY);

        //Log.d(getClass().getSimpleName(), Integer.toString(gameView.getFps()) + " fps");

    }

    private void playSuccessSound() {
        if(successSoundId!=0) {
            soundPool.play(successSoundId, 1, 1,1,0,1f);
        }
    }

    private void playBounceSound() {
        audioManager.playSoundEffect(SoundEffectConstants.CLICK);
    }

    private void hitTrap() {
        if(lavaSoundId!=0) {
            soundPool.play(lavaSoundId, 1, 1, 1, 0, 1f);
        }
        stop();
        handler.post(new Runnable() {
            @Override
            public void run() {
                onGameOverListener.onGameOver();
            }
        });
    }


}
