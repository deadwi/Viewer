package net.deadwi.viewer.server;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import net.deadwi.viewer.FileItem;
import net.deadwi.viewer.FileManager;
import net.deadwi.viewer.R;

import java.util.ArrayList;

public class ServerListAdapter extends BaseAdapter
{
    private ArrayList<FileItem> list;
    private Context context;
    private ServerManager serverManager;
    private Handler handler;

    private class CustomHolder {
        TextView textHead;
        TextView textInfo;
        TextView text;
        CheckBox checkbox;
    }

    public ServerListAdapter(Context _context,ServerManager _serverManager, Handler _handler)
    {
        context = _context;
        serverManager = _serverManager;
        handler = _handler;
        list = new ArrayList<>();
    }

    @Override
    public int getCount()
    {
        return list.size();
    }

    @Override
    public Object getItem(int position)
    {
        return list.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final int pos = position;
        final Context context = parent.getContext();

        TextView text;
        TextView textInfo;
        TextView textHead;
        CheckBox checkbox;
        CustomHolder holder;

        // 리스트가 길어지면서 현재 화면에 보이지 않는 아이템은 converView가 null인 상태로 들어 옴
        if ( convertView == null )
        {
            // view가 null일 경우 커스텀 레이아웃을 얻어 옴
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.file_item, parent, false);

            text = (TextView) convertView.findViewById(R.id.text);
            textInfo = (TextView) convertView.findViewById(R.id.textInfo);
            textHead = (TextView) convertView.findViewById(R.id.textHead);
            checkbox = (CheckBox)convertView.findViewById(R.id.checkBox);

            // 홀더 생성 및 Tag로 등록
            holder = new CustomHolder();
            holder.text = text;
            holder.textInfo = textInfo;
            holder.textHead = textHead;
            holder.checkbox = checkbox;
            convertView.setTag(holder);
        }
        else
        {
            holder = (CustomHolder) convertView.getTag();
            text = holder.text;
            textInfo = holder.textInfo;
            textHead = holder.textHead;
            checkbox = holder.checkbox;
        }
        text.setText(list.get(position).name);

        if(list.get(position).type == FileItem.TYPE_NEW_SITE)
        {
            textHead.setText("+");
            textInfo.setText("");
            checkbox.setVisibility(View.GONE);
        }
        else if(list.get(position).type == FileItem.TYPE_SITE)
        {
            textHead.setText("S");
            textInfo.setText("");
            checkbox.setVisibility(View.GONE);
        }
        else if(list.get(position).type == FileItem.TYPE_DIR)
        {
            textHead.setText("D");
            textInfo.setText("");
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setChecked( list.get(position).checked );
        }
        else
        {
            if(FileManager.isBookFile(list.get(position).name))
                textHead.setText("B");
            else
                textHead.setText("F");

            String info = FileManager.getFileSizeText(list.get(position).size);
            textInfo.setText(info);
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setChecked(list.get(position).checked);
        }

        if(list.get(position).type != FileItem.TYPE_DOWNLOAD_DIR && list.get(position).type != FileItem.TYPE_DOWNLOAD_FILE) {
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FileItem item = list.get(pos);
                    Message msg = Message.obtain();
                    Bundle data = new Bundle();
                    data.putString(ServerListActivity.MSG_DATA_PATH, item.path);
                    data.putString(ServerListActivity.MSG_DATA_NAME, item.name);
                    data.putInt(ServerListActivity.MSG_DATA_TYPE, item.type);
                    msg.setData(data);
                    msg.what = ServerListActivity.EVENT_CLICK;
                    handler.sendMessage(msg);
                }
            });
        }

        if(list.get(position).type == FileItem.TYPE_SITE)
        {
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    PopupMenu popup = new PopupMenu(context, v);
                    popup.getMenuInflater().inflate(R.menu.poupup_menu, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            FileItem item = list.get(pos);
                            Message msg = Message.obtain();
                            Bundle data = new Bundle();
                            data.putString(ServerListActivity.MSG_DATA_PATH, item.path);
                            data.putString(ServerListActivity.MSG_DATA_NAME, item.name);
                            data.putInt(ServerListActivity.MSG_DATA_TYPE, item.type);
                            msg.setData(data);
                            if (menuItem.getItemId() == R.id.edit)
                            {
                                msg.what = ServerListActivity.EVENT_SITE_EDIT;
                                handler.sendMessage(msg);
                            }
                            else if (menuItem.getItemId() == R.id.delete)
                            {
                                msg.what = ServerListActivity.EVENT_SITE_DELETE;
                                handler.sendMessage(msg);
                            }
                            return true;
                        }
                    });
                    popup.show();
                    return true;
                }
            });
        }

        checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                list.get(pos).checked = ((CheckBox) view).isChecked();
            }
        });


        return convertView;
    }

    public void updateFileList(String path, String keyword)
    {
        if(keyword.isEmpty())
            list = serverManager.getCurrentFiles(path);
        else
            list = serverManager.getMatchFiles(path, keyword);
    }

    public void updateDownloadList()
    {
        list = serverManager.getDownloadFiles(list);
    }

    public void checkAllFiles()
    {
        for(FileItem item : list)
            if(item.type==FileItem.TYPE_FILE ||
                    item.type==FileItem.TYPE_DOWNLOAD_FILE ||
                    item.type==FileItem.TYPE_DOWNLOAD_DIR)
                item.checked = true;
    }

    public void uncheckAllFiles()
    {
        for(FileItem item : list)
            item.checked = false;
    }

    public int downloadFiles()
    {
        int count = 0;
        for(FileItem item : list)
        {
            if(item.checked == false)
                continue;
            serverManager.addDownload( item.getFullPath(), item.name, item.type==FileItem.TYPE_DIR );
            count++;
        }
        return count;
    }

    public int cancelFiles()
    {
        return serverManager.cancelDownload(list);
    }
}

