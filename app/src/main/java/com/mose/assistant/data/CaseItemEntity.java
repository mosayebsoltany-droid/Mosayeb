package com.mose.assistant.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName="case_items",
        foreignKeys=@ForeignKey(entity=LegalCaseEntity.class,parentColumns="id",childColumns="caseId",onDelete=ForeignKey.CASCADE),
        indices={@Index("caseId"),@Index(value={"caseId","kind"})})
public class CaseItemEntity {
    @PrimaryKey(autoGenerate=true) public long id;
    @NonNull public String caseId;
    @NonNull public String kind;
    @NonNull public String title;
    @NonNull public String content;
    public String sourceUri;
    @NonNull public String verificationStatus;
    public long occurredAt;
    public long createdAt;

    public CaseItemEntity(@NonNull String caseId,@NonNull String kind,@NonNull String title,@NonNull String content,@NonNull String verificationStatus,long occurredAt,long createdAt){
        this.caseId=caseId;this.kind=kind;this.title=title;this.content=content;this.verificationStatus=verificationStatus;this.occurredAt=occurredAt;this.createdAt=createdAt;
    }
}
