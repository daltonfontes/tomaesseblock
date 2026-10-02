package com.tomaesseblock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {
    @Query("SELECT * FROM block_rules ORDER BY created_at DESC")
    fun observeAll(): Flow<List<BlockRuleEntity>>

    @Query("SELECT * FROM block_rules")
    suspend fun getAll(): List<BlockRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: BlockRuleEntity): Long

    @Delete
    suspend fun delete(rule: BlockRuleEntity)

    @Query("DELETE FROM block_rules WHERE pattern = :pattern AND type = 'EXACT'")
    suspend fun deleteExact(pattern: String)
}

@Dao
interface SpamReportDao {
    @Insert
    suspend fun insert(report: SpamReportEntity): Long

    @Query(
        "SELECT category, COUNT(*) AS total FROM spam_reports WHERE number = :number " +
            "GROUP BY category ORDER BY total DESC",
    )
    suspend fun countsFor(number: String): List<CategoryCount>

    @Query("DELETE FROM spam_reports WHERE number = :number")
    suspend fun clear(number: String)

    @Query("SELECT COUNT(DISTINCT number) FROM spam_reports")
    fun observeReportedNumbers(): Flow<Int>
}

@Dao
interface CallEventDao {
    @Query("SELECT * FROM call_events ORDER BY timestamp DESC LIMIT 500")
    fun observeRecent(): Flow<List<CallEventEntity>>

    @Query("SELECT COUNT(*) FROM call_events WHERE blocked = 1")
    fun observeBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_events WHERE blocked = 1 AND timestamp >= :since")
    fun observeBlockedSince(since: Long): Flow<Int>

    @Insert
    suspend fun insert(event: CallEventEntity): Long

    @Query("DELETE FROM call_events")
    suspend fun clear()
}
