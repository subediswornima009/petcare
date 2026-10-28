package com.swornima.petcare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    private List<Place> placeList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Place place);
        void onEditClick(Place place);
        void onDeleteClick(Place place);
    }

    public PlaceAdapter(Context context, List<Place> placeList, OnItemClickListener listener) {
        this.context = context;
        this.placeList = placeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Place place = placeList.get(position);
        holder.tvName.setText(place.getName());
        holder.tvType.setText(place.getType());
        holder.tvAddress.setText(place.getAddress());

        String details = "";
        if (place.getPhone() != null && !place.getPhone().trim().isEmpty()) {
            details = place.getPhone().trim();
        }
        if (place.getOpeningHours() != null && !place.getOpeningHours().trim().isEmpty()) {
            details += details.isEmpty() ? place.getOpeningHours().trim() : " - " + place.getOpeningHours().trim();
        }
        holder.tvDetails.setVisibility(details.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvDetails.setText(details);

        if (place.getNotes() == null || place.getNotes().trim().isEmpty()) {
            holder.tvNotes.setVisibility(View.GONE);
        } else {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(place.getNotes().trim());
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(place));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(place));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(place));
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvAddress, tvDetails, tvNotes;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvType = itemView.findViewById(R.id.tvPlaceType);
            tvAddress = itemView.findViewById(R.id.tvPlaceAddress);
            tvDetails = itemView.findViewById(R.id.tvPlaceDetails);
            tvNotes = itemView.findViewById(R.id.tvPlaceNotes);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
