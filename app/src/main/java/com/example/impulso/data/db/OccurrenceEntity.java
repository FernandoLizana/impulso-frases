package com.example.impulso.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "occurrences")
public class OccurrenceEntity {
    @PrimaryKey
    @NonNull
    public String occurrenceKey = "";
    public String quoteId;
    public String source;
    public long scheduledAtEpoch;
    public String status;
}
