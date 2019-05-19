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

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactHolder> {
  private List<Identity> contactList;
  private Context context;
  private DatabaseBackend databaseBackend;
  private AvatarWorkerTask avatarLoader;
  private OnOpenChatListener onOpenChatListener;

  public ContactAdapter(List<Identity> contactList, Context context) {
    this.contactList = contactList;
    this.context = context;
    this.databaseBackend = DatabaseBackend.getInstance(context);
    avatarLoader = new AvatarWorkerTask(context);
  }

  @Override
  public ContactHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

    View view = layoutInflater.inflate(R.layout.contact_list_row, parent, false);
    return new ContactHolder(view);
  }

  @Override
  public int getItemCount() {
    return contactList == null ? 0 : contactList.size();
  }

  @Override
  public void onBindViewHolder(@NonNull ContactHolder holder, final int position) {
    final Identity contact = contactList.get(position);

    // Set the data to the views here
    holder.setContactName(contact);
    holder.setPictrue(contact);
    Message last = databaseBackend.getLastMessageReceived(contact);

    if (last != null) {
      holder.setLastSeen(last);
      holder.setLastMessage(last);
    } else {
      holder.setNoLastMessage();
    }

    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (onOpenChatListener != null) {
          onOpenChatListener
              .onOpenChat(contact);
        }
      }
    });

  }

  public void setOnOpenChatListener(OnOpenChatListener listener) {
    this.onOpenChatListener = listener;
  }

  public interface OnOpenChatListener {
    void onOpenChat(Identity identity);
  }

  public class ContactHolder extends RecyclerView.ViewHolder {
    private TextView lbName;
    private TextView lbLastMessage;
    private TextView lbLastSeen;
    private ImageView ivContactPicture;

    public ContactHolder(View itemView) {
      super(itemView);
      lbName = itemView.findViewById(R.id.lbContactName);
      lbLastMessage = itemView.findViewById(R.id.lbLastMessage);
      lbLastSeen = itemView.findViewById(R.id.lbLastSeen);
      ivContactPicture = itemView.findViewById(R.id.ivContactPicture);
    }

    public void setContactName(Identity identity) {
      lbName.setText(identity.getUsername());
    }

    public void setPictrue(Identity identity) {
      if (!identity.getPicturePath().isEmpty()) {
        avatarLoader.displayImage(identity.getPicturePath(), ivContactPicture);
      }
    }

    public void setLastMessage(Message message) {
      if (!"".equals(message.getShortVersion())) {
        lbLastMessage.setText(message.getShortVersion());
      }
    }

    public void setLastSeen(Message message) {
      lbLastSeen.setText(message.getDate());
    }

    public void setNoLastMessage() {
      lbLastSeen.setText("");
      lbLastMessage.setText(R.string.clickHereToStartAConversation);
    }
  }
}
