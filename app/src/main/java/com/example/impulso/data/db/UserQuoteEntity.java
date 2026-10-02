package com.example.impulso.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_quotes")
public class UserQuoteEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public String text;
    public String category;
    public long createdAt;
    public long updatedAt;
}
