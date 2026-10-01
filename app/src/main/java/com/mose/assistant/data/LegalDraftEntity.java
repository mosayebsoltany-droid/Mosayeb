package com.mose.assistant.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName="legal_drafts",
        foreignKeys=@ForeignKey(entity=LegalCaseEntity.class,parentColumns="id",childColumns="caseId",onDelete=ForeignKey.CASCADE),
        indices={@Index("caseId"),@Index(value={"caseId","draftType","version"},unique=true)})
public class LegalDraftEntity {
    @PrimaryKey(autoGenerate=true) public long id;
    @NonNull public String caseId;
    @NonNull public String draftType;
    @NonNull public String status;
    @NonNull public String content;
    public int version;
    public String validationReport;
    public long createdAt;

    public LegalDraftEntity(@NonNull String caseId,@NonNull String draftType,@NonNull String status,@NonNull String content,int version,long createdAt){
        this.caseId=caseId;this.draftType=draftType;this.status=status;this.content=content;this.version=version;this.createdAt=createdAt;
    }
}
