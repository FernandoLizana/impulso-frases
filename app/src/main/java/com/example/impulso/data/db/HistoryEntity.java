package com.example.impulso.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String quoteKey;
    public String quoteId;
    public String source;
    public long viewedAt;
}
