package com.swornima.petcare;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PetPhotoAdapter extends RecyclerView.Adapter<PetPhotoAdapter.ViewHolder> {

    private final Context context;
    private final List<String> photoUris;
    private final OnPhotoActionListener listener;

    public interface OnPhotoActionListener {
        void onPhotoClick(String photoUri);
        void onDeleteClick(String photoUri);
    }

    public PetPhotoAdapter(Context context, List<String> photoUris, OnPhotoActionListener listener) {
        this.context = context;
        this.photoUris = photoUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String uri = photoUris.get(position);
        holder.ivPhoto.setImageResource(R.drawable.ic_pet);
        try {
            holder.ivPhoto.setImageURI(Uri.parse(uri));
        } catch (SecurityException | IllegalArgumentException e) {
            holder.ivPhoto.setImageResource(R.drawable.ic_pet);
        }
        holder.ivPhoto.setOnClickListener(v -> listener.onPhotoClick(uri));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(uri));
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivGalleryPhoto);
            btnDelete = itemView.findViewById(R.id.btnDeletePhoto);
        }
    }
}
