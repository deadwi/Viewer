package net.deadwi.viewer;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class OptionInfoFragment extends Fragment
{
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.activity_option_info, container, false);

        ((TextView)view.findViewById(R.id.textViewVersion)).setText("Fast Viewer "+BuildConfig.VERSION_NAME+" By deadwi");
        return view;
    }
}
