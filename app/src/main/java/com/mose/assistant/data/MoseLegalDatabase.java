package com.mose.assistant.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities={LegalCaseEntity.class,CaseItemEntity.class,LegalDraftEntity.class},version=1,exportSchema=false)
public abstract class MoseLegalDatabase extends RoomDatabase {
    public abstract LegalDao legalDao();
    private static volatile MoseLegalDatabase INSTANCE;
    public static MoseLegalDatabase get(Context context){
        if(INSTANCE==null)synchronized(MoseLegalDatabase.class){
            if(INSTANCE==null)INSTANCE=Room.databaseBuilder(context.getApplicationContext(),MoseLegalDatabase.class,"mose_legal.db").build();
        }
        return INSTANCE;
    }
}
