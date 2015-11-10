package de.spas.silverball;

import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by uwe on 23.09.13.
 */
public class GameEngine implements SensorEventListener, Runnable {

    private static final float BOUNCE_FACTOR = 0.25f;
    private static final long MS_PER_FRAME = 25;
    private static final long POINT_LOSS_PER_SECOND = 100;


    interface OnBallInHoleListener {
        void onBallInHole(int score);
    }
    interface OnGameOverListener {
        void onGameOver();
    }

    private float ACCELERATION_SCALE=400f;
    private float ballX, ballY;
    private float ballVX, ballVY;
    private float ballAX, ballAY;
    private float holeX, holeY;
    private float holeRadius;
    private float minX, minY, maxX, maxY;
    private IGameView gameView;
    private SensorManager sensorManager;
    private ScheduledExecutorService service;
    private Handler handler = new Handler();
    private OnBallInHoleListener onBallInHoleListener;
    private OnGameOverListener onGameOverListener;
    private int pointsStart;
    private int points;
    private int time;
    private long deadline;
    private List<RectF> obstacles = new ArrayList<RectF>();

    public GameEngine(SensorManager sensorManager, IGameView gameView, OnBallInHoleListener onBallInHoleListener, OnGameOverListener onGameOverListener) {
        this.sensorManager = sensorManager;
        this.gameView = gameView;
        this.onBallInHoleListener = onBallInHoleListener;
        this.onGameOverListener = onGameOverListener;
        this.gameView.clearObstacles();
    }

    public void setRegion(float minX,float minY, float maxX, float maxY) {
        this.minX=minX;
        this.minY=minY;
        this.maxX=maxX;
        this.maxY=maxY;
    }

    public void setPointsStart(int pointsStart) {
        this.pointsStart = pointsStart;
    }

    public void setTime(int time) {
        this.time = time;
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
        points=pointsStart;
        deadline = System.currentTimeMillis()+time*1000;
        gameView.setBallPosition(ballX, ballY);
        gameView.setHolePosition(holeX,holeY);
        service.scheduleAtFixedRate(this,MS_PER_FRAME,MS_PER_FRAME, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        service.shutdown();
        sensorManager.unregisterListener(this);
        obstacles.clear();
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
        gameView.setCountdown((int) ((deadline-System.currentTimeMillis())/1000));

        if(ballX<minX) { ballX=minX; ballVX= -ballVX*BOUNCE_FACTOR;}
        if(ballY<minY) { ballY=minY; ballVY= -ballVY*BOUNCE_FACTOR;}
        if(ballX>maxX) { ballX=maxX; ballVX= -ballVX*BOUNCE_FACTOR;}
        if(ballY>maxY) { ballY=maxY; ballVY= -ballVY*BOUNCE_FACTOR;}

        float br = gameView.getBaseDimension()/2;
        for(RectF r : obstacles) {
            // left
            if(ballVX>0 && ballX>r.left-br && ballX<r.right+br && ballY>=r.top && ballY<=r.bottom) { ballX=r.left-br; ballVX= -ballVX*BOUNCE_FACTOR;}
            // right
            if(ballVX<0 && ballX>r.left-br && ballX<r.right+br && ballY>=r.top && ballY<=r.bottom) { ballX=r.right+br; ballVX= -ballVX*BOUNCE_FACTOR;}
            // top
            if(ballVY>0 && ballY>r.top-br && ballY<r.bottom+br && ballX>=r.left && ballX<=r.right) { ballY=r.top-br; ballVY= -ballVY*BOUNCE_FACTOR;}
            //bottom
            if(ballVY<0 && ballY>r.top-br && ballY<r.bottom+br && ballX>=r.left && ballX<=r.right) { ballY=r.bottom+br; ballVY= -ballVY*BOUNCE_FACTOR;}
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                gameView.setBallPosition(ballX,ballY);
            }
        });

        if(Math.sqrt((ballX-holeX)*(ballX-holeX) + (ballY-holeY)*(ballY-holeY)) < holeRadius ) {
            stop();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onBallInHoleListener.onBallInHole(points);
                }
            });
        }

        Log.d(getClass().getSimpleName(), Integer.toString(gameView.getFps()) + " fps");

    }

    public void addObstacle(String type, float x, float y, float w, float h) {
        RectF r = new RectF(x, y, x + w, y + h);
        obstacles.add(r);
        if("wood1".equals(type)) gameView.addObstacle1Rect(r);
        if("wood2".equals(type)) gameView.addObstacle2Rect(r);
    }

}
