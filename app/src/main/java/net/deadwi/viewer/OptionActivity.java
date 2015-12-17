package net.deadwi.viewer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by jihun.jo on 2015-11-18.
 */
public class OptionActivity  extends AppCompatActivity
{
    protected static final int GRAY_MIN_VALUE=100;
    protected static final int GRAY_MAX_VALUE=255;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        findViewById(R.id.checkBoxGray).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SeekBar)findViewById(R.id.seekGray)).setEnabled( ((CheckBox)findViewById(R.id.checkBoxGray)).isChecked() );
            }
        });
        ((SeekBar)findViewById(R.id.seekGray)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //currntFileIndex = progress;
                setGraySeek(progress, GRAY_MAX_VALUE - GRAY_MIN_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
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

    private void setGraySeek(int v,int max)
    {
        ((SeekBar)findViewById(R.id.seekGray)).setMax(max);
        ((SeekBar)findViewById(R.id.seekGray)).setProgress(v);
        if(v==max)
            ((TextView)findViewById(R.id.textGrayThreshold)).setText("Auto");
        else
            ((TextView)findViewById(R.id.textGrayThreshold)).setText("" + v);
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

        switch (Option.getInstance().getTouchOption())
        {
            case Option.TOUCH_PREV_NEXT:
                ((RadioButton) findViewById(R.id.radioButtonTouchPN)).setChecked(true);
                break;
            case Option.TOUCH_NEXT_PREV:
                ((RadioButton) findViewById(R.id.radioButtonTouchNP)).setChecked(true);
                break;
            case Option.TOUCH_AUTO:
            default:
                ((RadioButton) findViewById(R.id.radioButtonTouchA)).setChecked(true);
                break;
        }

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

        if(Option.getInstance().IsEnableFilterGray())
        {
            ((SeekBar)findViewById(R.id.seekGray)).setEnabled(true);
            ((CheckBox)findViewById(R.id.checkBoxGray)).setChecked(true);
        }
        else
        {
            ((SeekBar)findViewById(R.id.seekGray)).setEnabled(false);
            ((CheckBox)findViewById(R.id.checkBoxGray)).setChecked(false);
        }
        int v = Option.getInstance().getFilterGrayThreshold();
        if(v<GRAY_MIN_VALUE)
            v = GRAY_MAX_VALUE;
        setGraySeek(v - GRAY_MIN_VALUE, GRAY_MAX_VALUE - GRAY_MIN_VALUE);
    }

    private void setOptionFromUI()
    {
        Option.getInstance().setPortrait(((RadioButton) findViewById(R.id.radioButtonPortraitP)).isChecked());
        Option.getInstance().setReadDirection(((RadioButton) findViewById(R.id.radioButtonReadL)).isChecked());

        if(((RadioButton) findViewById(R.id.radioButtonTouchPN)).isChecked())
            Option.getInstance().setTouchOption(Option.TOUCH_PREV_NEXT);
        else if(((RadioButton) findViewById(R.id.radioButtonTouchNP)).isChecked())
            Option.getInstance().setTouchOption(Option.TOUCH_NEXT_PREV);
        else
            Option.getInstance().setTouchOption(Option.TOUCH_AUTO);

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

        if(((CheckBox)findViewById(R.id.checkBoxGray)).isChecked())
        {
            Option.getInstance().setEnableFilterGray(true);
            Option.getInstance().setFilterGrayThreshold( GRAY_MIN_VALUE + ((SeekBar)findViewById(R.id.seekGray)).getProgress() );
        }
        else
        {
            Option.getInstance().setEnableFilterGray(false);
        }

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
