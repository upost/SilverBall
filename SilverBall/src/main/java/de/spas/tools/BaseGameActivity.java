package de.spas.tools;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by uwe on 19.09.13.
 */
public class BaseGameActivity extends Activity {

    private Map<String,Typeface> typefaces = new HashMap<String,Typeface>();
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
    }

    protected float scale(float v) {
        return  density*v;
    }

    protected void addTypeface(String name) {
        Typeface typeface = Typeface.createFromAsset(getAssets(),name+".ttf");
        typefaces.put(name, typeface);
    }

    protected void setTypeface(TextView v, String typefaceName) {
        Typeface t = getTypeface(typefaceName);
        if(t!=null) {
            v.setTypeface(t);
        }
    }

    protected Typeface getTypeface(String typefaceName) {
        return typefaces.get(typefaceName);
    }

    protected void hideView(int id) {
        findViewById(id).setVisibility(View.GONE);
    }
    protected void showView(int id) {
        findViewById(id).setVisibility(View.VISIBLE);
    }

    protected  void setText(int id, String text) {
        ((TextView)findViewById(id)).setText(text);
    }

}
