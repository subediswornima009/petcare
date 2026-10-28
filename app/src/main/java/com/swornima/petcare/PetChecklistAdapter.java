package com.swornima.petcare;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for the auto-generated care checklist shown on the Pet Detail
 * screen. Ticking a row marks the task done for the current care period.
 */
public class PetChecklistAdapter extends RecyclerView.Adapter<PetChecklistAdapter.ViewHolder> {

    private final Context context;
    private final List<CareTask> careTaskList;
    private final OnCompletionListener listener;

    public interface OnCompletionListener {
        void onCheckChanged(CareTask careTask, boolean isChecked);
    }

    public PetChecklistAdapter(Context context, List<CareTask> careTaskList, OnCompletionListener listener) {
        this.context = context;
        this.careTaskList = careTaskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_checklist_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CareTask careTask = careTaskList.get(position);

        holder.tvName.setText(careTask.getName());
        holder.tvDetails.setText(buildDetails(careTask));
        holder.tvNotes.setText(careTask.getNotes());

        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(careTask.isCompleted());
        updateStrikeThrough(holder.tvName, careTask.isCompleted());

        holder.cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            careTask.setCompleted(isChecked);
            careTask.setCompletedAtMillis(isChecked ? System.currentTimeMillis() : 0);
            updateStrikeThrough(holder.tvName, isChecked);
            listener.onCheckChanged(careTask, isChecked);
        });

        holder.ivInfo.setOnClickListener(v -> {
            if (holder.tvNotes.getVisibility() == View.VISIBLE) {
                holder.tvNotes.setVisibility(View.GONE);
            } else {
                holder.tvNotes.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return careTaskList.size();
    }

    public void replaceTasks(List<CareTask> tasks) {
        careTaskList.clear();
        if (tasks != null) {
            careTaskList.addAll(tasks);
        }
        notifyDataSetChanged();
    }

    private void updateStrikeThrough(TextView tv, boolean isCompleted) {
        if (isCompleted) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    private String buildDetails(CareTask careTask) {
        String details = "";
        details = append(details, careTask.getCategory());
        details = append(details, careTask.getTime());
        details = append(details, careTask.getFrequency());
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
        CheckBox cbCompleted;
        TextView tvName, tvDetails, tvNotes;
        ImageView ivInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvDetails = itemView.findViewById(R.id.tvTaskDetails);
            tvNotes = itemView.findViewById(R.id.tvTaskNotes);
            ivInfo = itemView.findViewById(R.id.ivTaskInfo);
        }
    }
}
