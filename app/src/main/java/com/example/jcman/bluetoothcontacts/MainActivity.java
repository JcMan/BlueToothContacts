package com.example.jcman.bluetoothcontacts;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import com.app.tool.logger.Logger;
import com.example.jcman.bluetoothcontacts.activity.SearchBlueToothActivity;
import com.example.jcman.bluetoothcontacts.adapter.CommonAdapter;
import com.example.jcman.bluetoothcontacts.adapter.ViewHolder;
import com.example.jcman.bluetoothcontacts.model.Contacts;
import com.example.jcman.bluetoothcontacts.service.BluetoothChatService;
import com.example.jcman.bluetoothcontacts.util.Constants;
import com.example.jcman.bluetoothcontacts.util.ContactsUtil;
import com.example.jcman.bluetoothcontacts.util.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    private Toolbar toolbar;
    private View v_container;
    private List<Contacts> mContactsList;
    private List<Contacts> mMsgContacts;
    private MyAdapter myAdapter;
    private MyAdapter mMsgAdapter;

    private BluetoothAdapter ba;
    private BluetoothChatService mChatService = null;

    private String mConnectedDeviceName;
    private String mReceivedMsg = "";

    public final static int CODE_SEARCH_BLUETOOTH = 101;

    private ProgressDialog mLoadingDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setMessage("正在备份");
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        v_container = findViewById(R.id.coordinatorlayout);
        toolbar.setSubtitle("无连接");
        setSupportActionBar(toolbar);
        initFab();
        mListView = (ListView) findViewById(R.id.listView_contacts);
        mContactsList = ContactsUtil.getLocalContacts(this);
        Collections.sort(mContactsList, new Comparator<Contacts>(){
            @Override
            public int compare(Contacts lhs, Contacts rhs){
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        setAdapter();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                mContactsList.get(position).changeChecked();
                myAdapter.notifyDataSetChanged();
            }
        });
        ba = BluetoothAdapter.getDefaultAdapter();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(1,mConnectedDeviceName,"连接到  " + mConnectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus("连接中");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus("无连接");
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    receive(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != this) {
                        ToastUtil.showMsg(MainActivity.this,"Connected to "
                                + mConnectedDeviceName);
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != this){
                        ToastUtil.showMsg(MainActivity.this,msg.getData().getString(Constants.TOAST));
                    }
                    break;
            }
        }
    };

    private void receive(final String msg)
    {
        Snackbar.make(v_container,"您收到一条消息!",Snackbar.LENGTH_INDEFINITE)
                .setAction("查看", new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mReceivedMsg = msg;
                showMessageDialog();
            }
        }).show();
    }

    private void showMessageDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v_content = View.inflate(this,R.layout.v_dlg_msg,null);
        dialog.setContentView(v_content);
        ListView listView = (ListView) v_content.findViewById(R.id.listview_dlg);
        mMsgContacts = getContactsFromMsg(mReceivedMsg);
        mMsgAdapter = new MyAdapter(this,mMsgContacts,R.layout.item_list_contacts_msg);
        listView.setAdapter(mMsgAdapter);
        Button btn_copy = (Button) v_content.findViewById(R.id.btn_copy);
        btn_copy.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                dialog.dismiss();
                mLoadingDialog.show();
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        int count = 0;
                        for(Contacts con:mMsgContacts){
                            boolean result = ContactsUtil.insert(MainActivity.this,con.getName(),con.getPhoneNumber());
                            if(result)
                                count++;
                        }
                        cancelLoadingDialg();
                        if (count==mMsgContacts.size())
                            showSnackBarMsg();

                    }
                }).start();

            }
        });
        dialog.show();
    }

    private void cancelLoadingDialg(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingDialog.dismiss();
            }
        });
    }

    private void showSnackBarMsg() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(v_container,"备份成功",Snackbar.LENGTH_INDEFINITE).show();
            }
        });
    }


    private List<Contacts> getContactsFromMsg(String msg) {
        List<Contacts> _List = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(msg);
            for(int i=0;i<array.length();i++){
                JSONObject ob = array.getJSONObject(i);
                Contacts con = new Contacts(ob.getString("name"),ob.getString("phonenumber"),true);
                _List.add(con);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return _List;
    }

    private void sendMessageByBlueTooth(String msg){
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
            ToastUtil.showMsg(this,"未连接蓝牙设备！");
            return;
        }
        if (msg.length() > 0){
            ToastUtil.showMsg(this,"正在发送...");
            byte[] send = msg.getBytes();
            mChatService.write(send);
        }
    }



    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mChatService != null){
            mChatService.stop();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if (mChatService != null){
            if (mChatService.getState() == BluetoothChatService.STATE_NONE){
                mChatService.start();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!ba.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 0);
        }
        else if (mChatService==null){
            mChatService = new BluetoothChatService(this,mHandler);
        }
    }

    private void setStatus(int ok,CharSequence Title ,CharSequence subTitle){
        toolbar.setSubtitle(subTitle);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        if (null == this){
            return;
        }
        toolbar.setSubtitle(subTitle);
    }


    public void openBlueTooth(){
        if (!ba.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,0);
        }
    }
    private void  setBlueToothVisible(){
        Intent intent = new Intent(BluetoothAdapter.
                    ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(intent, 0);
    }

    private void setAdapter() {
        myAdapter = new MyAdapter(this,mContactsList, R.layout.item_list_contacts);
        mListView.setAdapter(myAdapter);
    }

    private void initFab(){
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                sendPhoneNumbers();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_bluetooth_visible){
            setBlueToothVisible();
            return true;
        }else if(id==R.id.action_new_bluetooth){
            LinkToDevice();
            return true;
        }else if(id==R.id.action_select_all){
            selectAll();
            return true;
        }else if(id==R.id.action_cancel_all){
            cancelAll();
            return true;
        }else if(id==R.id.action_bluetooth_disable){
            ba.disable();
        }
        return super.onOptionsItemSelected(item);
    }
    private void LinkToDevice(){
        if(!ba.isEnabled())
            openBlueTooth();
        else{
            Intent intent = new Intent(this, SearchBlueToothActivity.class);
            startActivityForResult(intent,CODE_SEARCH_BLUETOOTH);
        }
    }

    private void sendPhoneNumbers(){
        boolean flag = false;
        for(Contacts con:mContactsList){
            if (con.getIsChecked())
                flag = true;
        }
        if(flag){
            getNumbersAndSend();
        }else{
            showMessage("没有号码被选中");
        }
    }

    /**
     * 得到被选中的号码并发送
     */
    private void getNumbersAndSend() {
        JSONArray array = new JSONArray();
        for(Contacts con:mContactsList){
            if(con.getIsChecked()){
                JSONObject ob = new JSONObject();
                try {
                    ob.put("name",con.getName());
                    ob.put("phonenumber",con.getPhoneNumber());
                    array.put(ob);
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }
        sendMessageByBlueTooth(array.toString());
    }

    private void showMessage(String msg){
        View view = findViewById(R.id.coordinatorlayout);
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    /**
     * 取消全部选择
     */
    private void cancelAll() {
        for(Contacts con:mContactsList){
            con.setChecked(false);
        }
        myAdapter.notifyDataSetChanged();
    }

    /**
     * 选择全部号码
     */
    private void selectAll(){
        for(Contacts con:mContactsList){
            con.setChecked(true);
        }
        myAdapter.notifyDataSetChanged();
    }

    private class MyAdapter extends CommonAdapter<Contacts>{
        public MyAdapter(Context context, List<Contacts> list, int layoutId){
            super(context, list, layoutId);
        }
        @Override
        public void convert(ViewHolder holder, final Contacts contacts,int position){
            holder.setText(R.id.tv_item_contacts_name,contacts.getName());
            holder.setText(R.id.tv_item_contacts_phonenumber,contacts.getPhoneNumber());
            final CheckBox box = holder.getView(R.id.checkbox);
            final int pos = position;
            box.setChecked(contacts.getIsChecked());
            box.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    mContactsList.get(pos).changeChecked();
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode==CODE_SEARCH_BLUETOOTH){
            if(resultCode== Activity.RESULT_OK){
                String address = data.getStringExtra("address");
                connectToDevice(address);
            }
        }
    }

    /**
     * 连接到其他蓝牙设备
     * @param address
     */
    private void connectToDevice(String address){
        BluetoothDevice device = ba.getRemoteDevice(address);
        if(device!=null){
            mConnectedDeviceName = device.getName();
            mChatService.connect(device,true);
        }
    }
}
