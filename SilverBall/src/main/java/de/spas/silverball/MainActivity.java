package de.spas.silverball;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.InputStream;

import de.spas.silverball.model.LevelPack;
import de.spas.tools.BaseGameActivity;
import de.spas.tools.SimpleAnimationListener;

public class MainActivity extends BaseGameActivity implements View.OnClickListener, GameEngine.OnBallInHoleListener, GameEngine.OnGameOverListener {

    private final static String FONTNAME="airmole";
    private GameTextureView gameView;
    private ViewGroup container;
    private GameEngine gameEngine;
    private LevelPack levelPack;
    private int level;
    private int totalPoints;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addTypeface(FONTNAME);
        addShader("silver", new LinearGradient(
                0, 0, 0, scale(16),
                getResources().getColor(R.color.silver1), getResources().getColor(R.color.silver2),
                Shader.TileMode.MIRROR));
        applyTypeface((TextView) findViewById(R.id.start), FONTNAME);
        applyTypeface((TextView) findViewById(R.id.title), FONTNAME);
        applyTypeface((TextView) findViewById(R.id.title_back), FONTNAME);
        applyShader((TextView) findViewById(R.id.title_back), "silver");
        applyOutline((TextView) findViewById(R.id.title), 2);
        applyTypeface((TextView) findViewById(R.id.score), FONTNAME);
        applyShader((TextView) findViewById(R.id.score), "silver");
        applyEmboss((TextView) findViewById(R.id.score), new float[]{0f, scale(1f), scale(0.5f)}, 0.8f, 3f, scale(3f));
        findViewById(R.id.start).setOnClickListener(this);
        container = (ViewGroup) findViewById(R.id.container);
        //gameView = new GameView(this);
        gameView = new GameTextureView(this);
        gameView.setTypeface(getTypeface(FONTNAME));
        container.addView(gameView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        gameOver();
        try {
            InputStream source = getAssets().open("levels.xml");
            Serializer serializer = new Persister();
            levelPack = serializer.read(LevelPack.class, source);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "loading levels threw exception", e);
        }
    }



    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.start) {
            Animation a = AnimationUtils.loadAnimation(this,R.anim.buttonpress);
            a.setAnimationListener(new SimpleAnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    startGame();
                }
            });
            view.startAnimation(a);
        }
    }

    private void startGame() {
        hideView(R.id.menu);
        gameView.setVisibility(View.VISIBLE);
        level=0;
        totalPoints =0;
        gameView.setTotalPoints(totalPoints);
        startLevel();
    }

    private void startLevel() {
        gameEngine = new GameEngine((SensorManager)getSystemService(Context.SENSOR_SERVICE),gameView,this,this,  levelPack.getLevels().get(level));
        gameEngine.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(gameEngine!=null) gameEngine.stop();
    }

    @Override
    public void onBallInHole(int score) {
        totalPoints += score;
        gameView.setTotalPoints(totalPoints);
        gameView.invalidate();
        level++;
        if(levelPack.getLevels().size() > level) {
            startLevel();
        } else {
            gameOver();
        }
    }

    private void gameOver() {
        showView(R.id.menu);
        gameView.setVisibility(View.INVISIBLE);
        setText(R.id.score, getString(R.string.score) + " " + Integer.toString(totalPoints));
    }


    @Override
    public void onGameOver() {
        gameOver();
    }
}
