package br.jss.motoreviso.central;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import br.jss.motoreviso.R;

/** Notificação como o painel a enxerga. */
public class PanelNotification {

    public static class Action {
        public final int index;
        public final String title;
        Action(int index, String title) { this.index = index; this.title = title; }
    }

    public interface ActionClick { void onAction(PanelNotification n, Action a); }

    public final String key;
    public final String pkg;
    public final String app;
    public final String title;
    public final String text;
    public final long time;
    public final String category;
    public final List<Action> actions = new ArrayList<>();

    private PanelNotification(JSONObject o) {
        key = o.optString("key");
        pkg = o.optString("pkg");
        app = o.optString("app");
        title = o.optString("title");
        text = o.optString("text");
        time = o.optLong("time", System.currentTimeMillis());
        category = o.optString("category");
        JSONArray arr = o.optJSONArray("actions");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject a = arr.optJSONObject(i);
                if (a != null) actions.add(new Action(a.optInt("index"), a.optString("title")));
            }
        }
    }

    public static PanelNotification from(JSONObject o) { return new PanelNotification(o); }

    public boolean isCall() { return "call".equals(category); }
    public boolean isNavigation() { return "navigation".equals(category); }

    public String relativeTime() {
        long diff = System.currentTimeMillis() - time;
        if (diff < 60_000) return "agora";
        return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
    }

    /** Monta os botões de ação (grandes, para uso com luva). */
    public void bindActions(LinearLayout container, int heightPx, ActionClick click) {
        container.removeAllViews();
        container.setVisibility(actions.isEmpty() ? View.GONE : View.VISIBLE);
        Context ctx = container.getContext();
        for (int i = 0; i < actions.size(); i++) {
            Action a = actions.get(i);
            MaterialButton b = new MaterialButton(ctx);
            b.setText(a.title);
            b.setAllCaps(false);
            b.setTextSize(15);
            b.setMaxLines(1);
            b.setInsetTop(0);
            b.setInsetBottom(0);
            b.setCornerRadius(heightPx / 2);
            int bg = R.color.surface3;
            if (isCall()) {
                // A ordem dos botões varia entre discadores: decide pela legenda.
                String t = a.title.toLowerCase();
                if (t.contains("recus") || t.contains("rejeit") || t.contains("decline")
                        || t.contains("deslig") || t.contains("encerr") || t.contains("hang")) {
                    bg = R.color.error;
                } else if (t.contains("atend") || t.contains("aceit") || t.contains("answer")) {
                    bg = R.color.success;
                }
            }
            b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, bg)));
            b.setTextColor(ContextCompat.getColor(ctx, bg == R.color.surface3 ? R.color.text_primary : R.color.black));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, heightPx, 1f);
            if (i > 0) lp.setMarginStart(Math.round(8 * ctx.getResources().getDisplayMetrics().density));
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> click.onAction(this, a));
            container.addView(b, lp);
        }
    }
}
