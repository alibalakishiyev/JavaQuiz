package com.ali.pymain.taskmanager.library;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * AutoCompleteAdapter - PyCharm stilində autocomplete siyahısı
 * İki sətir: əsas label (icon + ad) + alt mətn (tip + açıqlama)
 */
public class AutoCompleteAdapter extends ArrayAdapter<AutoCompleteEngine.CompletionItem> {

    // Tip rəngləri
    private static final int COLOR_KEYWORD = Color.parseColor("#82AAFF");  // Mavi
    private static final int COLOR_BUILTIN = Color.parseColor("#C3E88D");  // Yaşıl
    private static final int COLOR_MODULE  = Color.parseColor("#FFCB6B");  // Sarı
    private static final int COLOR_SNIPPET = Color.parseColor("#F07178");  // Qırmızı
    private static final int COLOR_METHOD  = Color.parseColor("#89DDFF");  // Açıq mavi
    private static final int COLOR_DEFAULT = Color.parseColor("#EEFFFF");  // Ağ

    private final List<AutoCompleteEngine.CompletionItem> items;

    public AutoCompleteAdapter(Context context,
                               List<AutoCompleteEngine.CompletionItem> items) {
        super(context, android.R.layout.simple_list_item_2, items);
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            holder = new ViewHolder();
            holder.text1 = convertView.findViewById(android.R.id.text1);
            holder.text2 = convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);

            convertView.setPadding(24, 12, 24, 12);
            convertView.setBackgroundColor(Color.parseColor("#2D3748"));
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AutoCompleteEngine.CompletionItem item = items.get(position);
        if (item == null) return convertView;

        // ── Əsas sətir: icon + label ─────────────────────────────────
        holder.text1.setText(item.getDisplayText());
        holder.text1.setTextColor(getTypeColor(item.type));
        holder.text1.setTextSize(14f);

        // ── İkinci sətir: tip + açıqlama + params ────────────────────
        holder.text2.setText(item.getSubText());
        holder.text2.setTextColor(Color.parseColor("#718096"));
        holder.text2.setTextSize(11f);

        return convertView;
    }

    private int getTypeColor(String type) {
        if (type == null) return COLOR_DEFAULT;
        switch (type) {
            case "keyword": return COLOR_KEYWORD;
            case "builtin": return COLOR_BUILTIN;
            case "module":  return COLOR_MODULE;
            case "snippet": return COLOR_SNIPPET;
            case "method":  return COLOR_METHOD;
            default:        return COLOR_DEFAULT;
        }
    }

    private static class ViewHolder {
        TextView text1;
        TextView text2;
    }
}