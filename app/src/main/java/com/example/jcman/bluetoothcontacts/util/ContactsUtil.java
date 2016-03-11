package com.example.jcman.bluetoothcontacts.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.app.tool.logger.Logger;
import com.example.jcman.bluetoothcontacts.model.Contacts;
import android.provider.ContactsContract.CommonDataKinds.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jcman on 16-3-11.
 */
public class ContactsUtil {

    public static List<Contacts> getLocalContacts(Context context){
        return getPhoneNumbersByUri(context,Phone.CONTENT_URI);
    }
    private static List<Contacts> getPhoneNumbersByUri(Context context, Uri uri){
        String[] PHONES_PROJECTION = new String[]{
                Phone.DISPLAY_NAME, Phone.NUMBER, Phone.CONTACT_ID };
        List<Contacts> _List = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri,PHONES_PROJECTION,null,null,null);
        if(cursor!=null){
            while (cursor.moveToNext()){
                String phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                String name = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
                Contacts contacts = new Contacts(name,phoneNumber,false);
                _List.add(contacts);
            }
        }
        return _List;
    }

    public static boolean insert(Context context,String name, String mobile_number){
        try {
            ContentValues values = new ContentValues();
            // 下面的操作会根据RawContacts表中已有的rawContactId使用情况自动生成新联系人的rawContactId
            Uri rawContactUri = context.getContentResolver().insert(
                    ContactsContract.RawContacts.CONTENT_URI, values);
            long rawContactId = ContentUris.parseId(rawContactUri);

            // 向data表插入姓名数据
            if (name != "") {
                values.clear();
                values.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
                values.put(ContactsContract.Contacts.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                values.put(StructuredName.GIVEN_NAME, name);
                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
            }
            // 向data表插入电话数据
            if (mobile_number != "") {
                values.clear();
                values.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
                values.put(ContactsContract.Contacts.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                values.put(Phone.NUMBER, mobile_number);
                values.put(Phone.TYPE, Phone.TYPE_MOBILE);
                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI,
                        values);
            }
        }
        catch (Exception e){
            return false;
        }
        return true;
    }
}
