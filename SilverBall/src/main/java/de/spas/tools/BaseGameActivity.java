package de.spas.tools;

import android.app.Activity;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
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
    private Map<String,Shader> shaders = new HashMap<String,Shader>();
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

    protected void addShader(String name, Shader shader) {
        shaders.put(name,shader);
    }


    protected void applyTypeface(TextView v, String typefaceName) {
        Typeface t = getTypeface(typefaceName);
        if(t!=null) {
            v.setTypeface(t);
        }
    }

    protected void applyShader(TextView textView, String shaderName) {
        textView.getPaint().setShader(shaders.get(shaderName));
    }

    protected void applyOutline(TextView textView, float width) {
        textView.getPaint().setStyle(Paint.Style.STROKE);
        textView.getPaint().setStrokeWidth(scale(width));
    }

    protected void applyEmboss(TextView textView, float[] direction, float ambient,
            float specular, float blurRadius) {
        if (Build.VERSION.SDK_INT >= 11) {
            textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        EmbossMaskFilter filter = new EmbossMaskFilter(direction, ambient, specular, blurRadius);
        textView.getPaint().setMaskFilter(filter);
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
