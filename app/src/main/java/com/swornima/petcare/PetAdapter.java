package com.swornima.petcare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Adapter for the pet list shown on the "My Pets" screen and the recent
 * pets section of the Home screen.
 */
public class PetAdapter extends RecyclerView.Adapter<PetAdapter.ViewHolder> {

    private Context context;
    private List<Pet> petList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(Pet pet);
        void onDeleteClick(Pet pet);
        void onItemClick(Pet pet);
    }

    public PetAdapter(Context context, List<Pet> petList, OnItemClickListener listener) {
        this.context = context;
        this.petList = petList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pet pet = petList.get(position);
        holder.tvName.setText(pet.getName());
        holder.tvDescription.setText(pet.getBreed());

        if (pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
            holder.ivImage.setColorFilter(null);
            Picasso.get().load(pet.getImageUrl()).placeholder(R.drawable.logo_icon).into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.logo_icon);
        }

        holder.btnStart.setOnClickListener(v -> listener.onItemClick(pet));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(pet));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(pet));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(pet));
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription;
        ImageView ivImage, btnEdit, btnDelete;
        MaterialButton btnStart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPetName);
            tvDescription = itemView.findViewById(R.id.tvPetBreed);
            ivImage = itemView.findViewById(R.id.ivPetImage);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnStart = itemView.findViewById(R.id.btnStart);
        }
    }
}
