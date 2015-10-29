package net.deadwi.viewer;

/**
 * Created by jihun.jo on 2015-10-28.
 */
import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CustomAdapter extends BaseAdapter
{
    private ArrayList<FileItem> list;
    private FileManager fileManager;
    private Handler handler;

    public CustomAdapter(FileManager _fileManager, Handler _handler)
    {
        fileManager = _fileManager;
        handler = _handler;
        list = new ArrayList<FileItem>();
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

        // 리스트가 길어지면서 현재 화면에 보이지 않는 아이템은 converView가 null인 상태로 들어 옴
        if ( convertView == null )
        {
            // view가 null일 경우 커스텀 레이아웃을 얻어 옴
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.file_item, parent, false);

            // TextView에 현재 position의 문자열 추가
            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText(list.get(position).name);

            // 버튼을 터치 했을 때 이벤트 발생
            Button btn = (Button) convertView.findViewById(R.id.btn_test);
            btn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 터치 시 해당 아이템 이름 출력
                    Toast.makeText(context, list.get(pos).name, Toast.LENGTH_SHORT).show();
                }
            });

            // 리스트 아이템을 터치 했을 때 이벤트 발생
            convertView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    FileItem item = list.get(pos);
                    if(item.type == FileItem.TYPE_DIR)
                    {
                        fileManager.moveDir(item.name);
                        Message msg = Message.obtain();
                        msg.what = 1;
                        handler.sendMessage(msg);
                    }
                    else
                        Toast.makeText(context, "리스트 클릭 : "+list.get(pos).name, Toast.LENGTH_SHORT).show();
                }
            });

            // 리스트 아이템을 길게 터치 했을 떄 이벤트 발생
            convertView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // 터치 시 해당 아이템 이름 출력
                    Toast.makeText(context, "리스트 롱 클릭 : " + list.get(pos).name, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        return convertView;
    }

    public void add(FileItem item)
    {
        list.add(item);
    }

    public void remove(int _position)
    {
        list.remove(_position);
    }

    public void updateFileList()
    {
        list.clear();

        ArrayList<FileItem> list = fileManager.getCurrentFiles();
        for(Iterator<FileItem> iter = list.iterator();iter.hasNext();)
        {
            FileItem item = iter.next();
            add(item);
        }
    }
}
