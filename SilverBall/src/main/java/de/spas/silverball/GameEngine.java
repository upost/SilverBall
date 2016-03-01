package de.spas.silverball;

import android.content.Context;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.view.SoundEffectConstants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.spas.silverball.model.CollisionResult;
import de.spas.silverball.model.Level;
import de.spas.silverball.model.Obstacle;


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

    public GameEngine(SensorManager sensorManager, GameTextureView gameView, OnBallInHoleListener onBallInHoleListener, OnGameOverListener onGameOverListener, Level level) {
        this.sensorManager = sensorManager;
        this.gameView = gameView;
        this.onBallInHoleListener = onBallInHoleListener;
        this.onGameOverListener = onGameOverListener;
        this.level = level;
        audioManager = (AudioManager) ((gameView.getContext().getSystemService(Context.AUDIO_SERVICE)));
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

        float bd = gameView.getBaseDimension();
        setRegion(bd /2, bd /2,gameView.getWidth()- bd /2,gameView.getHeight()- bd /2);
        hd = gameView.getHorizontalBaseDimension();
        vd = gameView.getVerticalBaseDimension();
        setBallPosition(level.getBall().getStartx() * hd, level.getBall().getStarty() * vd);
        setHolePosition(level.getHole().getX() * hd, level.getHole().getY() * vd, bd / 2);
        points = pointsStart = level.getPoints();
        gameView.setObstacles(level.getObstacles());
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

        float lastBallX = ballX;
        float lastBallY = ballY;

        ballVX+=ballAX*MS_PER_FRAME/1000;
        ballVY+=ballAY*MS_PER_FRAME/1000;
        ballX+=ballVX*MS_PER_FRAME/1000;
        ballY+=ballVY*MS_PER_FRAME/1000;

        points = Math.round((deadline-System.currentTimeMillis()) * pointsStart / time / 1000 );
        gameView.setPoints(points);
        //gameView.setCountdown((int) ((deadline-System.currentTimeMillis())/1000));

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

        float ballRadius = gameView.getBaseDimension()/2;

        for(Obstacle o : level.getObstacles()) {

            collisionRect.set(o.getX()*hd, o.getY()*vd,(o.getX()+o.getW())*hd-1,(o.getY()+o.getH())*vd-1);

            CollisionResult cr = checkCollision(collisionRect, ballX, ballY, ballVX, ballVY, ballRadius);
            if(cr.isCollided()) {
                Log.d(LOG_TAG,"collision: " + ballVX+"/"+ballVY + " → " + cr );
                if(Math.abs(ballVX-cr.getVx())>HALT_BALL_THRESHOLD)
                    ballX=lastBallX;
                if(Math.abs(ballVY - cr.getVy())>HALT_BALL_THRESHOLD)
                    ballY=lastBallY;
                double angleBefore = Math.atan2(ballVY,ballVX);
                double angleAfter = Math.atan2(cr.getVy(),cr.getVx());
                ballVX=cr.getVx();
                ballVY=cr.getVy();
                if(Math.abs(angleBefore-angleAfter)>BOUNCE_ANGLE_SOUND_THRESHOLD)
                    playBounceSound();
            }
        }

        if(Math.sqrt((ballX-holeX)*(ballX-holeX) + (ballY-holeY)*(ballY-holeY)) < holeRadius ) {
            stop();
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

    private CollisionResult checkCollision(RectF rect, float centerX, float centerY, float vx, float vy, float radius) {
        // quadrants
        float dx = Math.abs(centerX - rect.centerX());
        float dy = Math.abs(centerY - rect.centerY());

        // too far away for any collision
        if(dx > rect.width()/2+radius) return CollisionResult.NO_COLLISION;
        if(dy > rect.height()/2+radius)  return CollisionResult.NO_COLLISION;

        // side collision
        if(dx <= rect.width()/2) return new CollisionResult(vx,-vy*BOUNCE_FACTOR);
        if(dy <= rect.height()/2) return new CollisionResult(-vx*BOUNCE_FACTOR,vy);

        // corner collision
        float cornerDistance_sq = (float) (Math.pow(dx - rect.width()/2,2) + Math.pow(dy - rect.height()/2,2));
        if(cornerDistance_sq<=radius*radius) {
            // calculate new direction
            // dist corner->center
            float cdx=Math.abs(dx-rect.width()/2)*Math.signum(centerX-rect.centerX());
            float cdy=-Math.abs(dy-rect.height()/2)*Math.signum(centerY-rect.centerY());
            // calc angles
            // gamma is rotation of line from corner to ball center
            double gamma = Math.atan2(cdy,cdx) * 180/Math.PI;
            // rho is direction of velocity
            double rho = Math.atan2(-vy,vx)* 180/Math.PI;
            // delta is the angle between direction and line from corner to ball center
            double delta = Math.abs(90+gamma-rho);
            // alpha is the rotation to be applied
            double alpha = 2*delta;
            // apply the rotation matrix to the velocity
            float newVx = (float) (Math.cos(alpha*Math.PI/180)*vx - Math.sin(alpha*Math.PI/180)*vy) ;
            float newVy = (float) (Math.sin(alpha*Math.PI/180)*vx + Math.cos(alpha*Math.PI/180)*vy) ;

            return new CollisionResult(newVx,newVy);
        }
        return CollisionResult.NO_COLLISION;
    }

    private void playBounceSound() {
        audioManager.playSoundEffect(SoundEffectConstants.CLICK);
    }

    private void hitDeadlyObstacle() {
        stop();
        handler.post(new Runnable() {
            @Override
            public void run() {
                onGameOverListener.onGameOver();
            }
        });
    }


}
