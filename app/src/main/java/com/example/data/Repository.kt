package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val database: AppDatabase) {
    private val userDao = database.userDao()
    private val grupPatroliDao = database.grupPatroliDao()
    private val patroliDao = database.patroliDao()

    suspend fun getUser(username: String): User? = userDao.getUser(username)
    suspend fun insertUser(user: User) = userDao.insertUser(user)

    val allGrup: Flow<List<GrupPatroli>> = grupPatroliDao.getAllGrupFlow()
    suspend fun getAllGrup(): List<GrupPatroli> = grupPatroliDao.getAllGrup()
    suspend fun insertGrup(grup: GrupPatroli): Long = grupPatroliDao.insertGrup(grup)
    suspend fun getGrupById(id: Int): GrupPatroli? = grupPatroliDao.getGrupById(id)
    suspend fun getGrupByName(namaGrup: String): GrupPatroli? = grupPatroliDao.getGrupByName(namaGrup)

    val allPatroli: Flow<List<Patroli>> = patroliDao.getAllPatroliFlow()
    suspend fun getAllPatroli(): List<Patroli> = patroliDao.getAllPatroli()
    suspend fun insertPatroli(patroli: Patroli): Long = patroliDao.insertPatroli(patroli)
    suspend fun updatePatroli(patroli: Patroli) = patroliDao.updatePatroli(patroli)
    suspend fun deletePatroli(patroli: Patroli) = patroliDao.deletePatroli(patroli)
    suspend fun getPatroliById(id: Int): Patroli? = patroliDao.getPatroliById(id)
    suspend fun getPatroliByDateAndGroup(hariTanggal: String, grupId: String): List<Patroli> =
        patroliDao.getPatroliByDateAndGroup(hariTanggal, grupId)
    suspend fun searchPatroli(query: String): List<Patroli> = patroliDao.searchPatroli(query)
}
