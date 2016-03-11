package com.example.jcman.bluetoothcontacts.model;

/**
 * Created by jcman on 16-3-11.
 */
public class Contacts {

    private String name;
    private String phoneNumber;
    private boolean isChecked;

    public Contacts(String name,String phoneNumber,boolean isChecked){
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.isChecked = isChecked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean getIsChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public void changeChecked(){
        if (isChecked){
            isChecked = false;
        }else{
            isChecked = true;
        }
    }
}
