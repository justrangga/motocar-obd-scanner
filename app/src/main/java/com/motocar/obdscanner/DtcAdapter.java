package com.motocar.obdscanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DtcAdapter extends RecyclerView.Adapter<DtcAdapter.ViewHolder> {

    private final List<DtcDatabase.DtcInfo> items = new ArrayList<>();

    public void setDtcCodes(List<String> codes) {
        items.clear();
        if (codes != null) {
            for (String code : codes) {
                items.add(DtcDatabase.lookup(code));
            }
        }
        notifyDataSetChanged();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dtc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DtcDatabase.DtcInfo info = items.get(position);
        holder.tvCode.setText(info.code);
        holder.tvTitle.setText(info.title);
        holder.tvDesc.setText(info.description);
        holder.tvSeverity.setText(info.severity);

        if ("Kritis".equalsIgnoreCase(info.severity)) {
            holder.tvSeverity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.neon_red));
        } else if ("Sedang".equalsIgnoreCase(info.severity)) {
            holder.tvSeverity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.neon_amber));
        } else {
            holder.tvSeverity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.neon_green));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode;
        TextView tvTitle;
        TextView tvDesc;
        TextView tvSeverity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_dtc_code);
            tvTitle = itemView.findViewById(R.id.tv_dtc_title);
            tvDesc = itemView.findViewById(R.id.tv_dtc_desc);
            tvSeverity = itemView.findViewById(R.id.tv_dtc_severity);
        }
    }
}
