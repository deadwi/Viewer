package net.deadwi.viewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
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
    public static final int EVENT_NEXT_PATH = 1002;
    public static final int EVENT_VIEW_FILE = 1003;
    public static final int EVENT_OPEN_FILE = 1004;
    public static final String VIEW_TYPE_IMAGE = "image";
    public static final String VIEW_TYPE_PDF = "pdf";

    public static final String MSG_DATA_NAME = "name";
    public static final String MSG_DATA_PATH = "path";
    public static final String MSG_DATA_ZIP_PATH = "zip_path";
    public static final String MSG_DATA_VIEW_TYPE = "view_type";
    public static final String MSG_DATA_VIEW_FILE = "view_file";
    public static final String MSG_DATA_VIEW_INDEX = "view_index";
    public static final String MSG_DATA_FILES = "files";

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

        Option.getInstance().loadOption( getFilesDir().getAbsolutePath() );
        if(Option.getInstance().IsPortrait())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Bookmark.getInstance().setSavePath(getApplicationContext().getFilesDir().getAbsolutePath());

        setContentView(R.layout.activity_fullscreen);

        fileManager = new FileManager();
        fileManager.setShowHiddenFiles(true);
        fileManager.setSortType(FileManager.SORT_ALPHA_WITH_NUM);
        // last path
        fileManager.setCurrentDirInZip(Option.getInstance().getLastCurrentPath(), Option.getInstance().getLastInnerPath());
        Log.d("MAIN", "last path : " + (Option.getInstance().getLastCurrentPath() != null ? Option.getInstance().getLastCurrentPath() : "NULL"));

        currentNameTextView = (TextView) findViewById(R.id.textCurrentName);
        fileListAdapter = new CustomAdapter(fileManager, handler);
        fileListView = (ListView) findViewById(R.id.listView);
        fileListView.setAdapter(fileListAdapter);

        refreshFileList(true);

        // 옵션 디렉토리
        findViewById(R.id.buttonOption).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(FullscreenActivity.this, OptionTabActivity.class);
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
        // 다운로드 디렉토리
        findViewById(R.id.buttonDown).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Not support yet", Toast.LENGTH_SHORT).show();
            }
        });
        // 상위 디렉토리
        findViewById(R.id.buttonUpFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goUpFolder();
            }
        });
        // 이전 페이지
        findViewById(R.id.buttonPrevPage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goNextPage();
            }
        });
        // 다음 페이지
        findViewById(R.id.buttonNextPage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goPreviousPage();
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

        viewLastImage();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        hide();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(Option.getInstance().IsPortrait())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        refreshFileList(false);
        Option.getInstance().setChanged(false);
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent event)
    {
        Log.d("MAIN", "key=" + keycode);
        // ridibooks.com/Paper hardware key
        switch(keycode)
        {
            // prev
            case KeyEvent.KEYCODE_PAGE_UP:
                //case KeyEvent.KEYCODE_VOLUME_UP:
                goPreviousPage();
                return true;
            // next
            case KeyEvent.KEYCODE_PAGE_DOWN:
                //case KeyEvent.KEYCODE_VOLUME_DOWN:
                goNextPage();
                return true;
        }
        return super.onKeyDown(keycode, event);
    }

    @Override
    public void onBackPressed()
    {
        popupCloseDialog();
    }

    private void goUpFolder()
    {
        fileManager.movePreviousDir();
        refreshFileList(true);
    }

    private void goNextPage()
    {
        hideKeyboard();
        int visibleChildCount = (fileListView.getLastVisiblePosition() - fileListView.getFirstVisiblePosition()) + 1;
        int prevPos = fileListView.getFirstVisiblePosition() - visibleChildCount + 2;
        if (prevPos < 0)
            prevPos = 0;
        fileListView.setSelection(prevPos);
    }

    private void goPreviousPage()
    {
        hideKeyboard();
        fileListView.setSelection(fileListView.getLastVisiblePosition());
    }

    private void handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case EVENT_UPDATE_FILE_LIST:
                refreshFileList(true);
                break;
            case EVENT_NEXT_PATH:
                fileManager.moveNextDir(msg.getData().getString(MSG_DATA_NAME));
                refreshFileList(true);
                break;
            case EVENT_VIEW_FILE:
                String name = msg.getData().getString(MSG_DATA_NAME);
                if(fileManager.isZipFile(name))
                {
                    String viewName = null;
                    int viewIndex = 0;
                    String zipPath = msg.getData().getString(MSG_DATA_PATH);
                    if(zipPath.endsWith("/")==false)
                        zipPath += "/";
                    zipPath += msg.getData().getString(MSG_DATA_NAME);

                    // load by bookmark
                    if(msg.getData().containsKey(MSG_DATA_VIEW_FILE))
                    {
                        viewName = msg.getData().getString(MSG_DATA_VIEW_FILE);
                        viewIndex = msg.getData().getInt(MSG_DATA_VIEW_INDEX);
                    }

                    if(viewImage(null, viewName, zipPath, viewIndex)==false)
                    {
                        Toast.makeText(this.getApplicationContext(), R.string.MESSAGE_NO_IMAGE, Toast.LENGTH_SHORT).show();
                    }
                }
                else if(fileManager.isImageFile(name))
                {
                    if(viewImage(msg.getData().getString(MSG_DATA_PATH), msg.getData().getString(MSG_DATA_NAME), msg.getData().getString(MSG_DATA_ZIP_PATH), 0)==false)
                    {
                        Toast.makeText(this.getApplicationContext(), R.string.MESSAGE_NO_IMAGE, Toast.LENGTH_SHORT).show();
                    }
                }
                else if(fileManager.isPdfFile(name))
                {
                    String path = msg.getData().getString(MSG_DATA_PATH);
                    if(path.endsWith("/")==false)
                        path += "/";
                    path += msg.getData().getString(MSG_DATA_NAME);

                    String viewName = null;
                    int viewIndex = 0;
                    // load by bookmark
                    if(msg.getData().containsKey(MSG_DATA_VIEW_FILE))
                    {
                        viewName = msg.getData().getString(MSG_DATA_VIEW_FILE);
                        viewIndex = msg.getData().getInt(MSG_DATA_VIEW_INDEX);
                    }
                    viewPdf(path, viewName, viewIndex);
                }
                break;
            case EVENT_OPEN_FILE:
                if(msg.getData().getString(MSG_DATA_ZIP_PATH)!=null)
                {
                    String fullPath = FileManager.getFullPath( msg.getData().getString(MSG_DATA_PATH), msg.getData().getString(MSG_DATA_NAME) );
                    if(fullPath.charAt(0)=='/')
                        fullPath = fullPath.substring(1);
                    Log.d("Main","Open : "+fullPath);
                    fileManager.setCurrentDirInZip(msg.getData().getString(MSG_DATA_ZIP_PATH), fullPath);
                    refreshFileList(true);
                }
                else if(fileManager.isZipFile(msg.getData().getString(MSG_DATA_NAME)))
                {
                    String fullPath = msg.getData().getString(MSG_DATA_PATH);
                    if(fullPath.endsWith("/")==false)
                        fullPath += "/";
                    fullPath += msg.getData().getString(MSG_DATA_NAME);
                    fileManager.setCurrentDir(fullPath);
                    refreshFileList(true);
                }
                break;
        }
    }

    private void viewLastImage()
    {
        String viewPath = Option.getInstance().getLastViewPath();
        String zipPath = Option.getInstance().getLastViewZipPath();
        if(viewPath==null)
            return;

        Message msg = Message.obtain();
        Bundle data = new Bundle();
        if(zipPath==null)
        {
            if(FileManager.isExist(viewPath)==false)
                return;
            data.putString(FullscreenActivity.MSG_DATA_PATH, FileManager.getPathFromFullpath(viewPath, "/"));
            data.putString(FullscreenActivity.MSG_DATA_NAME, FileManager.getNameFromFullpath(viewPath));
        }
        else
        {
            if(FileManager.isExist(zipPath)==false)
                return;
            data.putString(FullscreenActivity.MSG_DATA_PATH, FileManager.getPathFromFullpath(zipPath, "/"));
            data.putString(FullscreenActivity.MSG_DATA_NAME, FileManager.getNameFromFullpath(zipPath));
            data.putString(FullscreenActivity.MSG_DATA_VIEW_FILE, viewPath );
            data.putInt(FullscreenActivity.MSG_DATA_VIEW_INDEX, Option.getInstance().getLastViewIndex() );
        }
        msg.setData(data);
        msg.what = FullscreenActivity.EVENT_VIEW_FILE;
        handler.sendMessage(msg);
    }

    private boolean viewImage(String path, String name, String zipPath, int viewIndex)
    {
        Intent myIntent = new Intent(FullscreenActivity.this, FastImageActivity.class);
        String fullPath = null;
        ArrayList<FileItem> files;

        if(path==null)
        {
            if(name!=null)
                fullPath = name;
            files = fileManager.getFiles(zipPath);
        }
        else
        {
            fullPath = FileManager.getFullPath(path,name);
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

        myIntent.putExtra(MSG_DATA_VIEW_TYPE, VIEW_TYPE_IMAGE);
        myIntent.putExtra(MSG_DATA_PATH, fullPath);
        myIntent.putExtra(MSG_DATA_ZIP_PATH, zipPath);
        myIntent.putExtra(MSG_DATA_FILES, pathArray);
        myIntent.putExtra(MSG_DATA_VIEW_INDEX, viewIndex);

        startActivity(myIntent);
        overridePendingTransition(0, 0);
        return true;
    }

    private boolean viewPdf(String path, String viewName, int viewIndex)
    {
        Intent pdfIntent = new Intent(FullscreenActivity.this, FastImageActivity.class);
        pdfIntent.putExtra(MSG_DATA_VIEW_TYPE, VIEW_TYPE_PDF);
        pdfIntent.putExtra(MSG_DATA_ZIP_PATH, path);
        pdfIntent.putExtra(MSG_DATA_PATH, viewName);
        pdfIntent.putExtra(MSG_DATA_VIEW_INDEX, viewIndex);

        startActivity(pdfIntent);
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
        currentNameTextView.setText(fileManager.getCurrentName());
        fileListAdapter.updateFileList(search.getText().toString());
        fileListAdapter.notifyDataSetChanged();
        fileListView.setSelectionAfterHeaderView();

        Option.getInstance().setLastPath(fileManager.getCurrentDir(), fileManager.getCurrentInnerDir());
        Option.getInstance().saveLastPath();
    }

    private void popupCloseDialog()
    {
        Log.d("MAIN","call close dialog");
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setMessage("Do you want to exit the program?")
                .setCancelable(false)
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.cancel();
                            return true;
                        }
                        return false;
                    }
                })
                .setPositiveButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    }
                });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }
}
