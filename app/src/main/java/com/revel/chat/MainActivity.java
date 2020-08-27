package com.revel.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static com.revel.chat.AuthenticationActivity.phoneUser;
import static com.revel.chat.ContactFragment.nameFriend;
import static com.revel.chat.ContactFragment.phoneFriend;

public class MainActivity extends AppCompatActivity {
    EditText text;
    Button send, optionButton;
    ListView lv;
    ImageView dp;
    ArrayList arrayListChat;
    ArrayAdapter adapter;
    DatabaseReference mRefSend, mRefReceive;
    TextView tvS, tvR;
    Queue q;
    String notificationKey;
    TextView userName;
    String lastMsg;

    @Override
    protected void onStop() {
        super.onStop();
        saveArrayList(arrayListChat, phoneFriend);
        SharedPreferences sp = getSharedPreferences("lastMsg", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("lastMsg", lastMsg);
        editor.apply();
    }

    @Override
    protected void onResume() {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(1500);
        text.startAnimation(alphaAnimation);
        send.startAnimation(alphaAnimation);
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = findViewById(R.id.et);
        send = findViewById(R.id.btn);

        lv = findViewById(R.id.lv);
        dp = findViewById(R.id.dp);
        optionButton = findViewById(R.id.optionButton);
        userName = findViewById(R.id.userName);
        userName.setText(nameFriend);

        arrayListChat = getArrayList(phoneFriend);
        if (arrayListChat == null) {
            arrayListChat = new ArrayList();
        } else {
            adapter = new MyAdapterS(MainActivity.this, android.R.layout.simple_list_item_1, arrayListChat);
            lv.setAdapter(adapter);
        }

        Glide.with(this).load(R.drawable.dp).circleCrop().into(dp);
        // Write a message to the database
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        mRefSend = database.getReference("" + phoneFriend);
        mRefReceive = database.getReference("" + phoneUser);
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("NotificationKey");
                reference.child("" + phoneUser).setValue("" + userId);
            }
        });

        optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(MainActivity.this, optionButton);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.delete:
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Delete")
                                        .setMessage("Once chat is deleted can't be recovered")
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                                SharedPreferences.Editor editor = prefs.edit();
                                                editor.remove(phoneFriend).apply();
                                                arrayListChat = getArrayList(phoneFriend);
                                                arrayListChat = new ArrayList();
                                                adapter = new MyAdapterS(MainActivity.this, android.R.layout.simple_list_item_1, arrayListChat);
                                                lv.setAdapter(adapter);
                                            }
                                        });
                                builder.create().show();
                                break;
                        }
                        return true;
                    }
                });

                popup.show();//showing popup menu
            }
        });
        q = new LinkedList();
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = text.getText().toString();
                if (msg.trim().length() != 0) {
                    q.add(msg);
                    arrayListChat.add("S" + msg);
                    lv.setAdapter(adapter);
                    text.setText("");

                    FirebaseDatabase.getInstance().getReference("NotificationKey").child(phoneFriend)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    notificationKey = (String) dataSnapshot.getValue();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                    try {
                        OneSignal.postNotification(new JSONObject("{'contents': {'en':'" + msg + "'}, 'include_player_ids': ['" + notificationKey + "'], 'headings': {'en':'New message'}}"), null);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        if (q.size() != 0 && checkNetwork() != null) {

                            Iterator itr = q.iterator();
                            while (itr.hasNext()) {
                                mRefSend.child("" + phoneUser).setValue(q.poll());
                            }
                        }
                        handler.postDelayed(this, 1000);
                    }
                };
                handler.postDelayed(runnable, 1000);

            }
        });

        mRefReceive.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, String> map = (Map<String, String>) dataSnapshot.getValue();
                    if (map != null) {
                        Set key = map.keySet();
                        Iterator itr = key.iterator();
                        while (itr.hasNext()) {
                            String phone = (String) itr.next();
                            if (phoneFriend.equals(phone)) {
                                String received = map.get(phone);
                                if (!received.trim().equals("")) {
                                    SharedPreferences sp = getSharedPreferences("lastMsg", MODE_PRIVATE);
                                    lastMsg = sp.getString("lastMsg", "");
                                    if (null != lastMsg && !received.equals(lastMsg)) {
                                        lastMsg = received;
                                        arrayListChat.add("R" + received);
                                        adapter = new MyAdapterS(MainActivity.this, android.R.layout.simple_list_item_1, arrayListChat);
                                        lv.setAdapter(adapter);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


    private Network checkNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager.getActiveNetwork();
        return activeNetwork;
    }


    private class MyAdapterS extends ArrayAdapter {
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.chat_ui, parent, false);
            tvS = v.findViewById(R.id.sent_text);
            tvR = v.findViewById(R.id.received_text);
            String curr = (String) arrayListChat.get(position);
            int size = curr.length();
            if (curr.startsWith("S")) {
                curr = curr.substring(1, size);
                tvR.setVisibility(View.GONE);
                tvS.setVisibility(View.VISIBLE);
                tvS.setText(curr);
            } else if (curr.startsWith("R")) {
                curr = curr.substring(1, size);
                tvS.setVisibility(View.GONE);
                tvR.setVisibility(View.VISIBLE);
                tvR.setText(curr);
            }
            return v;
        }

        public MyAdapterS(MainActivity mainActivity, int simple_list_item_1, ArrayList textS) {
            super(mainActivity, simple_list_item_1, textS);

        }
    }

    public void saveArrayList(ArrayList<String> list, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();     // This line is IMPORTANT !!!
    }

    public ArrayList<String> getArrayList(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return gson.fromJson(json, type);
    }
}