package com.revel.chat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ContactFragment extends Fragment {
    ArrayList<String> arrayName, arrayNumber, arrayRegisteredUser;
    public static ArrayList<String> arrayListFinalUsersName, arrayLisFinalUserNumber;
    ArrayAdapter adapter;
    ListView lv;
    String number;
    public static String phoneFriend, nameFriend;
    DatabaseReference registeredNumbers;
    TextView tv;

    @Override
    public void onStop() {
        super.onStop();
        saveArrayList(arrayListFinalUsersName, "name");
        saveArrayList(arrayLisFinalUserNumber, "number");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            resolveData();
        } else {

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_contact, container, false);
        lv = v.findViewById(R.id.lv);
        arrayName = new ArrayList<>();
        arrayNumber = new ArrayList<>();
        arrayRegisteredUser = new ArrayList<>();
        arrayListFinalUsersName = getArrayList("name");
        arrayLisFinalUserNumber = getArrayList("number");
        if (arrayListFinalUsersName != null && arrayLisFinalUserNumber != null) {
            adapter = new MyArrayAdapter(getContext(), android.R.layout.simple_list_item_1, arrayLisFinalUserNumber);
            lv.setAdapter(adapter);
        } else {
            arrayLisFinalUserNumber = new ArrayList<>();
            arrayListFinalUsersName = new ArrayList<>();
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            resolveData();
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE}, 1);
        }
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                phoneFriend = arrayLisFinalUserNumber.get(position);
                nameFriend = arrayListFinalUsersName.get(position);
                startActivity(intent);
            }
        });


        return v;
    }

    public void saveArrayList(ArrayList<String> list, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();     // This line is IMPORTANT !!!
    }

    public ArrayList<String> getArrayList(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return gson.fromJson(json, type);
    }


    private void resolveData() {
        ContentResolver cr = getActivity().getContentResolver();
        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

        while (cursor.moveToNext()) {
            number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            if (number.trim().startsWith("+91")) {
                number = number.trim().substring(3, number.length()).replaceAll(" ", "").replaceAll("-", "");
            }
            arrayNumber.add(number);
            arrayName.add(name);
        }
        registeredNumbers = FirebaseDatabase.getInstance().getReference("Registered number");
        registeredNumbers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, String> map = (Map<String, String>) dataSnapshot.getValue();
                    Set key = map.keySet();
                    Iterator iterator = key.iterator();
                    while (iterator.hasNext()) {
                        String number = (String) iterator.next();
                        arrayRegisteredUser.add(number);
                    }
                    workOverData();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void workOverData() {
        for (int i = 0; i < arrayRegisteredUser.size(); i++) {
            for (int j = 0; j < arrayNumber.size(); j++) {
                if (arrayNumber.get(j).contains(arrayRegisteredUser.get(i))) {
                    if (!arrayListFinalUsersName.contains(arrayName.get(j)))
                        arrayListFinalUsersName.add(arrayName.get(j));
                    if (!arrayLisFinalUserNumber.contains(arrayNumber.get(j)))
                        arrayLisFinalUserNumber.add(arrayNumber.get(j));
                }
            }
        }
        adapter = new MyArrayAdapter(getContext(), android.R.layout.simple_list_item_2, arrayLisFinalUserNumber);
        lv.setAdapter(adapter);
    }

    private class MyArrayAdapter extends ArrayAdapter {
        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.custom_list_page, parent, false);
            ImageView iv = v.findViewById(R.id.dp);
            tv = v.findViewById(R.id.name);
            tv.setText(arrayListFinalUsersName.get(position) + "\n" + arrayLisFinalUserNumber.get(position));
            Glide.with(getActivity()).load(R.drawable.dp).circleCrop().into(iv);
            return v;
        }

        public MyArrayAdapter(Context context, int simple_list_item_2, ArrayList<String> arrayListFinalUsersNumber) {
            super(context, simple_list_item_2, arrayListFinalUsersNumber);
        }
    }
}