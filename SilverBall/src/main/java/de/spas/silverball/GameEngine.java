package de.spas.silverball;

import android.content.Context;
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
    private static final long FRAME_INTERVAL = GameTextureView.FRAME_INTERVAL;
    private static final float BOUNCE_SOUND_THRESHOLD = 500f;
    public static final String LOG_TAG = "GameEngine";
    private final SoundPool soundPool;
    private final int successSoundId, gameOverSoundId;
    private final static float ACCELERATION_SCALE=400f;
    private float ballVX, ballVY;
    private float ballAX, ballAY;
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

    interface OnBallInHoleListener {
        void onBallInHole(int score);
    }
    interface OnGameOverListener {
        void onGameOver();
    }

    public GameEngine(Context context, SensorManager sensorManager, GameTextureView gameView, OnBallInHoleListener onBallInHoleListener, OnGameOverListener onGameOverListener, Level level) {
        this.sensorManager = sensorManager;
        this.gameView = gameView;
        this.onBallInHoleListener = onBallInHoleListener;
        this.onGameOverListener = onGameOverListener;
        this.level = level;
        audioManager = (AudioManager) ((context.getSystemService(Context.AUDIO_SERVICE)));

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        successSoundId = soundPool.load(context,R.raw.success,1);
        gameOverSoundId = soundPool.load(context, R.raw.lava,1);
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

        // ball movement
        ballVX += ballAX* FRAME_INTERVAL /1000;
        ballVY += ballAY* FRAME_INTERVAL /1000;

        gameView.moveBall(ballVX* FRAME_INTERVAL /1000,ballVY* FRAME_INTERVAL /1000);

        // calc points
        points = Math.round((deadline-System.currentTimeMillis()) * pointsStart / time / 1000 );
        gameView.setPoints(points);

        // check playfield bounds and bounce
        if(!gameView.isBallInPlayfield()) {
            if (gameView.getBallX() < gameView.getPlayfield().left) {
                if (ballVX < -BOUNCE_SOUND_THRESHOLD) playBounceSound();
                gameView.setBallX(gameView.getPlayfield().left);
                ballVX = -ballVX * BOUNCE_FACTOR;
            }
            if (gameView.getBallY() < gameView.getPlayfield().top) {
                if (ballVY < -BOUNCE_SOUND_THRESHOLD) playBounceSound();
                gameView.setBallY(gameView.getPlayfield().top);
                ballVY = -ballVY * BOUNCE_FACTOR;
            }
            if (gameView.getBallX() > gameView.getPlayfield().right) {
                if (ballVX > BOUNCE_SOUND_THRESHOLD) playBounceSound();
                gameView.setBallX(gameView.getPlayfield().right);
                ballVX = -ballVX * BOUNCE_FACTOR;
            }
            if (gameView.getBallY() > gameView.getPlayfield().bottom) {
                if (ballVY > BOUNCE_SOUND_THRESHOLD) playBounceSound();
                gameView.setBallY(gameView.getPlayfield().bottom);
                ballVY = -ballVY * BOUNCE_FACTOR;
            }
        }

        Trap trap = gameView.getHitTrap();

        if(trap!=null) {
            hitTrap();
            return;
        }

        if(gameView.isBallInHole()) {
            stop();
            playSuccessSound();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onBallInHoleListener.onBallInHole(points);
                }
            });
        }

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
        if(gameOverSoundId !=0) {
            soundPool.play(gameOverSoundId, 1, 1, 1, 0, 1f);
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
