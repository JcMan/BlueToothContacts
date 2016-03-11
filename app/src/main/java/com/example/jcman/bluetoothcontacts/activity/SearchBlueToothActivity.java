package com.example.jcman.bluetoothcontacts.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.app.tool.logger.Logger;
import com.example.jcman.bluetoothcontacts.R;
import com.example.jcman.bluetoothcontacts.adapter.CommonAdapter;
import com.example.jcman.bluetoothcontacts.adapter.ViewHolder;
import com.example.jcman.bluetoothcontacts.service.BluetoothChatService;
import com.example.jcman.bluetoothcontacts.util.Constants;
import com.example.jcman.bluetoothcontacts.util.ToastUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by jcman on 16-3-11.
 */
public class SearchBlueToothActivity extends AppCompatActivity implements View.OnClickListener
        ,AdapterView.OnItemClickListener{

    private Toolbar mToolBar;
    private ListView mMatchedListView;
    private ListView mUnMatchedListView;
    private Button mSearchBtn;
    private List<BluetoothDevice> mMatchedDevices;
    private List<BluetoothDevice> mUnMatchedDevices;

    private BluetoothAdapter ba;

    private MyAdapter mUnMatchedAdapter;
    private MyAdapter mMatchedAdapter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_search_bluetooth);
        initView();
        initData();
        initRegisterReceiver();

    }

    private void initRegisterReceiver(){
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private void initData() {
        ba = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
        mMatchedDevices = new ArrayList<>();
        mUnMatchedDevices = new ArrayList<>();
        for(BluetoothDevice device:pairedDevices){
            mMatchedDevices.add(device);
        }
        mMatchedAdapter = new MyAdapter(this,mMatchedDevices, R.layout.item_list_bluetooth_device);
        mUnMatchedAdapter = new MyAdapter(this,mUnMatchedDevices,R.layout.item_list_bluetooth_device);
        mMatchedListView.setAdapter(mMatchedAdapter);
        mUnMatchedListView.setAdapter(mUnMatchedAdapter);
    }

    private void initView() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle("设备列表");
        setSupportActionBar(mToolBar);
        final android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.mipmap.ic_actionbar_back);
        ab.setDisplayHomeAsUpEnabled(true);
        mMatchedListView = (ListView) findViewById(R.id.listview_matched);
        mUnMatchedListView = (ListView) findViewById(R.id.listview_not_match);
        mUnMatchedListView.setOnItemClickListener(this);
        mMatchedListView.setOnItemClickListener(this);
        mSearchBtn = (Button) findViewById(R.id.btn_search_new_bluetooth);
        mSearchBtn.setOnClickListener(this);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED){
                    String address = device.getAddress();
                    boolean isExist = false;
                    for(BluetoothDevice bluetoothDevice :mUnMatchedDevices){
                        if(device.getName().equals(bluetoothDevice.getName()))
                            isExist = true;
                    }
                    if (address.length()>10&&!isExist){
                        mUnMatchedDevices.add(device);
                        mUnMatchedAdapter.notifyDataSetChanged();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                if(mUnMatchedDevices.size()==0){
                    Toast.makeText(SearchBlueToothActivity.this,"没有发现设备",Toast.LENGTH_SHORT).show();
                }
                mSearchBtn.setText("搜索设备");
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThreadExit = true;
        if (ba != null) {
            ba.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ac_search_bluetooth,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id==android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v){
        if (mSearchBtn.getText().toString().equals("搜索设备中～"))
            return;
        ba.startDiscovery();
        mSearchBtn.setText("搜索设备中～");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){

        if(parent.getId()==R.id.listview_matched){
            connectToDevice(position);
        }else if(parent.getId()==R.id.listview_not_match){
            requestBondToDevice(position);
        }
    }

    private void connectToDevice(int position){
        BluetoothDevice device = mMatchedDevices.get(position);
        String address = device.getAddress();
        Intent intent = new Intent();
        intent.putExtra("address",address);
        setResult(Activity.RESULT_OK,intent);
        finish();
    }

    private void requestBondToDevice(int position) {
        BluetoothDevice device = mUnMatchedDevices.get(position);
        ToastUtil.showMsg(this,"正在请求与"+device.getName()+"配对");
        if(device.getBondState()==BluetoothDevice.BOND_NONE){
            try {
                Method creMethod = BluetoothDevice.class.getMethod("createBond");
                creMethod.invoke(device);
                if(mListenBondThread==null){
                    mListenBondThread = new ListenBondThread(device);
                    mListenBondThread.start();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private ListenBondThread mListenBondThread;
    private boolean mThreadExit = false;

    /**
     * 用来监听配对结果的线程
     */
    class ListenBondThread extends Thread{

        private BluetoothDevice mDevice;

        public ListenBondThread(BluetoothDevice device){
            mDevice = device;
        }
        @Override
        public void run(){
            while (true&&!mThreadExit){
                if(mDevice.getBondState()==BluetoothDevice.BOND_NONE) {
//                    Logger.e("none");
                }else if(mDevice.getBondState()==BluetoothDevice.BOND_BONDING){
//                    Logger.e("连接中...");
                }
                else if (mDevice.getBondState()==BluetoothDevice.BOND_BONDED) {
//                    Logger.e("连接成功");
                    bondSuccess(mDevice);
                    break;
                }else{
                    bondFail();
//                    Logger.e("连接失败");
                    break;
                }
            }
        }
    }

    private void bondSuccess(final BluetoothDevice p_device){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.showMsg(SearchBlueToothActivity.this,"成功与"+p_device.getName()+"配对");
                for(BluetoothDevice device:mUnMatchedDevices){
                    if(p_device.getName().equals(device.getName())){
                        mMatchedDevices.add(device);
                        mUnMatchedDevices.remove(device);
                    }
                }
                mUnMatchedAdapter.notifyDataSetChanged();
                mMatchedAdapter.notifyDataSetChanged();
            }
        });
    }

    private void bondFail(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                ToastUtil.showMsg(SearchBlueToothActivity.this,"配对失败");
            }
        });
    }


    private class MyAdapter extends CommonAdapter<BluetoothDevice>{

        public MyAdapter(Context context, List<BluetoothDevice> list, int layoutId) {
            super(context, list, layoutId);
        }

        @Override
        public void convert(ViewHolder holder, BluetoothDevice bluetoothDevice, int pos){
            holder.setText(R.id.tv_item_device_name,bluetoothDevice.getName());
        }
    }
}
