package de.spas.silverball;

import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Created by uwe on 09.12.13.
 */
public interface IGameView {
    void setBallPosition(float x, float y);

    void setHolePosition(float x, float y);

    void clearObstacles();

    void addObstacle1Rect(RectF r);

    void addObstacle2Rect(RectF r);

    void setCountdown(int countdown);

    void setPoints(int points);

    void setTotalPoints(int totalPoints);

    float getBaseDimension();

    void setTypeface(Typeface typeface);

    int getFps();
}
