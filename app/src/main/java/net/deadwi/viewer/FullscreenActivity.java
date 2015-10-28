package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity
{
    private FileManager fileManager;
    private ListView fileListView;
    private CustomAdapter fileListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        fileManager = new FileManager();
        fileManager.setShowHiddenFiles(true);
        fileManager.setSortType(FileManager.SORT_ALPHA);

        fileListAdapter = new CustomAdapter(fileManager);
        fileListView = (ListView) findViewById(R.id.listView);
        fileListView.setAdapter(fileListAdapter);
        fileListView.setOnItemClickListener(onClickListItem);

        fileListAdapter.updateFileList();

        /*
        mContentView = findViewById(R.id.fullscreen_content);
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //toggle();
            }
        });
        */
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        hide();
    }

    private void hide()
    {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private OnItemClickListener onClickListItem = new OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
        {
            // 이벤트 발생 시 해당 아이템 위치의 텍스트를 출력
            //Toast.makeText(getApplicationContext(), fileListAdapter.getItem(arg2), Toast.LENGTH_SHORT).show();
        }
    };
}
