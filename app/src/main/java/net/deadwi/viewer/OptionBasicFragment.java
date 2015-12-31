package net.deadwi.viewer;

import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by jihun.jo on 2015-12-31.
 */
public class OptionBasicFragment extends Fragment
{
    protected static final int GRAY_MIN_VALUE=100;
    protected static final int GRAY_MAX_VALUE=255;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.activity_option, container, false);

        setUIFromOption(view);
        view.findViewById(R.id.checkBoxGray).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SeekBar) getView().findViewById(R.id.seekGray)).setEnabled(((CheckBox) getView().findViewById(R.id.checkBoxGray)).isChecked());
            }
        });
        ((SeekBar)view.findViewById(R.id.seekGray)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //currntFileIndex = progress;
                setGraySeek(getView(), progress, GRAY_MAX_VALUE - GRAY_MIN_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        return view;
    }

    private void setGraySeek(View view, int v,int max)
    {
        ((SeekBar)view.findViewById(R.id.seekGray)).setMax(max);
        ((SeekBar)view.findViewById(R.id.seekGray)).setProgress(v);
        if(v==max)
            ((TextView)view.findViewById(R.id.textGrayThreshold)).setText("Auto");
        else
            ((TextView)view.findViewById(R.id.textGrayThreshold)).setText("" + v);
    }

    private void setUIFromOption(View view)
    {
        if(Option.getInstance().IsPortrait())
            ((RadioButton) view.findViewById(R.id.radioButtonPortraitP)).setChecked(true);
        else
            ((RadioButton) view.findViewById(R.id.radioButtonPortraitL)).setChecked(true);

        if(Option.getInstance().IsReadLeftToRight())
            ((RadioButton) view.findViewById(R.id.radioButtonReadL)).setChecked(true);
        else
            ((RadioButton) view.findViewById(R.id.radioButtonReadR)).setChecked(true);

        switch (Option.getInstance().getTouchOption())
        {
            case Option.TOUCH_PREV_NEXT:
                ((RadioButton) view.findViewById(R.id.radioButtonTouchPN)).setChecked(true);
                break;
            case Option.TOUCH_NEXT_PREV:
                ((RadioButton) view.findViewById(R.id.radioButtonTouchNP)).setChecked(true);
                break;
            case Option.TOUCH_AUTO:
            default:
                ((RadioButton) view.findViewById(R.id.radioButtonTouchA)).setChecked(true);
                break;
        }

        switch (Option.getInstance().getSplitOption())
        {
            case Option.SPLIT_SINGLE:
                ((RadioButton) view.findViewById(R.id.radioButtonSplitS)).setChecked(true);
                break;
            case Option.SPLIT_DOUBLE:
                ((RadioButton) view.findViewById(R.id.radioButtonSplitD)).setChecked(true);
                break;
            case Option.SPLIT_AUTO:
            default:
                ((RadioButton) view.findViewById(R.id.radioButtonSplitA)).setChecked(true);
                break;
        }

        switch (Option.getInstance().getDisplayOption())
        {
            case Option.DISPLAY_WIDTH:
                ((RadioButton) view.findViewById(R.id.radioButtonDisplayW)).setChecked(true);
                break;
            case Option.DISPLAY_HEIGHT:
                ((RadioButton) view.findViewById(R.id.radioButtonDisplayH)).setChecked(true);
                break;
            case Option.DISPLAY_FIT:
            default:
                ((RadioButton) view.findViewById(R.id.radioButtonDisplayF)).setChecked(true);
                break;
        }

        switch (Option.getInstance().getResizeMethodOption())
        {
            case Option.RESIZE_METHOD_BOX:
                ((RadioButton) view.findViewById(R.id.radioMethod1)).setChecked(true);
                break;
            case Option.RESIZE_METHOD_BSPLINE:
                ((RadioButton) view.findViewById(R.id.radioMethod3)).setChecked(true);
                break;
            case Option.RESIZE_METHOD_CATMULLROM:
                ((RadioButton) view.findViewById(R.id.radioMethod4)).setChecked(true);
                break;
            case Option.RESIZE_METHOD_LANCZOS3:
                ((RadioButton) view.findViewById(R.id.radioMethod5)).setChecked(true);
                break;
            case Option.RESIZE_METHOD_BILINEAR:
            default:
                ((RadioButton) view.findViewById(R.id.radioMethod2)).setChecked(true);
                break;
        }

        if(Option.getInstance().IsEnableFilterGray())
        {
            ((SeekBar)view.findViewById(R.id.seekGray)).setEnabled(true);
            ((CheckBox)view.findViewById(R.id.checkBoxGray)).setChecked(true);
        }
        else
        {
            ((SeekBar)view.findViewById(R.id.seekGray)).setEnabled(false);
            ((CheckBox)view.findViewById(R.id.checkBoxGray)).setChecked(false);
        }
        int v = Option.getInstance().getFilterGrayThreshold();
        if(v<GRAY_MIN_VALUE)
            v = GRAY_MAX_VALUE;
        setGraySeek(view,v - GRAY_MIN_VALUE, GRAY_MAX_VALUE - GRAY_MIN_VALUE);
    }

    public void setOptionFromUI(View view)
    {
        Option.getInstance().setPortrait(((RadioButton) view.findViewById(R.id.radioButtonPortraitP)).isChecked());
        Option.getInstance().setReadDirection(((RadioButton) view.findViewById(R.id.radioButtonReadL)).isChecked());

        if(((RadioButton) view.findViewById(R.id.radioButtonTouchPN)).isChecked())
            Option.getInstance().setTouchOption(Option.TOUCH_PREV_NEXT);
        else if(((RadioButton) view.findViewById(R.id.radioButtonTouchNP)).isChecked())
            Option.getInstance().setTouchOption(Option.TOUCH_NEXT_PREV);
        else
            Option.getInstance().setTouchOption(Option.TOUCH_AUTO);

        if(((RadioButton) view.findViewById(R.id.radioButtonSplitS)).isChecked())
            Option.getInstance().setSplitOption(Option.SPLIT_SINGLE);
        else if(((RadioButton) view.findViewById(R.id.radioButtonSplitS)).isChecked())
            Option.getInstance().setSplitOption(Option.SPLIT_DOUBLE);
        else
            Option.getInstance().setSplitOption(Option.SPLIT_AUTO);

        if(((RadioButton) view.findViewById(R.id.radioButtonDisplayW)).isChecked())
            Option.getInstance().setDisplayOption(Option.DISPLAY_WIDTH);
        else if(((RadioButton) view.findViewById(R.id.radioButtonDisplayH)).isChecked())
            Option.getInstance().setDisplayOption(Option.DISPLAY_HEIGHT);
        else
            Option.getInstance().setDisplayOption(Option.DISPLAY_FIT);

        if(((RadioButton) view.findViewById(R.id.radioMethod1)).isChecked())
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_BOX);
        else if(((RadioButton) view.findViewById(R.id.radioMethod3)).isChecked())
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_BSPLINE);
        else if(((RadioButton) view.findViewById(R.id.radioMethod4)).isChecked())
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_CATMULLROM);
        else if(((RadioButton) view.findViewById(R.id.radioMethod5)).isChecked())
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_LANCZOS3);
        else
            Option.getInstance().setResizeMethodOption(Option.RESIZE_METHOD_BILINEAR);

        if(((CheckBox)view.findViewById(R.id.checkBoxGray)).isChecked())
        {
            Option.getInstance().setEnableFilterGray(true);
            Option.getInstance().setFilterGrayThreshold(GRAY_MIN_VALUE + ((SeekBar) view.findViewById(R.id.seekGray)).getProgress());
        }
        else
        {
            Option.getInstance().setEnableFilterGray(false);
        }
    }
}
