package com.jackiepenghe.tcpclientsample.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jackiepenghe.tcpclientsample.bean.ChatMess;
import com.jackiepenghe.tcpclientsample.R;

import java.util.List;

/**
 * Chat adapter
 * @author jackie
 */

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatHolder> {
    private List<ChatMess> chatMessList;
    private Context mContext;

    public ChatAdapter(Context mContext, List<ChatMess> chatMessList) {
        this.chatMessList = chatMessList;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChatHolder(LayoutInflater.from(mContext).inflate(R.layout.item_rv_chat, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHolder holder, int position) {
        ChatMess chatMess = chatMessList.get(position);
        if (chatMess.getType() == 1) {
            holder.tvSend.setVisibility(View.GONE);
            holder.tvRec.setVisibility(View.VISIBLE);
            holder.tvRec.setText(chatMess.getMesg());
        } else {
            holder.tvRec.setVisibility(View.GONE);
            holder.tvSend.setVisibility(View.VISIBLE);
            holder.tvSend.setText(chatMess.getMesg());
        }
    }

    @Override
    public int getItemCount() {
        return chatMessList.size();
    }

    static class ChatHolder extends RecyclerView.ViewHolder {

        TextView tvSend;
        TextView tvRec;

        ChatHolder(View itemView) {
            super(itemView);
            tvSend =  itemView.findViewById(R.id.tv_send);
            tvRec =  itemView.findViewById(R.id.tv_rec);
        }
    }
}
