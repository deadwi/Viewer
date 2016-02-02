package net.deadwi.viewer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;

/**
 * Created by jihun.jo on 2015-12-31.
 */
public class OptionBasicFragment extends Fragment
{
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.activity_option, container, false);
        setUIFromOption(view);
        return view;
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

        switch (Option.getInstance().getVolumeButtonOption())
        {
            case Option.VOLUME_BUTTON_PREV_NEXT:
                ((RadioButton) view.findViewById(R.id.radioButtonVolButtonPN)).setChecked(true);
                break;
            case Option.VOLUME_BUTTON_NEXT_PREV:
                ((RadioButton) view.findViewById(R.id.radioButtonVolButtonNP)).setChecked(true);
                break;
            case Option.VOLUME_BUTTON_DISABLE:
            default:
                ((RadioButton) view.findViewById(R.id.radioButtonVolButtonD)).setChecked(true);
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

        switch (Option.getInstance().getFooterOption())
        {
            case Option.FOOTER_PAGE:
                ((RadioButton) view.findViewById(R.id.radioButtonFooterP)).setChecked(true);
                break;
            case Option.FOOTER_TITLE_PAGE:
                ((RadioButton) view.findViewById(R.id.radioButtonFooterTP)).setChecked(true);
                break;
            case Option.FOOTER_NONE:
            default:
                ((RadioButton) view.findViewById(R.id.radioButtonFooterN)).setChecked(true);
                break;
        }

        switch (Option.getInstance().getEinkCleanOption())
        {
            case 1:
                ((RadioButton) view.findViewById(R.id.radioButtonEink1)).setChecked(true);
                break;
            case 5:
                ((RadioButton) view.findViewById(R.id.radioButtonEink5)).setChecked(true);
                break;
            case 10:
                ((RadioButton) view.findViewById(R.id.radioButtonEink10)).setChecked(true);
                break;
            case 0:
            default:
                ((RadioButton) view.findViewById(R.id.radioButtonEink0)).setChecked(true);
                break;
        }

        final EditText editHome = (EditText)view.findViewById(R.id.editTextHome);
        editHome.setText(Option.getInstance().getHomePath());
        view.findViewById(R.id.buttonFileHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileChooser fileChooser = new FileChooser(view.getContext()).setFileListener(new FileChooser.FileSelectedListener() {
                    @Override public void fileSelected(final File file) {
                        editHome.setText(file.getAbsolutePath());
                    }});
                fileChooser.showDialog();
            }
        });

        final EditText editDown = (EditText)view.findViewById(R.id.editTextDownload);
        editDown.setText(Option.getInstance().getDownloadPath());
        view.findViewById(R.id.buttonFileDown).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileChooser fileChooser = new FileChooser(view.getContext()).setFileListener(new FileChooser.FileSelectedListener() {
                    @Override public void fileSelected(final File file) {
                        editDown.setText(file.getAbsolutePath());
                    }});
                fileChooser.showDialog();
            }
        });
    }

    public void setOptionFromUI(View view)
    {
        Log.d("OPTION","Set basic option");
        Option.getInstance().setPortrait(((RadioButton) view.findViewById(R.id.radioButtonPortraitP)).isChecked());
        Option.getInstance().setReadDirection(((RadioButton) view.findViewById(R.id.radioButtonReadL)).isChecked());

        if(((RadioButton) view.findViewById(R.id.radioButtonTouchPN)).isChecked())
            Option.getInstance().setTouchOption(Option.TOUCH_PREV_NEXT);
        else if(((RadioButton) view.findViewById(R.id.radioButtonTouchNP)).isChecked())
            Option.getInstance().setTouchOption(Option.TOUCH_NEXT_PREV);
        else
            Option.getInstance().setTouchOption(Option.TOUCH_AUTO);

        if(((RadioButton) view.findViewById(R.id.radioButtonVolButtonPN)).isChecked())
            Option.getInstance().setVolumeButtonOption(Option.VOLUME_BUTTON_PREV_NEXT);
        else if(((RadioButton) view.findViewById(R.id.radioButtonVolButtonNP)).isChecked())
            Option.getInstance().setVolumeButtonOption(Option.VOLUME_BUTTON_NEXT_PREV);
        else
            Option.getInstance().setVolumeButtonOption(Option.VOLUME_BUTTON_DISABLE);

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

        if(((RadioButton) view.findViewById(R.id.radioButtonFooterP)).isChecked())
            Option.getInstance().setFooterOption(Option.FOOTER_PAGE);
        else if(((RadioButton) view.findViewById(R.id.radioButtonFooterTP)).isChecked())
            Option.getInstance().setFooterOption(Option.FOOTER_TITLE_PAGE);
        else
            Option.getInstance().setFooterOption(Option.FOOTER_NONE);

        if(((RadioButton) view.findViewById(R.id.radioButtonEink1)).isChecked())
            Option.getInstance().setEinkCleanOption(1);
        else if(((RadioButton) view.findViewById(R.id.radioButtonEink5)).isChecked())
            Option.getInstance().setEinkCleanOption(5);
        else if(((RadioButton) view.findViewById(R.id.radioButtonEink10)).isChecked())
            Option.getInstance().setEinkCleanOption(10);
        else
            Option.getInstance().setEinkCleanOption(0);

        Option.getInstance().setHomePath(((EditText) view.findViewById(R.id.editTextHome)).getText().toString());
        Option.getInstance().setDownloadPath(((EditText) view.findViewById(R.id.editTextDownload)).getText().toString());
    }
}
