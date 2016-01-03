package net.deadwi.viewer;

import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
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
public class OptionImageFragment extends Fragment
{
    protected static final int BRIGHTNESS_MIN_VALUE=-100;
    protected static final int BRIGHTNESS_MAX_VALUE=100;
    protected static final int CONTRAST_MIN_VALUE=-100;
    protected static final int CONTRAST_MAX_VALUE=100;
    protected static final int GRAY_MIN_VALUE=100;
    protected static final int GRAY_MAX_VALUE=255;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.activity_option_image, container, false);

        setUIFromOption(view);
        view.findViewById(R.id.checkBoxBrightness).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SeekBar) getView().findViewById(R.id.seekBrightness)).setEnabled(((CheckBox) getView().findViewById(R.id.checkBoxBrightness)).isChecked());
            }
        });
        ((SeekBar)view.findViewById(R.id.seekBrightness)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setSeekColorValue(getView(), R.id.seekBrightness, R.id.textBrightnessValue, progress, BRIGHTNESS_MAX_VALUE - BRIGHTNESS_MIN_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        view.findViewById(R.id.checkBoxContrast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SeekBar) getView().findViewById(R.id.seekContrast)).setEnabled(((CheckBox) getView().findViewById(R.id.checkBoxContrast)).isChecked());
            }
        });
        ((SeekBar)view.findViewById(R.id.seekContrast)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setSeekColorValue(getView(), R.id.seekContrast, R.id.textContrastValue, progress, CONTRAST_MAX_VALUE - CONTRAST_MIN_VALUE);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        view.findViewById(R.id.checkBoxGray).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SeekBar) getView().findViewById(R.id.seekGray)).setEnabled(((CheckBox) getView().findViewById(R.id.checkBoxGray)).isChecked());
            }
        });
        ((SeekBar)view.findViewById(R.id.seekGray)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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

    private void setSeekColorValue(View view, int seekId, int textId, int v, int max)
    {
        ((SeekBar)view.findViewById(seekId)).setMax(max);
        ((SeekBar)view.findViewById(seekId)).setProgress(v);
        ((TextView)view.findViewById(textId)).setText("" + (v+BRIGHTNESS_MIN_VALUE));
    }

    private void setUIFromOption(View view)
    {
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
        int v;

        if(Option.getInstance().IsEnableColorBrightness())
        {
            ((SeekBar)view.findViewById(R.id.seekBrightness)).setEnabled(true);
            ((CheckBox)view.findViewById(R.id.checkBoxBrightness)).setChecked(true);
        }
        else
        {
            ((SeekBar)view.findViewById(R.id.seekBrightness)).setEnabled(false);
            ((CheckBox)view.findViewById(R.id.checkBoxBrightness)).setChecked(false);
        }
        v = Option.getInstance().getColorBrightnessValue();
        if(v<BRIGHTNESS_MIN_VALUE)
            v = 0;
        setSeekColorValue(view, R.id.seekBrightness, R.id.textBrightnessValue, v - BRIGHTNESS_MIN_VALUE, BRIGHTNESS_MAX_VALUE - BRIGHTNESS_MIN_VALUE);

        if(Option.getInstance().IsEnableColorContrast())
        {
            ((SeekBar)view.findViewById(R.id.seekContrast)).setEnabled(true);
            ((CheckBox)view.findViewById(R.id.checkBoxContrast)).setChecked(true);
        }
        else
        {
            ((SeekBar)view.findViewById(R.id.seekContrast)).setEnabled(false);
            ((CheckBox)view.findViewById(R.id.checkBoxContrast)).setChecked(false);
        }
        v = Option.getInstance().getColorContrastValue();
        if(v<CONTRAST_MIN_VALUE)
            v = 0;
        setSeekColorValue(view, R.id.seekContrast, R.id.textContrastValue, v - CONTRAST_MIN_VALUE, CONTRAST_MAX_VALUE - CONTRAST_MIN_VALUE);

        if(Option.getInstance().IsEnableColorInvert())
        {
            ((CheckBox)view.findViewById(R.id.checkBoxInvert)).setChecked(true);
        }
        else
        {
            ((CheckBox)view.findViewById(R.id.checkBoxInvert)).setChecked(false);
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
        v = Option.getInstance().getFilterGrayThreshold();
        if(v<GRAY_MIN_VALUE)
            v = GRAY_MAX_VALUE;
        setGraySeek(view,v - GRAY_MIN_VALUE, GRAY_MAX_VALUE - GRAY_MIN_VALUE);
    }

    public void setOptionFromUI(View view)
    {
        Log.d("OPTION","Set image option");
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

        if(((CheckBox)view.findViewById(R.id.checkBoxBrightness)).isChecked())
        {
            Option.getInstance().setEnableColorBrightness(true);
            Option.getInstance().setColorBrightnessValue(BRIGHTNESS_MIN_VALUE + ((SeekBar) view.findViewById(R.id.seekBrightness)).getProgress());
        }
        else
        {
            Option.getInstance().setEnableColorBrightness(false);
        }

        if(((CheckBox)view.findViewById(R.id.checkBoxContrast)).isChecked())
        {
            Option.getInstance().setEnableColorContrast(true);
            Option.getInstance().setColorContrastValue(CONTRAST_MIN_VALUE + ((SeekBar) view.findViewById(R.id.seekContrast)).getProgress());
        }
        else
        {
            Option.getInstance().setEnableColorContrast(false);
        }

        if(((CheckBox)view.findViewById(R.id.checkBoxInvert)).isChecked())
        {
            Option.getInstance().setEnableColorInvert(true);
        }
        else
        {
            Option.getInstance().setEnableColorInvert(false);
        }

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
