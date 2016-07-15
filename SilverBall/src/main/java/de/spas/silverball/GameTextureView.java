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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.spas.silverball.model.Trap;

/**
 * Created by uwe on 01.03.16.
 */
public class GameTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private final static float SIZE = 32;
    private static final long FRAME_INTERVAL = 15;
    private float ballX,ballY;
    private float holeX,holeY;
    private float scale;
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
    private Collection<Trap> traps = new ArrayList<Trap>();
    private ScheduledExecutorService executorService;
    private long t;
    private long frames;
    private String level;

    public GameTextureView(Context context) {
        super(context);
        scale = getResources().getDisplayMetrics().density;
        ball = (BitmapDrawable) getResources().getDrawable(R.drawable.ball);
        paintBitmap.setAntiAlias(true);
        paintHole.setColor(Color.BLACK);
        paintHole.setAntiAlias(true);
        paintHole.setStyle(Paint.Style.FILL);
        paintText.setAntiAlias(true);
        paintText.setColor(Color.argb(200, 255, 255, 255));
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
        // clear background
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        // hole
        canvas.drawCircle(holeX, holeY, SIZE * scale / 2, paintHole);

        // traps
        for(Trap t : traps) {
            Bitmap bitmap = findCachedBitmap(t.getTexture());
            rect.set(0,0,bitmap.getWidth()-1, bitmap.getHeight()-1);
            drawRect.set(t.getX()*getHorizontalBaseDimension(), t.getY()*getVerticalBaseDimension(),
                    (t.getX()+t.getW())*getHorizontalBaseDimension()-1,
                    (t.getY()+t.getH())*getVerticalBaseDimension()-1);
            canvas.drawBitmap(bitmap, rect, drawRect, paintBitmap);
        }

        // ball
        drawRect.set(ballX - SIZE * scale / 2, ballY - SIZE * scale / 2, ballX + SIZE * scale / 2, ballY + SIZE * scale / 2);
        canvas.drawBitmap(ball.getBitmap(), ballRect, drawRect, paintBitmap);

        // score
        canvas.drawText(Integer.toString(totalPoints),10*scale, 40*scale, paintText);
        canvas.drawText(Integer.toString(points),canvas.getWidth()-100*scale,40*scale, paintText);
        canvas.drawText("Level " + level, 10*scale,canvas.getHeight()-10*scale, paintText);
        //canvas.drawText(Integer.toString(countdown),canvas.getWidth()-30*scale,canvas.getHeight()-30*scale, paintText);
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
        else Log.e("GameView", "texture not found: " + texture);
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

    public void setTraps(Collection<Trap> traps) {
        this.traps = traps;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
