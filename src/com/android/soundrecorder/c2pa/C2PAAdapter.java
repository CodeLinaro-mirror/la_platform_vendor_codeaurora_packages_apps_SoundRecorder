/**
 * Copyright (c) 2024, 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.soundrecorder.c2pa;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.soundrecorder.C2PAActivity;
import com.android.soundrecorder.R;

import java.util.ArrayList;

public class C2PAAdapter extends RecyclerView.Adapter<C2PAAdapter.C2PAViewHolder> {

    private final ArrayList<C2PAActivity.Item> mItems;
    private final Context mContext;

    public C2PAAdapter(ArrayList<C2PAActivity.Item> items, Context context) {
        mItems = items;
        mContext = context;
    }

    @NonNull
    @Override
    public C2PAViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.c2pa_details, parent, false);
        return new C2PAViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull C2PAViewHolder holder, int position) {
        C2PAActivity.Item item = mItems.get(position);
        holder.title.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        holder.progress.setVisibility(View.VISIBLE);
        if (item.getThumbnail() != null) {
            holder.thumbnail.setImageBitmap(item.getThumbnail());
        } else {
            int imageResource = mContext.getResources().getIdentifier(
                    "@drawable/no_thumbnail_audio", null, mContext.getPackageName());
            holder.thumbnail.setImageResource(imageResource);
        }
        if (item.getAddress() != null) {
            holder.location_label.setVisibility(View.VISIBLE);
            holder.location_text.setVisibility(View.VISIBLE);
            holder.location_text.setText(item.getAddress());
        } else {
            holder.location_label.setVisibility(View.GONE);
            holder.location_text.setVisibility(View.GONE);
        }
        holder.thumbnail_type.setText(item.getTypeLabel());
        holder.captured_with_label.setText(item.getCapturedWithLabel());
        holder.captured_with_text.setText(item.getCapturedWith());
        holder.captured_label.setText(item.getCapturedLabel());
        holder.ai_warning.setVisibility((item.isAiGenerated() && (
                !item.getDescriptor().equals("Original"))) ? View.VISIBLE : View.GONE);
        if (item.getModifications() > 0) {
            holder.modifications_label.setVisibility(View.VISIBLE);
            holder.modifications_text.setVisibility(View.VISIBLE);
            holder.modifications_text.setText(String.valueOf(item.getModifications()));
            holder.signed_with_text.setVisibility(View.GONE);
        } else {
            holder.modifications_label.setVisibility(View.GONE);
            holder.modifications_text.setVisibility(View.GONE);
        }

        holder.captured_text.setText(item.getCapturedDateText());
        holder.signed_by_text.setText(item.getSignedByText());
        holder.progress.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class C2PAViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private ImageView thumbnail;
        private TextView thumbnail_desc;
        private TextView thumbnail_type;
        private TextView ai_warning;
        private TextView captured_label;
        private TextView captured_text;
        private TextView location_label;
        private TextView location_text;
        private TextView captured_with_label;
        private TextView captured_with_text;
        private TextView modifications_label;
        private TextView modifications_text;
        private TextView signed_by_label;
        private TextView signed_by_text;
        private TextView signed_with_label;
        private TextView signed_with_text;
        private ProgressBar progress;


        public C2PAViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            thumbnail_type = itemView.findViewById(R.id.thumbnail_type);
            ai_warning = itemView.findViewById(R.id.ai_warning);
            captured_label = itemView.findViewById(R.id.captured_label);
            captured_text = itemView.findViewById(R.id.captured_text);
            location_label = itemView.findViewById(R.id.location_label);
            location_text = itemView.findViewById(R.id.location_text);
            captured_with_label = itemView.findViewById(R.id.captured_with_label);
            captured_with_text = itemView.findViewById(R.id.captured_with_text);
            modifications_label = itemView.findViewById(R.id.modifications_label);
            modifications_text = itemView.findViewById(R.id.modifications_text);
            signed_by_label = itemView.findViewById(R.id.signed_by_label);
            signed_by_text = itemView.findViewById(R.id.signed_by_text);
            progress = itemView.findViewById(R.id.progress);
        }
    }
}
