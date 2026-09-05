package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUser(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
}

@Dao
interface GrupPatroliDao {
    @Query("SELECT * FROM grup_patroli ORDER BY id DESC")
    fun getAllGrupFlow(): Flow<List<GrupPatroli>>

    @Query("SELECT * FROM grup_patroli ORDER BY id DESC")
    suspend fun getAllGrup(): List<GrupPatroli>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGrup(grup: GrupPatroli): Long

    @Query("SELECT * FROM grup_patroli WHERE id = :id LIMIT 1")
    suspend fun getGrupById(id: Int): GrupPatroli?

    @Query("SELECT * FROM grup_patroli WHERE nama_grup = :namaGrup LIMIT 1")
    suspend fun getGrupByName(namaGrup: String): GrupPatroli?
}

@Dao
interface PatroliDao {
    @Query("SELECT * FROM patroli ORDER BY id DESC")
    fun getAllPatroliFlow(): Flow<List<Patroli>>

    @Query("SELECT * FROM patroli ORDER BY id DESC")
    suspend fun getAllPatroli(): List<Patroli>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatroli(patroli: Patroli): Long

    @Update
    suspend fun updatePatroli(patroli: Patroli)

    @Delete
    suspend fun deletePatroli(patroli: Patroli)

    @Query("SELECT * FROM patroli WHERE id = :id LIMIT 1")
    suspend fun getPatroliById(id: Int): Patroli?

    @Query("SELECT * FROM patroli WHERE hari_tanggal = :hariTanggal AND grup_id = :grupId ORDER BY jam ASC")
    suspend fun getPatroliByDateAndGroup(hariTanggal: String, grupId: String): List<Patroli>

    @Query("SELECT * FROM patroli WHERE petugas LIKE :query OR uraian LIKE :query ORDER BY id DESC")
    suspend fun searchPatroli(query: String): List<Patroli>
}
