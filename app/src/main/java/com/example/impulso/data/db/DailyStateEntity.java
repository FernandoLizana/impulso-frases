package com.example.impulso.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_state")
public class DailyStateEntity {
    @PrimaryKey
    @NonNull
    public String date = "";
    public String quoteId;
    public String source;
    public String actionStatus;
    public long actionUpdatedAt;
    @NonNull
    public String placeNote = "";
}
