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

/**
 * Adapter used both for the standalone "Manage Care Tasks" list and for
 * showing tasks selected while building/editing a pet routine.
 */
public class CareTaskAdapter extends RecyclerView.Adapter<CareTaskAdapter.ViewHolder> {

    private final List<CareTask> careTaskList;
    private final Context context;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(CareTask careTask);
        void onEditClick(CareTask careTask);
        void onDeleteClick(CareTask careTask);
    }

    public CareTaskAdapter(Context context, List<CareTask> careTaskList, OnItemClickListener listener) {
        this.context = context;
        this.careTaskList = careTaskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_care_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CareTask careTask = careTaskList.get(position);
        holder.tvName.setText(careTask.getName());
        holder.tvCategory.setText(careTask.getCategory());
        holder.tvDetails.setText(buildDetails(careTask));

        if (careTask.getNotes() == null || careTask.getNotes().trim().isEmpty()) {
            holder.tvNotes.setVisibility(View.GONE);
        } else {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(careTask.getNotes());
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(careTask));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(careTask));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(careTask));
    }

    @Override
    public int getItemCount() {
        return careTaskList.size();
    }

    private String buildDetails(CareTask careTask) {
        String details = "";
        details = append(details, careTask.getTime());
        details = append(details, careTask.getFrequency());
        details = append(details, careTask.getSupplies());
        details = append(details, label("Vet", careTask.getAppointmentDate()));
        details = append(details, label("Grooming", careTask.getGroomingDate()));
        details = append(details, label("Vaccine", careTask.getVaccinationDueDate()));
        if (hasText(careTask.getMedicationStartDate()) || hasText(careTask.getMedicationEndDate())) {
            details = append(details, "Meds: " + safe(careTask.getMedicationStartDate())
                    + " to " + safe(careTask.getMedicationEndDate()));
        }
        return details;
    }

    private String append(String existing, String value) {
        if (!hasText(value)) return existing;
        return existing.isEmpty() ? value.trim() : existing + " - " + value.trim();
    }

    private String label(String label, String value) {
        return hasText(value) ? label + ": " + value.trim() : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return hasText(value) ? value.trim() : "not set";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvNotes, tvDetails;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
