package com.mose.assistant.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface LegalDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) void saveCase(LegalCaseEntity value);
    @Insert long addItem(CaseItemEntity value);
    @Insert long addDraft(LegalDraftEntity value);
    @Query("SELECT * FROM legal_cases WHERE id=:id LIMIT 1") LegalCaseEntity findCase(String id);
    @Query("SELECT * FROM case_items WHERE caseId=:caseId ORDER BY occurredAt, id") List<CaseItemEntity> items(String caseId);
    @Query("SELECT * FROM case_items WHERE caseId=:caseId AND kind=:kind ORDER BY id") List<CaseItemEntity> itemsByKind(String caseId,String kind);
    @Query("SELECT * FROM legal_drafts WHERE caseId=:caseId ORDER BY createdAt DESC") List<LegalDraftEntity> drafts(String caseId);
    @Query("SELECT * FROM legal_drafts WHERE caseId=:caseId ORDER BY id DESC LIMIT 1") LegalDraftEntity latestDraft(String caseId);
    @Query("SELECT COUNT(*) FROM case_items WHERE caseId=:caseId AND kind=:kind") int countItemsByKind(String caseId,String kind);
    @Query("SELECT COALESCE(MAX(version),0)+1 FROM legal_drafts WHERE caseId=:caseId AND draftType=:type") int nextDraftVersion(String caseId,String type);
    @Query("UPDATE legal_cases SET workflowState=:state,updatedAt=:time WHERE id=:caseId") void updateWorkflow(String caseId,String state,long time);
    @Query("UPDATE legal_drafts SET status=:status, validationReport=:report WHERE id=:draftId") void updateDraftStatus(long draftId,String status,String report);
    @Transaction default long saveNextDraft(String caseId,String type,String content,String validation,long time){
        LegalDraftEntity d=new LegalDraftEntity(caseId,type,"DRAFT",content,nextDraftVersion(caseId,type),time);
        d.validationReport=validation;return addDraft(d);
    }
}
