package com.example.jcman.bluetoothcontacts.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by jcman on 16-3-11.
 */
public class ToastUtil {

    public static void showMsg(Context context,String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
