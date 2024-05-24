package com.example.peachzyapp.fragments.MainFragments.Chats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.example.peachzyapp.LiveData.MyViewChatModel;
import com.example.peachzyapp.MainActivity;
import com.example.peachzyapp.Other.Utils;
import com.example.peachzyapp.R;
import com.example.peachzyapp.SocketIO.MyWebSocket;
import com.example.peachzyapp.adapters.ConversationAdapter;
import com.example.peachzyapp.dynamoDB.DynamoDBManager;
import com.example.peachzyapp.entities.Conversation;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class ChatListForForwardMessageFragment extends Fragment  implements MyWebSocket.WebSocketListener {
    public static final String TAG= ChatListForForwardMessageFragment.class.getName();
    private MainActivity mainActivity;
    private RecyclerView rcvChatList;
    private View view;
    private ArrayList<Conversation> conversationsList;
    private DynamoDBManager dynamoDBManager;
    private String uid;
    private ConversationAdapter conversationAdapter;
    private MyViewChatModel viewModel;
    private String forwardType, forwardMessage,forwardIDfriend;
    private String forwardChannelID,forwardMyAvatar,forwardMyName;
    private MyWebSocket myWebSocket;
    private ImageButton btnBack;
    private SimpleDateFormat dateFormat;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_chat_list_for_forward_message, container, false);
        dynamoDBManager = new DynamoDBManager(getActivity());
        rcvChatList = view.findViewById(R.id.rcvConversation);
        rcvChatList.setLayoutManager(new LinearLayoutManager(mainActivity));
        conversationsList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationsList);
        // Set adapter to RecyclerView
        rcvChatList.setAdapter(conversationAdapter);
        SharedPreferences preferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        uid = preferences.getString("uid", null);
        if (uid != null) {
            Log.d("FriendcheckUIDInChatListFragment", uid);
            // Sử dụng "uid" ở đây cho các mục đích của bạn
        } else {
            Log.e("FriendcheckUID", "UID is null");
        }
        Bundle bundle = getArguments();
        if (bundle != null) {
            forwardIDfriend=bundle.getString("forwardIDfriend");
            forwardType = bundle.getString("forwardType");
            forwardMessage=bundle.getString("forwardMessage");
            forwardMyAvatar = bundle.getString("forwardMyAvatar");
            forwardMyName=bundle.getString("forwardMyName");
            Log.d("ForwardData", "Type: " + forwardType + ", Message: ");
        }
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        dynamoDBManager.loadConversation(uid, new DynamoDBManager.LoadConversationListener() {
            @Override
            public void onConversationFound(String conversationID, String friendID, String message, String time, String avatar, String name) {
                try {
                    if(forwardIDfriend.equals(friendID)==false) {
                        // Parse the time string into a Date object
                        Date conversationTime = dateFormat.parse(time);

                        // Create the conversation object
                        Conversation conversation = new Conversation(conversationID, friendID, message, time, avatar, name);

                        // Add the conversation to the list
                        conversationsList.add(conversation);

                        // Sort the list based on the conversationTime in descending order
                        Collections.sort(conversationsList, new Comparator<Conversation>() {
                            @Override
                            public int compare(Conversation c1, Conversation c2) {
                                Date date1 = null;
                                Date date2 = null;
                                try {
                                    date1 = dateFormat.parse(c1.getTime());
                                    date2 = dateFormat.parse(c2.getTime());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                return date2.compareTo(date1); // Sort in descending order
                            }
                        });

                        // Notify the adapter of the dataset change
                        conversationAdapter.notifyDataSetChanged();
                    }
                } catch (ParseException e) {
                    // Handle parsing exceptions if any
                    e.printStackTrace();
                }
            }

            @Override
            public void onLoadConversationError(Exception e) {
                e.printStackTrace();
            }
        });
        conversationsList.clear();
        conversationAdapter.setOnItemClickListener(new ConversationAdapter.OnItemClickListener(){
            @Override
            public void onItemClick(String id, String urlAvatar, String friendName) {

                dynamoDBManager.getChannelID(uid, id, new DynamoDBManager.ChannelIDinterface() {
                    @Override
                    public void GetChannelId(String channelID) {
                        forwardChannelID=channelID;
                        initWebSocket(channelID);
                        Log.d("ForwardData", "Channel ID: " + forwardChannelID + ", Message: " + forwardMessage +
                                ", Type: " + forwardType + ", My Avatar: " + forwardMyAvatar +
                                ", Friend Name: " + urlAvatar + ", My Name: " + forwardMyName+", Channel ID: " + forwardChannelID);
                        String currentTime = Utils.getCurrentTime();
                        sendMessageToSocket(channelID, uid, forwardMyName, forwardMyAvatar, forwardMessage, getCurrentDateTime(), forwardType);
                        dynamoDBManager.saveMessageOneToOne(forwardChannelID,forwardMessage,getCurrentDateTime(),forwardType,uid,id);
                        if(forwardType.equals("text"))
                        {
                            dynamoDBManager.saveConversation(uid,  id, forwardMyName+": "+forwardMessage, getCurrentDateTime(), forwardMyAvatar, friendName);
                            dynamoDBManager.saveConversation(id, uid,forwardMyName+": "+forwardMessage, getCurrentDateTime(), urlAvatar, forwardMyName);
                        }else
                        {
                            dynamoDBManager.saveConversation(uid,  id, forwardMyName+": "+forwardType, getCurrentDateTime(), forwardMyAvatar, friendName);
                            dynamoDBManager.saveConversation(id, uid,forwardMyName+": "+forwardType, getCurrentDateTime(), urlAvatar, forwardMyName);
                        }
                        //live data

                    }
                });

                conversationsList.clear();
                changeData();
                getParentFragmentManager().popBackStack();
                mainActivity.showBottomNavigation(false);
            }
        });
        //Live data
        viewModel = new ViewModelProvider(requireActivity()).get(MyViewChatModel.class);
//        viewModel.getData().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(String newData) {
//                Log.d("Livedata1", "onChanged: Yes");
//                resetRecycleView();
//            }
//        });
        btnBack=view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
            mainActivity.showBottomNavigation(false);
        });
        return view;
    }
    private void resetRecycleView() {
        conversationsList.clear();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                dynamoDBManager.loadConversation(uid, new DynamoDBManager.LoadConversationListener() {
                    @Override
                    public void onConversationFound(String conversationID, String friendID, String message, String time, String avatar, String name) {
                        try {
                            // Parse the time string into a Date object
                            Date conversationTime = dateFormat.parse(time);

                            // Create the conversation object
                            Conversation conversation = new Conversation(conversationID, friendID, message, time, avatar, name);

                            // Add the conversation to the list
                            conversationsList.add(conversation);

                            // Sort the list based on the conversationTime in descending order
                            Collections.sort(conversationsList, new Comparator<Conversation>() {
                                @Override
                                public int compare(Conversation c1, Conversation c2) {
                                    Date date1 = null;
                                    Date date2 = null;
                                    try {
                                        date1 = dateFormat.parse(c1.getTime());
                                        date2 = dateFormat.parse(c2.getTime());
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    return date2.compareTo(date1); // Sort in descending order
                                }
                            });

                            // Notify the adapter of the dataset change
                            conversationAdapter.notifyDataSetChanged();
                        } catch (ParseException e) {
                            // Handle parsing exceptions if any
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onLoadConversationError(Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }, 1000); // 2000 milliseconds = 2 seconds
    }
    private void sendMessageToSocket(String channel_id, String friend_id, String userName, String senderAvatar, String message, String time, String type )
    {
        JSONObject messageToSend = new JSONObject();
        JSONObject json = new JSONObject();
        try{
            messageToSend.put("conversation_id", channel_id);
            messageToSend.put("from", uid);
            messageToSend.put("to", friend_id);
            messageToSend.put("memberName", userName);
            messageToSend.put("avatar", senderAvatar);
            messageToSend.put("text", message);
            messageToSend.put("time", time);
            messageToSend.put("type", type);
            json.put("type", "send-message");
            json.put("message", messageToSend);

        }catch (JSONException e) {
            throw new RuntimeException(e);
        }
        myWebSocket.sendMessage(String.valueOf(json));
    }
    private void initWebSocket(String channelID) {
        // Kiểm tra xem channel_id đã được thiết lập chưa
        if (channelID != null) {
            // Nếu đã có channel_id, thì khởi tạo myWebSocket
            myWebSocket = new MyWebSocket("wss://free.blr2.piesocket.com/v3/"+channelID+"?api_key=ujXx32mn0joYXVcT2j7Gp18c0JcbKTy3G6DE9FMB&notify_self=0", this);
        } else {
            // Nếu channel_id vẫn chưa được thiết lập, hiển thị thông báo hoặc xử lý lỗi tương ứng
            Log.e("WebSocket", "Error: Channel ID is null");
        }
    }

    @Override
    public void onMessageReceived(String message) {

    }

    @Override
    public void onConnectionStateChanged(boolean isConnected) {

    }
    private void changeData() {
        viewModel.setData("New data");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainActivity.showBottomNavigation(false);
    }

    public static String getCurrentDateTime() {
        // Định dạng cho ngày tháng năm và giờ
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        // Lấy thời gian hiện tại
        Date currentTime = new Date();
        // Định dạng thời gian hiện tại thành chuỗi
        String formattedDateTime = dateFormat.format(currentTime);
        return formattedDateTime;
    }
}