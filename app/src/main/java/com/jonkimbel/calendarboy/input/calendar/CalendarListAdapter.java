package com.jonkimbel.calendarboy.input.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jonkimbel.calendarboy.R;
import com.jonkimbel.calendarboy.model.Calendar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class CalendarListAdapter extends RecyclerView.Adapter<CalendarListAdapter.CalendarViewHolder> {
    private List<Calendar> calendars = new ArrayList<>();
    private ItemSelectedCallback callback;

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.calendar_list_item, parent, false);
        return new CalendarViewHolder(listItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        holder.bind(() -> {
            if (callback != null) {
                callback.onItemSelect(calendars.get(position).getId());
            }
        }, calendars.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return calendars.size();
    }

    void updateData(List<Calendar> calendars) {
        this.calendars = calendars;
        notifyDataSetChanged();
    }

    void setCallback(ItemSelectedCallback callback) {
        this.callback = callback;
    }

    interface ItemSelectedCallback {
        void onItemSelect(long calendarId);
    }

    class CalendarViewHolder extends RecyclerView.ViewHolder {
        CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void bind(Runnable onClick, CharSequence calendarName) {
            Button button = itemView.findViewById(R.id.item_select_button);
            button.setOnClickListener(view -> onClick.run());

            TextView nameView = itemView.findViewById(R.id.item_name);
            nameView.setText(calendarName);
        }
    }
}
