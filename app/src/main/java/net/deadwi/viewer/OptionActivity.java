package net.deadwi.viewer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * Created by jihun.jo on 2015-11-18.
 */
public class OptionActivity  extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);

        findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setOptionFromUI();
                finish();
                overridePendingTransition(0, 0);
            }
        });
        findViewById(R.id.buttonCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(0, 0);
            }
        });

        setUIFromOption();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        hide();
    }

    private void setUIFromOption()
    {
        if(Option.getInstance().IsPortrait())
            ((RadioButton) findViewById(R.id.radioButtonPortraitP)).setChecked(true);
        else
            ((RadioButton) findViewById(R.id.radioButtonPortraitL)).setChecked(true);

        if(Option.getInstance().IsReadLeftToRight())
            ((RadioButton) findViewById(R.id.radioButtonReadL)).setChecked(true);
        else
            ((RadioButton) findViewById(R.id.radioButtonReadR)).setChecked(true);

        switch (Option.getInstance().getSplitOption())
        {
            case Option.SPLIT_SINGLE:
                ((RadioButton) findViewById(R.id.radioButtonSplitS)).setChecked(true);
                break;
            case Option.SPLIT_DOUBLE:
                ((RadioButton) findViewById(R.id.radioButtonSplitD)).setChecked(true);
                break;
            case Option.SPLIT_AUTO:
            default:
                ((RadioButton) findViewById(R.id.radioButtonSplitA)).setChecked(true);
                break;
        }

        switch (Option.getInstance().getDisplayOption())
        {
            case Option.DISPLAY_WIDTH:
                ((RadioButton) findViewById(R.id.radioButtonDisplayW)).setChecked(true);
                break;
            case Option.DISPLAY_HEIGHT:
                ((RadioButton) findViewById(R.id.radioButtonDisplayH)).setChecked(true);
                break;
            case Option.DISPLAY_FIT:
            default:
                ((RadioButton) findViewById(R.id.radioButtonDisplayF)).setChecked(true);
                break;
        }

        switch (Option.getInstance().getResizeMethodOption())
        {
            case Option.RESIZE_METHOD_BOX:
                ((RadioButton) findViewById(R.id.radioMethod1)).setChecked(true);
                break;
            case Option.RESIZE_METHOD_BSPLINE:
                ((RadioButton) findViewById(R.id.radioMethod3)).setChecked(true);
                break;
            case Option.RESIZE_METHOD_CATMULLROM:
                ((RadioButton) findViewById(R.id.radioMethod4)).setChecked(true);
                break;
            case Option.RESIZE_METHOD_LANCZOS3:
                ((RadioButton) findViewById(R.id.radioMethod5)).setChecked(true);
                break;
            case Option.RESIZE_METHOD_BILINEAR:
            default:
                ((RadioButton) findViewById(R.id.radioMethod2)).setChecked(true);
                break;
        }
    }

    private void setOptionFromUI()
    {
        Option.getInstance().setPortrait(((RadioButton) findViewById(R.id.radioButtonPortraitP)).isChecked());
        Option.getInstance().setReadDirection(((RadioButton) findViewById(R.id.radioButtonReadL)).isChecked());
        if(((RadioButton) findViewById(R.id.radioButtonSplitS)).isChecked())
            Option.getInstance().setSplitOption(Option.SPLIT_SINGLE);
        else if(((RadioButton) findViewById(R.id.radioButtonSplitS)).isChecked())
            Option.getInstance().setSplitOption(Option.SPLIT_DOUBLE);
        else
            Option.getInstance().setSplitOption(Option.SPLIT_AUTO);

        if(((RadioButton) findViewById(R.id.radioButtonDisplayW)).isChecked())
            Option.getInstance().setDisplayOption(Option.DISPLAY_WIDTH);
        else if(((RadioButton) findViewById(R.id.radioButtonDisplayH)).isChecked())
            Option.getInstance().setDisplayOption(Option.DISPLAY_HEIGHT);
        else
            Option.getInstance().setDisplayOption(Option.DISPLAY_FIT);

        if(((RadioButton) findViewById(R.id.radioMethod1)).isChecked())
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_BOX);
        else if(((RadioButton) findViewById(R.id.radioMethod3)).isChecked())
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_BSPLINE);
        else if(((RadioButton) findViewById(R.id.radioMethod4)).isChecked())
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_CATMULLROM);
        else if(((RadioButton) findViewById(R.id.radioMethod5)).isChecked())
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_LANCZOS3);
        else
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_BILINEAR);

        if(Option.getInstance().IsPortrait())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Option.getInstance().saveOption();
        Option.getInstance().setChanged(true);
    }

    private void hide()
    {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }
}
