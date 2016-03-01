package de.spas.silverball;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.spas.silverball.model.Obstacle;

/**
 * Created by uwe on 01.03.16.
 */
public class GameTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private final static float SIZE = 32;
    private static final long FRAME_INTERVAL = 15;
    private float ballX,ballY;
    private float holeX,holeY;
    private float scale;
    private int countdown;
    private int points;
    private int totalPoints;
    private BitmapDrawable ball;
    private Map<String,Bitmap> bitmaps = new HashMap<>();
    private Paint paintBitmap = new Paint();
    private Paint paintHole = new Paint();
    private Paint paintText = new Paint();
    private RectF drawRect = new RectF();
    private Rect ballRect = new Rect();
    private Rect rect = new Rect();
    private List<Obstacle> obstacles = new ArrayList<Obstacle>();
    private ScheduledExecutorService executorService;
    private long t;
    private long frames;

    public GameTextureView(Context context) {
        super(context);
        scale = getResources().getDisplayMetrics().density;
        ball = (BitmapDrawable) getResources().getDrawable(R.drawable.ball);
        paintBitmap.setAntiAlias(true);
        paintHole.setColor(Color.BLACK);
        paintHole.setAntiAlias(true);
        paintHole.setStyle(Paint.Style.FILL);
        paintText.setAntiAlias(true);
        paintText.setColor(Color.argb(100, 255, 255, 255));
        paintText.setTextSize(scale * 30);
        paintText.setStyle(Paint.Style.FILL);
        ballRect.set(0, 0, ball.getBitmap().getWidth(), ball.getBitmap().getHeight());

        setOpaque(false);
        setSurfaceTextureListener(this);
        setFocusable(false);
        setWillNotDraw(false);
    }

    public void setBallPosition(float x, float y) {
        ballX = x;
        ballY = y;
    }

    public void setHolePosition(float x, float y) {
        holeX = x;
        holeY = y;
    }

    public void clearObstacles() {
        obstacles.clear();
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public float getBaseDimension() {
        return scale*SIZE;
    }

    public void setTypeface(Typeface typeface) {
        paintText.setTypeface(typeface);
    }

    protected void doDraw(Canvas canvas) {
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        canvas.drawCircle(holeX, holeY, SIZE*scale/2, paintHole);
        drawRect.set(ballX - SIZE * scale / 2, ballY - SIZE * scale / 2, ballX + SIZE * scale / 2, ballY + SIZE * scale / 2);
        canvas.drawBitmap(ball.getBitmap(), ballRect, drawRect, paintBitmap);
        for(Obstacle o : obstacles) {
            Bitmap bitmap = findCachedBitmap(o.getTexture());
            rect.set(0,0,bitmap.getWidth()-1, bitmap.getHeight()-1);
            drawRect.set(o.getX()*getHorizontalBaseDimension(), o.getY()*getVerticalBaseDimension(),
                    (o.getX()+o.getW())*getHorizontalBaseDimension()-1,
                    (o.getY()+o.getH())*getVerticalBaseDimension()-1);
            canvas.drawBitmap(bitmap, rect, drawRect, paintBitmap);
        }
        canvas.drawText(Integer.toString(points),10*scale,canvas.getHeight()-30*scale, paintText);
        canvas.drawText(Integer.toString(totalPoints),10*scale,40*scale, paintText);
        canvas.drawText(Integer.toString(countdown),canvas.getWidth()-30*scale,canvas.getHeight()-30*scale, paintText);
        frames++;
    }

    private Bitmap findCachedBitmap(String texture) {
        Bitmap b = bitmaps.get(texture);
        if(b!=null) return  b;
        BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(getResources().getIdentifier(texture, "drawable", getContext().getPackageName()));
        if(bd!=null) {
            bitmaps.put(texture,bd.getBitmap());
            return bd.getBitmap();
        }
        else Log.d("GameView", "texture not found: " + texture);
        return null;
    }


    public int getFps() {
        long delta = System.currentTimeMillis() - t;
        if(delta<1000) return 0;
        return (int) (frames/(delta /1000));
    }

    public float getHorizontalBaseDimension() {
        return getWidth()/16;
    }

    public float getVerticalBaseDimension() {
        return getHeight()/9;
    }

    private Runnable renderer = new Runnable() {
        @Override
        public void run() {
            Canvas canvas=null;
            try {
                canvas = lockCanvas();
                doDraw(canvas);
            }
            finally {
                unlockCanvasAndPost(canvas);
            }
        }
    };

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        executorService = Executors.newSingleThreadScheduledExecutor();
        t= System.currentTimeMillis();
        executorService.scheduleAtFixedRate(renderer, FRAME_INTERVAL, FRAME_INTERVAL, TimeUnit.MILLISECONDS);
        Log.d(getClass().getSimpleName(), "onSurfaceTextureAvailable");
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        executorService.shutdown();
        Log.d(getClass().getSimpleName(), "onSurfaceTextureDestroyed");
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    public void setObstacles(List<Obstacle> obstacles) {
        this.obstacles = obstacles;
    }

}
