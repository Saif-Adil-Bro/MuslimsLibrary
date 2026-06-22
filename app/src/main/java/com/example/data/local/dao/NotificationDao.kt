package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)
    
    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 'global' ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE (userId = :userId OR userId = 'global') AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(userId: String): Flow<List<NotificationEntity>>
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId OR userId = 'global'")
    suspend fun markAllAsRead(userId: String)
    
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: String)
    
    @Query("DELETE FROM notifications WHERE userId = :userId OR userId = 'global'")
    suspend fun deleteAllNotifications(userId: String)
}
