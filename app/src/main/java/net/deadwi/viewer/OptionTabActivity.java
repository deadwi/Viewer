package net.deadwi.viewer;

import android.content.pm.ActivityInfo;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

public class OptionTabActivity extends AppCompatActivity
{
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option_tab);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem());
                return true;
            }
        });
        mViewPager.setPageTransformer(false, new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View view, float position) {
                if (position < 0) {
                    view.setScrollX((int) ((float) (view.getWidth()) * position));
                } else if (position > 0) {
                    view.setScrollX(-(int) ((float) (view.getWidth()) * -position));
                } else {
                    view.setScrollX(0);
                }
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


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
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        hide();
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            if(position==0)
                return Fragment.instantiate(getBaseContext(), OptionBasicFragment.class.getName());
            else if(position==1)
                return Fragment.instantiate(getBaseContext(), OptionImageFragment.class.getName());
            return Fragment.instantiate(getBaseContext(), OptionInfoFragment.class.getName());
        }

        @Override
        public int getCount()
        {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position) {
                case 0:
                    return "BASIC";
                case 1:
                    return "IMAGE";
                case 2:
                    return "INFO";
            }
            return null;
        }
    }

    private void setOptionFromUI()
    {
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for(Fragment f : fragmentList)
        {
            if(f.isVisible()==false)
                continue;

            if(f instanceof OptionBasicFragment)
            {
                Log.d("OPTION","setOptionFromUI : Basic");
                ((OptionBasicFragment) f).setOptionFromUI(f.getView());
            }
            else if(f instanceof OptionImageFragment)
            {
                Log.d("OPTION","setOptionFromUI : Image");
                ((OptionImageFragment) f).setOptionFromUI(f.getView());
            }
            else if(f instanceof OptionInfoFragment)
            {
                // nothing
            }
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
