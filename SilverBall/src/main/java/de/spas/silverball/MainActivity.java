package de.spas.silverball;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;

import de.spas.silverball.model.LevelPack;
import de.spas.tools.BaseGameActivity;
import de.spas.tools.SimpleAnimationListener;

public class MainActivity extends BaseGameActivity implements  GameEngine.OnGameEventListener {

    private final static String FONTNAME="airmole";
    private GameTextureView gameView;
    private GameEngine gameEngine;
    private LevelPack levelPack;
    private int level;
    private int highscore;
    private AudioManager audioManager;
    private int currentScore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // certain effects for the main menu
        setContentView(R.layout.activity_main);
        addTypeface(FONTNAME);
        addShader("silver", new LinearGradient(
                0, 0, 0, scale(16),
                getResources().getColor(R.color.silver1), getResources().getColor(R.color.silver2),
                Shader.TileMode.MIRROR));
        applyTypeface(findViewById(R.id.start), FONTNAME);
        findViewById(R.id.start).setLayerType(View.LAYER_TYPE_SOFTWARE, null); // nice effects need software rendering
        ((TextView) findViewById(R.id.start)).getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        applyTypeface( findViewById(R.id.title), FONTNAME);
        applyTypeface( findViewById(R.id.title_back), FONTNAME);
        applyShader( findViewById(R.id.title_back), "silver");
        applyOutline( findViewById(R.id.title), 2);
        applyTypeface( findViewById(R.id.highscore), FONTNAME);
        applyShader( findViewById(R.id.highscore), "silver");
        applyEmboss( findViewById(R.id.highscore), new float[]{0f, scale(1f), scale(0.5f)}, 0.8f, 3f, scale(3f));
        findViewById(R.id.start).setOnClickListener(this::clickedStart);

        gameView = findViewById(R.id.gameview);
        gameView.setTypeface(getTypeface(FONTNAME));

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        // load levelpack
        try {
            InputStream source = getAssets().open("levels.json");
            Gson gson = new Gson();
            levelPack = gson.fromJson(new InputStreamReader(source),LevelPack.class);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "loading levels threw exception", e);
        }

        showMenu();

    }

    private void clickedStart(View view) {
        Animation a = AnimationUtils.loadAnimation(this,R.anim.buttonpress);
        a.setAnimationListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                startGame();
            }
        });
        view.startAnimation(a);
    }

    private void startGame() {
        hideView(R.id.menu);
        level=0;
        gameView.setTotalPoints(0);
        startLevel();
    }

    private void startLevel() {
        gameEngine = new GameEngine(this, (SensorManager)getSystemService(Context.SENSOR_SERVICE),gameView,this,  levelPack.getLevels().get(level));
        gameEngine.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(gameEngine!=null) gameEngine.stop();
    }

    @Override
    public void onBallInHole(int score) {
        currentScore += score;
        runOnUiThread(this::nextLevel);
    }

    public void nextLevel() {
        gameView.setTotalPoints(currentScore);
        gameView.invalidate();
        level++;
        if(levelPack.getLevels().size() > level) {
            startLevel();
        } else {
            onGameOver();
        }
    }

    @Override
    public void onGameOver() {
        if(currentScore >highscore) highscore= currentScore;
        runOnUiThread(this::showMenu);
    }

    public void showMenu() {
        showView(R.id.menu);
        findViewById(R.id.menu).startAnimation(AnimationUtils.loadAnimation(this,R.anim.scalein));
        setText(R.id.highscore, getString(R.string.highscore) + " " + Integer.toString(highscore));
    }

    // handle volume keys
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                return false;
        }
    }
}
