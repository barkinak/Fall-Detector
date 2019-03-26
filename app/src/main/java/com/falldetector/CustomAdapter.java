package com.falldetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {
    Context context;
    ArrayList<String> contact_names;
    ArrayList<String> phone_numbers;
    LayoutInflater myInflater;

    public CustomAdapter(Context context, ArrayList<String> contact_names, ArrayList<String> phone_numbers){
        this.context = context;
        this.contact_names = contact_names;
        this.phone_numbers = phone_numbers;
        myInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount(){
        return contact_names.size();
    }
    public Object getItem(int arg0){
        return contact_names.get(arg0);
    }
    public long getItemId(int arg0){
        return arg0;
    }

    public View getView(int position, View view, ViewGroup parent){
        if(view == null) {
            view = myInflater.inflate(R.layout.contact_layout, parent, false);
        }
        ViewHolder holder = new ViewHolder();
        holder.contactName = (TextView) view.findViewById(R.id.contact_name);
        holder.phoneNumber = (TextView) view.findViewById(R.id.phone_num);
        holder.contactName.setText(contact_names.get(position));
        holder.phoneNumber.setText(phone_numbers.get(position));
        return view;
    }

    public class ViewHolder{
        TextView contactName;
        TextView phoneNumber;
    }

}