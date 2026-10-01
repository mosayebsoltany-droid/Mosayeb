package com.mose.assistant.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "legal_cases")
public class LegalCaseEntity {
    @PrimaryKey @NonNull public String id;
    @NonNull public String name;
    @NonNull public String domain;
    @NonNull public String matterType;
    @NonNull public String userRole;
    public String authority;
    public String opponent;
    public String claim;
    @NonNull public String workflowState;
    public long createdAt;
    public long updatedAt;

    public LegalCaseEntity(@NonNull String id,@NonNull String name,@NonNull String domain,@NonNull String matterType,@NonNull String userRole,@NonNull String workflowState,long createdAt,long updatedAt){
        this.id=id;this.name=name;this.domain=domain;this.matterType=matterType;this.userRole=userRole;this.workflowState=workflowState;this.createdAt=createdAt;this.updatedAt=updatedAt;
    }
}
