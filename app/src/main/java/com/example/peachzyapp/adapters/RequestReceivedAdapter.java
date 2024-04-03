package com.example.peachzyapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peachzyapp.R;
import com.example.peachzyapp.entities.FriendItem;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RequestReceivedAdapter extends RecyclerView.Adapter<RequestReceivedAdapter.FriendViewHolder>{

    private List<FriendItem> listFriend;
    public ImageView avatarImageView;


    public RequestReceivedAdapter(List<FriendItem> mListFriend) {
        this.listFriend = mListFriend;
    }

    @Override
    public int getItemCount() {
        if(listFriend!=null)
        {
            return listFriend.size();
        }
        return 0;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_request_received_adapter, parent, false);
        return new FriendViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendItem friends= listFriend.get(position);
        if(friends==null){
            return;
        }
        holder.tvFriend.setText(friends.getName());
       Picasso.get().load(friends.getAvatar()).into(avatarImageView);

    }


    public class FriendViewHolder extends RecyclerView.ViewHolder{
        public TextView tvFriend;
       // public ImageView avatarImageView;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriend= itemView.findViewById(R.id.tv_friend);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
        }
    }
}
