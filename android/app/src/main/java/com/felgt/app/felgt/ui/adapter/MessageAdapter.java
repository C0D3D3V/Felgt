package com.felgt.app.felgt.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.felgt.app.felgt.R;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.entities.Message;
import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.ui.util.AvatarWorkerTask;

import java.util.List;
import java.util.UUID;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageHolder> {
  private List<Message> messageList;
  private Context context;
  private DatabaseBackend databaseBackend;
  private AvatarWorkerTask avatarLoader;
  private OnClickOnMessage onClickOnMessage;
  private Identity partner;

  public MessageAdapter(List<Message> messageList, Identity partner, Context context) {
    this.partner = partner;
    this.messageList = messageList;
    this.context = context;
    this.databaseBackend = DatabaseBackend.getInstance(context);
    avatarLoader = new AvatarWorkerTask(context);
  }

  public void setNewIdentity(Identity identity) {
    this.partner = identity;
  }

  @Override
  public int getItemViewType(int position) {
    final Message message = messageList.get(position);

    if (!message.getSender().equals(new UUID(0, 0).toString())) {
      return 0;

    }
    return 1;
  }

  @Override
  public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    View view = null;
    switch (viewType) {
      case 0:
        view = layoutInflater.inflate(R.layout.item_message_received, parent, false);
        break;
      case 1:
        view = layoutInflater.inflate(R.layout.item_message_sent, parent, false);
        break;
    }

    return new MessageHolder(view);
  }

  @Override
  public int getItemCount() {
    return messageList == null ? 0 : messageList.size();
  }

  @Override
  public void onBindViewHolder(@NonNull MessageHolder holder, final int position) {
    final Message message = messageList.get(position);

    // Set the data to the views here
    holder.setMessageBody(message);
    holder.setMessageTime(message);

    if (!message.getSender().equals(new UUID(0, 0).toString())) {
      holder.setPictrue(partner);
      holder.setMessageSender(partner);
    }

    holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        if (onClickOnMessage != null) {
          onClickOnMessage
              .onClickMessage(message, view, position);
        }
        return true;
      }
    });

  }

  public void setOnClickOnMessage(OnClickOnMessage listener) {
    this.onClickOnMessage = listener;
  }

  public interface OnClickOnMessage {
    void onClickMessage(Message message, View view, final int position);
  }

  public class MessageHolder extends RecyclerView.ViewHolder {
    private TextView lbMessageBody;
    private TextView lbMessageSender;
    private TextView lbMessageTime;
    private ImageView ivSenderPicture;

    public MessageHolder(View itemView) {
      super(itemView);
      lbMessageBody = itemView.findViewById(R.id.lbMessageBody);
      lbMessageSender = itemView.findViewById(R.id.lbMessageSender);
      lbMessageTime = itemView.findViewById(R.id.lbMessageTime);
      ivSenderPicture = itemView.findViewById(R.id.ivSenderPicture);
    }

    public void setMessageSender(Identity identity) {
      if (lbMessageSender != null) {
        lbMessageSender.setText(identity.getUsername());
      }
    }

    public void setPictrue(Identity identity) {
      if (!identity.getPicturePath().isEmpty() && ivSenderPicture != null) {
        avatarLoader.displayImage(identity.getPicturePath(), ivSenderPicture);
      }
    }

    public void setMessageTime(Message message) {
      lbMessageTime.setText(message.getDate());
    }

    public void setMessageBody(Message message) {
      lbMessageBody.setText(message.getMessage());
    }

  }
}
