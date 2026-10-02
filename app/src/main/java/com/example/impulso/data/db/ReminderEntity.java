package com.example.impulso.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class ReminderEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public int hour;
    public int minute;
    public int daysMask;
    public boolean enabled;
    public String kind;
}
