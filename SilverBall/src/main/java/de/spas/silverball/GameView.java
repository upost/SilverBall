package de.spas.silverball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by uwe on 21.09.13.
 */
public class GameView extends View implements IGameView {

    private final static float SIZE = 32;
    private float ballX,ballY;
    private float holeX,holeY;
    private float scale;
    private int countdown;
    private int points;
    private int totalPoints;
    private BitmapDrawable ball;
    private BitmapDrawable wood1;
    private BitmapDrawable wood2;
    private Paint paintBitmap = new Paint();
    private Paint paintHole = new Paint();
    private Paint paintText = new Paint();
    private RectF drawRect = new RectF();
    private Rect ballRect = new Rect();
    private Rect wood1Rect = new Rect();
    private Rect wood2Rect = new Rect();
    private List<RectF> obstacles1 = new ArrayList<RectF>();
    private List<RectF> obstacles2 = new ArrayList<RectF>();
    private long t;
    private long frames;


    public GameView(Context context) {
        super(context);
        scale = getResources().getDisplayMetrics().density;
        ball = (BitmapDrawable) getResources().getDrawable(R.drawable.ball);
        wood1 = (BitmapDrawable) getResources().getDrawable(R.drawable.wood1);
        wood2 = (BitmapDrawable) getResources().getDrawable(R.drawable.wood2);
        paintBitmap.setAntiAlias(true);
        paintHole.setColor(Color.BLACK);
        paintHole.setAntiAlias(true);
        paintHole.setStyle(Paint.Style.FILL);
        paintText.setAntiAlias(true);
        paintText.setColor(Color.argb(100, 255, 255, 255));
        paintText.setTextSize(scale * 30);
        paintText.setStyle(Paint.Style.FILL);
        ballRect.set(0, 0, ball.getBitmap().getWidth(), ball.getBitmap().getHeight());
        wood1Rect.set(0, 0, wood1.getBitmap().getWidth(), wood1.getBitmap().getHeight());
        wood2Rect.set(0, 0, wood2.getBitmap().getWidth(), wood2.getBitmap().getHeight());
    }

    @Override
    public void setBallPosition(float x, float y) {
        ballX = x;
        ballY = y;
        invalidate();
    }

    @Override
    public void setHolePosition(float x, float y) {
        holeX = x;
        holeY = y;
        invalidate();
    }

    @Override
    public void clearObstacles() {
        obstacles1.clear();
        obstacles2.clear();
    }

    @Override
    public void addObstacle1Rect(RectF r) {
        obstacles1.add(r);
    }

    @Override
    public void addObstacle2Rect(RectF r) {
        obstacles2.add(r);
    }

    @Override
    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    @Override
    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    @Override
    public float getBaseDimension() {
        return scale*SIZE;
    }

    @Override
    public void setTypeface(Typeface typeface) {
        paintText.setTypeface(typeface);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(t==0) t= System.currentTimeMillis();
        canvas.drawCircle(holeX, holeY, SIZE*scale/2, paintHole);
        drawRect.set(ballX - SIZE * scale / 2, ballY - SIZE * scale / 2, ballX + SIZE * scale / 2, ballY + SIZE * scale / 2);
        canvas.drawBitmap(ball.getBitmap(), ballRect, drawRect, paintBitmap);
        for(RectF r : obstacles1) {
            canvas.drawBitmap(wood1.getBitmap(), wood1Rect, r, paintBitmap);
        }
        for(RectF r : obstacles2) {
            canvas.drawBitmap(wood2.getBitmap(), wood2Rect, r, paintBitmap);
        }
        canvas.drawText(Integer.toString(points),10*scale,canvas.getHeight()-30*scale, paintText);
        canvas.drawText(Integer.toString(totalPoints),10*scale,40*scale, paintText);
        canvas.drawText(Integer.toString(countdown),canvas.getWidth()-30*scale,canvas.getHeight()-30*scale, paintText);
        frames++;
    }

    @Override
    public int getFps() {
        long delta = System.currentTimeMillis() - t;
        if(delta<1000) return 0;
        return (int) (frames/(delta /1000));
    }


}
