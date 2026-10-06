package com.motocar.obdscanner;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    public interface OnDeviceClickListener {
        void onConnect(BluetoothDevice device);
    }

    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final OnDeviceClickListener listener;

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void setDevices(List<BluetoothDevice> newDevices) {
        this.devices.clear();
        if (newDevices != null) {
            this.devices.addAll(newDevices);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        String name = device.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Perangkat Tak Dikenal";
        }
        holder.tvName.setText(name);
        holder.tvAddress.setText(device.getAddress());

        holder.btnConnect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConnect(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        MaterialButton btnConnect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvAddress = itemView.findViewById(R.id.tv_device_address);
            btnConnect = itemView.findViewById(R.id.btn_device_connect);
        }
    }
}
