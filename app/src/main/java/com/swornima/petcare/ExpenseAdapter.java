package com.swornima.petcare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    private final Context context;
    private final List<Expense> expenseList;
    private final OnItemActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnItemActionListener {
        void onItemClick(Expense expense);
        void onEditClick(Expense expense);
        void onDeleteClick(Expense expense);
    }

    public ExpenseAdapter(Context context, List<Expense> expenseList, OnItemActionListener listener) {
        this.context = context;
        this.expenseList = expenseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvDescription.setText(expense.getDescription());
        holder.tvCategory.setText(expense.getCategory());
        holder.tvMeta.setText(expense.getPetName() + " - " + dateFormat.format(expense.getDateMillis()));
        holder.tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", expense.getAmount()));
        holder.ivPetImage.setImageResource(R.drawable.logo_icon);

        if (expense.getPetImageUrl() != null && !expense.getPetImageUrl().trim().isEmpty()) {
            Picasso.get()
                    .load(expense.getPetImageUrl())
                    .placeholder(R.drawable.logo_icon)
                    .error(R.drawable.logo_icon)
                    .into(holder.ivPetImage);
        }

        if (expense.getNotes() == null || expense.getNotes().trim().isEmpty()) {
            holder.tvNotes.setVisibility(View.GONE);
        } else {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(expense.getNotes().trim());
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(expense));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(expense));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(expense));
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvCategory, tvAmount, tvMeta, tvNotes;
        ImageView ivPetImage, btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvExpenseDescription);
            tvCategory = itemView.findViewById(R.id.tvExpenseCategory);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvMeta = itemView.findViewById(R.id.tvExpenseMeta);
            tvNotes = itemView.findViewById(R.id.tvExpenseNotes);
            ivPetImage = itemView.findViewById(R.id.ivPetImage);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
