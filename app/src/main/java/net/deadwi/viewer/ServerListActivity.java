package net.deadwi.viewer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class ServerListActivity  extends AppCompatActivity
{
    private ListView fileListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);
        fileListView = (ListView) findViewById(R.id.listView);

        // 서버 목록
        findViewById(R.id.buttonServerList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        // 다운로드 진척도
        findViewById(R.id.buttonProgress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

    private void goUpFolder()
    {
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
}
