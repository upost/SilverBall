package de.spas.silverball;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.spas.silverball.model.Level;
import de.spas.silverball.model.Trap;
import de.spas.math.Matrix2;
import de.spas.math.Vector2;


/**
 * Created by uwe on 23.09.13.
 */
public class GameEngine implements SensorEventListener, Runnable {

    private static final long FRAME_INTERVAL = GameTextureView.FRAME_INTERVAL;
    private static final float BOUNCE_SOUND_THRESHOLD = 5f;
    public static final String LOG_TAG = "GameEngine";
    private final SoundPool soundPool;
    private final int successSoundId, gameOverSoundId, hitSoundId;
    private final static float ACCELERATION_SCALE=12f;
    private Vector2 location, velocity, acceleration,previousPosition;
    private GameTextureView gameView;
    private SensorManager sensorManager;
    private ScheduledExecutorService service;
    private OnGameEventListener onGameEventListener;
    private final Level level;
    private int pointsStart;
    private int points;
    private int time;
    private long deadline;


    interface OnGameEventListener {
        void onBallInHole(int score);
        void onGameOver();
    }

    public GameEngine(Context context, SensorManager sensorManager, GameTextureView gameView, OnGameEventListener onGameEventListener, Level level) {
        this.sensorManager = sensorManager;
        this.gameView = gameView;
        this.onGameEventListener = onGameEventListener;
        this.level = level;
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        successSoundId = soundPool.load(context,R.raw.success,1);
        gameOverSoundId = soundPool.load(context, R.raw.lava,1);
        hitSoundId = soundPool.load(context, R.raw.hit,1);
    }



    public void start() {
        service = Executors.newSingleThreadScheduledExecutor();
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        location = new Vector2(level.getBall().getStartx(), level.getBall().getStarty());
        velocity = new Vector2();
        previousPosition = new Vector2();
        acceleration = new Vector2();
        time = level.getTime();
        deadline = System.currentTimeMillis()+time*1000;
        points = pointsStart = level.getPoints();

        gameView.startLevel(level);

        service.scheduleAtFixedRate(this, FRAME_INTERVAL, FRAME_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        gameView.setPlaying(false);
        service.shutdown();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        acceleration.x = sensorEvent.values[1]*ACCELERATION_SCALE;
        acceleration.y = sensorEvent.values[0]*ACCELERATION_SCALE;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // not needed
    }

    @Override
    public void run() {

        if(System.currentTimeMillis() > deadline) {
            stop();
            onGameEventListener.onGameOver();
            return;
        }

        // ball movement
        velocity.add(acceleration,FRAME_INTERVAL *0.001f);
        previousPosition.copyFrom(location);
        location.add(velocity, FRAME_INTERVAL *0.001f);
        gameView.setBallPosition(location);

        // calc points
        points = Math.round((deadline-System.currentTimeMillis()) * pointsStart*0.001f / time  );
        gameView.setPoints(points);

        boolean bounced = gameView.checkBounce();
        if(bounced) {
            Matrix2 bounceMatrix = gameView.getBounceMatrix();
            velocity = bounceMatrix.multiply(velocity);

            if(velocity.length()>BOUNCE_SOUND_THRESHOLD) {
                playSound(hitSoundId);
            }
            // move ball to bounced position instead
            location.copyFrom(previousPosition);
            location.add(velocity, FRAME_INTERVAL *0.001f);
            gameView.setBallPosition(location);
        }

        Trap trap = gameView.getHitTrap();

        if(trap!=null) {
            hitTrap();
            return;
        }

        if(gameView.isBallInHole()) {
            stop();
            playSound(successSoundId);
            onGameEventListener.onBallInHole(points);

        }

        //Log.d(getClass().getSimpleName(), Integer.toString(gameView.getFps()) + " fps");

    }

    private void playSound(int id) {
            soundPool.play(id, 1, 1,1,0,1f);
    }


    private void hitTrap() {
        playSound(gameOverSoundId);
        stop();
        onGameEventListener.onGameOver();

    }


}
