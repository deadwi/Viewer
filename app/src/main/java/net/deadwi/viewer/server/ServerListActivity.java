package net.deadwi.viewer.server;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.deadwi.viewer.FileItem;
import net.deadwi.viewer.R;

import java.lang.ref.WeakReference;

public class ServerListActivity  extends AppCompatActivity
{
    public static final int EVENT_CLICK = 1001;
    public static final int EVENT_LONG_CLICK = 1002;
    public static final int EVENT_UPDATE_LIST_OK = 2001;
    public static final int EVENT_UPDATE_LIST_FAIL = 2002;
    public static final int EVENT_SITE_EDIT = 3001;
    public static final int EVENT_SITE_DELETE = 3002;
    public static final int EVENT_UPDATE_DOWNLOAD_LIST = 4001;
    public static final int EVENT_UPDATE_DOWNLOAD_SIZE = 4002;

    public static final String MSG_DATA_NAME = "name";
    public static final String MSG_DATA_PATH = "path";
    public static final String MSG_DATA_TYPE = "type";
    public static final String MSG_DATA_MESSAGE = "msg";

    private ListView fileListView;
    private TextView currentNameTextView;
    private ServerListAdapter listAdapter;
    private ServerManager serverManager;
    private boolean isViewServer=true;
    private String currentPath=null;

    private final MyHandler handler = new MyHandler(this);
    private static class MyHandler extends Handler
    {
        private final WeakReference<ServerListActivity> mActivity;
        public MyHandler(ServerListActivity activity)
        {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg)
        {
            ServerListActivity activity = mActivity.get();
            if (activity == null)
                return;
            activity.handleMessage(msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);

        currentNameTextView = (TextView) findViewById(R.id.textCurrentName);

        serverManager = ServerManager.getInstance();
        serverManager.setHandler(handler);
        listAdapter = new ServerListAdapter(this, serverManager,handler);
        fileListView = (ListView) findViewById(R.id.listView);
        fileListView.setAdapter(listAdapter);

        // 서버 목록
        findViewById(R.id.buttonServerList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goServerList();
            }
        });
        // 다운로드 진척도
        findViewById(R.id.buttonProgress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewDownloadProgress();
            }
        });
        // close
        findViewById(R.id.buttonClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeActivity();
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
        findViewById(R.id.buttonCheckAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAllFiles();
            }
        });
        findViewById(R.id.buttonUncheckAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uncheckAllFiles();
            }
        });
        findViewById(R.id.buttonDown).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isViewServer)
                    downloadFiles();
                else
                    cancelDownloadFiles();
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
                fileListView.setSelection(fileListView.getCount() - 1);
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

    @Override
    protected void onResume()
    {
        super.onResume();
        refreshFileList(false);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        serverManager.setHandler(null);
    }

    @Override
    public void onBackPressed()
    {
        closeActivity();
    }

    private void hide()
    {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }

    private void closeActivity()
    {
        finish();
        overridePendingTransition(0, 0);
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
        hideKeyboard();
        if(isViewServer==true)
        {
            ((Button)findViewById(R.id.buttonDown)).setText(R.string.BUTTON_DOWN);
            findViewById(R.id.buttonDown).setEnabled(currentPath!=null);
            findViewById(R.id.editText).setEnabled(true);
            if(isClearStatus)
                search.setText("");

            currentNameTextView.setText(serverManager.getCurrentName(currentPath));
            listAdapter.updateFileList(currentPath, search.getText().toString());
            listAdapter.notifyDataSetChanged();
            fileListView.setSelectionAfterHeaderView();
        }
        else
        {
            ((Button)findViewById(R.id.buttonDown)).setText(R.string.BUTTON_CANCEL);
            findViewById(R.id.buttonDown).setEnabled(true);
            findViewById(R.id.editText).setEnabled(false);
            search.setText("");

            currentNameTextView.setText("DOWNLOAD");
            listAdapter.updateDownloadList();
            listAdapter.notifyDataSetChanged();
            fileListView.setSelectionAfterHeaderView();
        }
    }

    private void goServerList()
    {
        if(isViewServer == true)
        {
            serverManager.disconnectServer();
            currentPath = null;
            refreshFileList(true);
        }
        // 서버쪽 화면으로 이동
        else
        {
            isViewServer = true;
            refreshFileList(true);
        }
    }

    private void goUpFolder()
    {
        ((EditText) findViewById(R.id.editText)).setText("");
        if(isViewServer == true)
        {
            if (serverManager.movePreviousDir(currentPath) == false) {
                currentPath = null;
                refreshFileList(true);
            }
        }
        // 서버쪽 화면으로 이동
        else
        {
            isViewServer = true;
            refreshFileList(true);
        }
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
        Log.d("SEVER", "handler : " + msg.what);
        String path = msg.getData().getString(MSG_DATA_PATH);
        String name = msg.getData().getString(MSG_DATA_NAME);
        int type = msg.getData().getInt(MSG_DATA_TYPE);
        if(msg.what==EVENT_CLICK)
        {
            switch (type)
            {
                case FileItem.TYPE_NEW_SITE:
                    popupNewServer();
                    break;
                case FileItem.TYPE_SITE:
                    connectServer(path);
                    break;
                case FileItem.TYPE_DIR:
                    moveNextDir(path, name);
                    break;
            }
        }
        else if(msg.what==EVENT_UPDATE_LIST_OK)
        {
            isViewServer = true;
            currentPath = path;
            refreshFileList(true);
        }
        else if(msg.what==EVENT_UPDATE_LIST_FAIL)
        {
            String message = msg.getData().getString(MSG_DATA_MESSAGE);
            if(message==null)
                message = getString(R.string.MESSAGE_UNKNOWN_ERROR);
            Toast.makeText(this.getApplicationContext(), String.format(getString(R.string.MESSAGE_FAIL_GET_LIST), message), Toast.LENGTH_SHORT).show();
        }
        else if(msg.what==EVENT_SITE_EDIT)
        {
            popupEditServer(path);
        }
        else if(msg.what==EVENT_SITE_DELETE)
        {
            popupDeleteServer(path);
        }
        else if(msg.what==EVENT_UPDATE_DOWNLOAD_LIST)
        {
            if(isViewServer==false)
            {
                listAdapter.updateDownloadList();
                listAdapter.notifyDataSetChanged();
            }
        }
        else if(msg.what==EVENT_UPDATE_DOWNLOAD_SIZE)
        {
            if(isViewServer==false)
                updateDownloadSize();
        }
    }

    private void popupNewServer()
    {
        Intent myIntent = new Intent(ServerListActivity.this, ServerEditActivity.class);
        startActivity(myIntent);
        overridePendingTransition(0, 0);
    }

    private void popupEditServer(String path)
    {
        Intent myIntent = new Intent(ServerListActivity.this, ServerEditActivity.class);
        myIntent.putExtra(MSG_DATA_PATH, path);
        startActivity(myIntent);
        overridePendingTransition(0, 0);
    }

    private void popupDeleteServer(final String path)
    {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setMessage("Do you want to delete this sever?")
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
                        ServerStorage.getInstance().removeServer(Integer.parseInt(path));
                        refreshFileList(true);
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    private void connectServer(String path)
    {
        serverManager.connectServer(path);
    }

    private void moveNextDir(String path, String name)
    {
        serverManager.moveNextDir(path, name);
    }

    private void checkAllFiles()
    {
        listAdapter.checkAllFiles();
        listAdapter.notifyDataSetChanged();
    }

    private void uncheckAllFiles()
    {
        listAdapter.uncheckAllFiles();
        listAdapter.notifyDataSetChanged();
    }

    private void downloadFiles()
    {
        int count = listAdapter.downloadFiles();
        if(count>0)
        {
            Toast.makeText(this.getApplicationContext(), String.format(getString(R.string.MESSAGE_ADD_DOWNLOAD), count), Toast.LENGTH_SHORT).show();
            uncheckAllFiles();
        }
    }

    private void cancelDownloadFiles()
    {
        if(isViewServer==false)
        {
            int count = listAdapter.cancelFiles();
            refreshFileList(true);
        }
    }

    private void viewDownloadProgress()
    {
        isViewServer = false;
        refreshFileList(true);
    }

    private void updateDownloadSize()
    {
        listAdapter.updateDownloadSize();
        listAdapter.notifyDataSetChanged();
    }
}
