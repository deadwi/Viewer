package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import android.content.Intent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity
{
    public static final int EVENT_UPDATE_FILE_LIST = 1001;
    public static final int EVENT_VIEW_FILE = 1002;
    public static final int EVENT_OPEN_FILE = 1003;

    private FileManager fileManager;
    private TextView currentNameTextView;
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
        if(Option.getInstance().IsPortrait())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_fullscreen);

        fileManager = new FileManager();
        fileManager.setShowHiddenFiles(true);
        fileManager.setSortType(FileManager.SORT_ALPHA);

        currentNameTextView = (TextView) findViewById(R.id.textCurrentName);
        fileListAdapter = new CustomAdapter(fileManager, handler);
        fileListView = (ListView) findViewById(R.id.listView);
        fileListView.setAdapter(fileListAdapter);

        refreshFileList(true);

        // 옵션 디렉토리
        findViewById(R.id.buttonOption).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(FullscreenActivity.this, OptionActivity.class);
                startActivity(myIntent);
                overridePendingTransition(0, 0);
            }
        });
        // 홈 디렉토리
        findViewById(R.id.buttonHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileManager.setCurrentDir("/extsd");
                refreshFileList(true);
            }
        });
        // 상위 디렉토리
        findViewById(R.id.buttonUpFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileManager.movePreviousDir();
                refreshFileList(true);
            }
        });
        // 이전 페이지
        findViewById(R.id.buttonPrevPage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();

                int visibleChildCount = (fileListView.getLastVisiblePosition() - fileListView.getFirstVisiblePosition()) + 1;
                int prevPos = fileListView.getFirstVisiblePosition() - visibleChildCount + 2;
                if (prevPos < 0)
                    prevPos = 0;
                fileListView.setSelection(prevPos);
            }
        });
        // 다음 페이지
        findViewById(R.id.buttonNextPage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();
                fileListView.setSelection(fileListView.getLastVisiblePosition());
            }
        });
        // 첫페이지
        findViewById(R.id.buttonPrevPage).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                hideKeyboard();
                fileListView.setSelectionAfterHeaderView();
                return true;
            }
        });
        // 마지막페이지
        findViewById(R.id.buttonNextPage).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                hideKeyboard();
                fileListView.setSelection(fileListAdapter.getCount() - 1);
                return true;
            }
        });
        // 검색
        findViewById(R.id.editText).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    refreshFileList(false);
                    return true;
                }
                return false;
            }
        });
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
                refreshFileList(true);
                break;
            case EVENT_VIEW_FILE:
                String name = msg.getData().getString("name");
                if(fileManager.isZipFile(name))
                {
                    String fullPath = msg.getData().getString("path");
                    if(fullPath.endsWith("/")==false)
                        fullPath += "/";
                    fullPath += msg.getData().getString("name");
                    if(viewImage(null, null, fullPath)==false)
                    {
                        Toast.makeText(this.getApplicationContext(), R.string.MESSAGE_NO_IMAGE, Toast.LENGTH_SHORT).show();
                    }
                }
                else if(fileManager.isImageFile(name))
                {
                    if(viewImage(msg.getData().getString("path"), msg.getData().getString("name"), msg.getData().getString("zipPath"))==false)
                    {
                        Toast.makeText(this.getApplicationContext(), R.string.MESSAGE_NO_IMAGE, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case EVENT_OPEN_FILE:
                if(fileManager.isZipFile(msg.getData().getString("name")))
                {
                    String fullPath = msg.getData().getString("path");
                    if(fullPath.endsWith("/")==false)
                        fullPath += "/";
                    fullPath += msg.getData().getString("name");
                    fileManager.setCurrentDir(fullPath);
                    refreshFileList(true);
                }
                break;
        }
    }

    private boolean viewImage(String path, String name, String zipPath)
    {
        Intent myIntent = new Intent(FullscreenActivity.this, FastImageActivity.class);
        String fullPath = null;
        ArrayList<FileItem> files;

        if(path==null && name==null)
        {
            myIntent.putExtra("fileindex",0);
            files = fileManager.getFiles(zipPath);
        }
        else
        {
            fullPath = FileItem.getFullPath(path,name);
            files = fileManager.getRecentFiles();
        }

        int imageFileCount = 0;
        for(FileItem item : files)
            if (fileManager.isImageFile(item.name))
                imageFileCount++;
        if(imageFileCount==0)
            return false;

        String[] pathArray = new String[imageFileCount];
        int i =0;
        for(FileItem item : files)
        {
            if(fileManager.isImageFile(item.name)==false)
                continue;
            pathArray[i] = item.getFullPath();
            if(item.type == FileItem.TYPE_DIR || item.type == FileItem.TYPE_DIR_IN_ZIP)
                pathArray[i] += "/";
            i++;
        }

        myIntent.putExtra("path", fullPath);
        myIntent.putExtra("zipPath", zipPath);
        myIntent.putExtra("files", pathArray);

        startActivity(myIntent);
        overridePendingTransition(0, 0);
        return true;
    }

    private void hide()
    {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }

    private void hideKeyboard()
    {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = this.getCurrentFocus();
        if(currentFocus!=null)
        {
            currentFocus.clearFocus();
            inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void refreshFileList(boolean isClearStatus)
    {
        EditText search = (EditText) findViewById(R.id.editText);
        if(isClearStatus)
            search.setText("");

        hideKeyboard();
        currentNameTextView.setText( fileManager.getCurrentName() );
        fileListAdapter.updateFileList(search.getText().toString());
        fileListAdapter.notifyDataSetChanged();
        fileListView.setSelectionAfterHeaderView();
    }
}
