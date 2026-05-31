package com.example.data

import kotlinx.coroutines.flow.Flow

class SentHugRepository(private val sentHugDao: SentHugDao) {
    val allSentHugs: Flow<List<SentHug>> = sentHugDao.getAllSentHugs()

    suspend fun insert(hug: SentHug) {
        sentHugDao.insertSentHug(hug)
    }

    suspend fun deleteById(id: Int) {
        sentHugDao.deleteSentHugById(id)
    }

    suspend fun clear() {
        sentHugDao.clearAll()
    }
}
