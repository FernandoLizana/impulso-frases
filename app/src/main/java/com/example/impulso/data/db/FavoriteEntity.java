package com.example.impulso.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteEntity {
    @PrimaryKey
    @NonNull
    public String quoteKey = "";
    public String quoteId;
    public String source;
    public long createdAt;
}
