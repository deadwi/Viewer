package net.deadwi.viewer.server;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;

import net.deadwi.viewer.R;
import net.deadwi.viewer.server.ServerInfo;
import net.deadwi.viewer.server.ServerStorage;

/**
 * Created by jihun.jo on 2016-01-26.
 */
public class ServerEditActivity  extends AppCompatActivity
{
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_server);

        findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(setOptionFromUI()) {
                    finish();
                    overridePendingTransition(0, 0);
                }
            }
        });
        findViewById(R.id.buttonCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(0, 0);
            }
        });

        // default
        ((RadioButton)findViewById(R.id.radioMethodHttp)).setChecked(true);

        // edit server info?
        path = getIntent().getStringExtra(ServerListActivity.MSG_DATA_PATH);
        if(path!=null)
        {
            ServerInfo info = ServerStorage.getInstance().getServerInfo(Integer.parseInt(path));
            ((EditText)findViewById(R.id.editTextTitle)).setText(info.title);
            ((EditText)findViewById(R.id.editTextURL)).setText(info.url);
            ((EditText)findViewById(R.id.editTextUser)).setText(info.user);
            ((EditText)findViewById(R.id.editTextPassword)).setText(info.password);
        }
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

    private boolean setOptionFromUI()
    {
        if(((EditText)findViewById(R.id.editTextTitle)).getText().toString().isEmpty() ||
                ((EditText)findViewById(R.id.editTextURL)).getText().toString().isEmpty())
        {
            popupAlertDialog("There are missing fields");
            return false;
        }

        ServerInfo info = new ServerInfo();
        if(((RadioButton)findViewById(R.id.radioMethodHttp)).isChecked())
            info.method = ServerInfo.METHOD_HTTP;
        else
            info.method = ServerInfo.METHOD_HTTP;
        info.title = ((EditText)findViewById(R.id.editTextTitle)).getText().toString();
        info.url = ((EditText)findViewById(R.id.editTextURL)).getText().toString();
        info.user = ((EditText)findViewById(R.id.editTextUser)).getText().toString();
        info.password = ((EditText)findViewById(R.id.editTextPassword)).getText().toString();

        if(path!=null)
            ServerStorage.getInstance().updateServer(Integer.parseInt(path),info);
        else
            ServerStorage.getInstance().updateServer(ServerStorage.INDEX_NEW,info);
        return true;
    }

    private void popupAlertDialog(String message)
    {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setMessage(message)
                .setCancelable(false)
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.dismiss();
                            return true;
                        }
                        return false;
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }
}
