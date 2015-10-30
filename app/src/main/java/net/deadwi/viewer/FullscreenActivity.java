package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity
{
    public static final int EVENT_UPDATE_FILE_LIST = 1001;

    private FileManager fileManager;
    private ListView fileListView;
    private CustomAdapter fileListAdapter;

    private final MyHandler handler = new MyHandler(this);
    private static class MyHandler extends Handler
    {
        private final WeakReference<FullscreenActivity> mActivity;
        public MyHandler(FullscreenActivity activity)
        {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg)
        {
            FullscreenActivity activity = mActivity.get();
            if (activity == null)
                return;
            activity.handleMessage(msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        fileManager = new FileManager();
        fileManager.setShowHiddenFiles(true);
        fileManager.setSortType(FileManager.SORT_ALPHA);

        fileListAdapter = new CustomAdapter(fileManager, handler);
        fileListView = (ListView) findViewById(R.id.listView);
        fileListView.setAdapter(fileListAdapter);
        fileListView.setOnItemClickListener(onClickListItem);

        fileListAdapter.updateFileList();

        // 상위 디렉토리
        findViewById(R.id.buttonUpFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                fileManager.movePreviousDir();
                updateFileList();
            }
        });
        // 이전 페이지
        findViewById(R.id.buttonPrevPage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                int visibleChildCount = (fileListView.getLastVisiblePosition() - fileListView.getFirstVisiblePosition()) + 1;
                int prevPos = fileListView.getFirstVisiblePosition()-visibleChildCount+2;
                if(prevPos<0)
                    prevPos = 0;
                fileListView.setSelection(prevPos);
            }
        });
        // 다음 페이지
        findViewById(R.id.buttonNextPage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                fileListView.setSelection(fileListView.getLastVisiblePosition());
            }
        });
        // 첫페이지
        findViewById(R.id.buttonPrevPage).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view)
            {
                fileListView.setSelectionAfterHeaderView();
                return true;
            }
        });
        // 마지막페이지
        findViewById(R.id.buttonNextPage).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view)
            {
                fileListView.setSelection(fileListAdapter.getCount()-1);
                return true;
            }
        });


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

    private void handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case EVENT_UPDATE_FILE_LIST:
                updateFileList();
                break;
        }
    }

    private void hide()
    {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void updateFileList()
    {
        fileListAdapter.updateFileList();
        fileListAdapter.notifyDataSetChanged();
    }

    private OnItemClickListener onClickListItem = new OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
        {
            Log.d("MAIN","item click");
            // 이벤트 발생 시 해당 아이템 위치의 텍스트를 출력
            //Toast.makeText(getApplicationContext(), fileListAdapter.getItem(arg2), Toast.LENGTH_SHORT).show();
        }
    };
}
