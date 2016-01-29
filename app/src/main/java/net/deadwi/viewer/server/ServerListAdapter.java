package net.deadwi.viewer.server;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import net.deadwi.viewer.FileItem;
import net.deadwi.viewer.FileManager;
import net.deadwi.viewer.R;

import java.util.ArrayList;

public class ServerListAdapter extends BaseAdapter
{
    private ArrayList<FileItem> list;
    private ServerManager serverManager;
    private Handler handler;

    private class CustomHolder {
        TextView textHead;
        TextView textInfo;
        TextView text;
        CheckBox checkbox;
    }

    public ServerListAdapter(ServerManager _serverManager, Handler _handler)
    {
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

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
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

        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                FileItem item = list.get(pos);
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                data.putString(ServerListActivity.MSG_DATA_PATH, item.path);
                data.putString(ServerListActivity.MSG_DATA_NAME, item.name);
                data.putInt(ServerListActivity.MSG_DATA_TYPE, item.type);
                msg.setData(data);
                msg.what = ServerListActivity.EVENT_LONG_CLICK;
                handler.sendMessage(msg);
                return true;
            }
        });

        checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                list.get(pos).checked = ((CheckBox)view).isChecked();
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

    public void checkAllFiles()
    {
        for(FileItem item : list)
            if(item.type==FileItem.TYPE_FILE)
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
}

