package com.tomaesseblock.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tomaesseblock.domain.BlockRule
import com.tomaesseblock.domain.RuleType

@Entity(
    tableName = "block_rules",
    indices = [Index(value = ["pattern", "type"], unique = true)],
)
data class BlockRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    /** Nome do enum [RuleType]. */
    val type: String,
    val label: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
) {
    fun toDomain() = BlockRule(id = id, pattern = pattern, type = RuleType.valueOf(type), label = label)
}

@Entity(tableName = "spam_reports", indices = [Index("number")])
data class SpamReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    /** Nome do enum SpamCategory. */
    val category: String,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "call_events", indices = [Index("timestamp")])
data class CallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val timestamp: Long = System.currentTimeMillis(),
    val blocked: Boolean,
    /** Nome do enum BlockReason quando bloqueada. */
    val reason: String?,
    val label: String?,
)

/** Resultado agregado de denúncias por categoria. */
data class CategoryCount(
    val category: String,
    val total: Int,
)
