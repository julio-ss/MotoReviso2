package br.jss.motoreviso.central;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.jss.motoreviso.R;

public class PanelNotificationAdapter extends RecyclerView.Adapter<PanelNotificationAdapter.VH> {

    public interface Callbacks extends PanelNotification.ActionClick {
        void onDismiss(PanelNotification n);
    }

    private final List<PanelNotification> items = new ArrayList<>();
    private final Map<String, Bitmap> icons;
    private final Callbacks callbacks;
    private final int actionHeight;

    public PanelNotificationAdapter(Map<String, Bitmap> icons, int actionHeight, Callbacks callbacks) {
        this.icons = icons;
        this.actionHeight = actionHeight;
        this.callbacks = callbacks;
    }

    public void submit(List<PanelNotification> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public PanelNotification get(int position) { return items.get(position); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_painel_notificacao, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PanelNotification n = items.get(position);
        Bitmap icon = icons.get(n.pkg);
        if (icon != null) h.icon.setImageBitmap(icon);
        else h.icon.setImageResource(R.drawable.ic_painel_bell);
        h.app.setText(n.app + " · " + n.relativeTime());
        h.title.setText(n.title);
        h.text.setText(n.text);
        h.text.setVisibility(n.text.isEmpty() ? View.GONE : View.VISIBLE);
        n.bindActions(h.actions, actionHeight, callbacks);
        h.dismiss.setOnClickListener(v -> callbacks.onDismiss(n));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView app, title, text;
        final LinearLayout actions;
        final ImageButton dismiss;

        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.iv_icon);
            app = v.findViewById(R.id.tv_app);
            title = v.findViewById(R.id.tv_title);
            text = v.findViewById(R.id.tv_text);
            actions = v.findViewById(R.id.actions);
            dismiss = v.findViewById(R.id.btn_dismiss);
        }
    }
}
