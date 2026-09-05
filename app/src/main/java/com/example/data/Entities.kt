package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val password: String
)

@Entity(tableName = "grup_patroli")
data class GrupPatroli(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nama_grup") val namaGrup: String,
    val keterangan: String = ""
)

@Entity(tableName = "patroli")
data class Patroli(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "grup_id") val grupId: String?,
    val jam: String,
    val uraian: String,
    @ColumnInfo(name = "hari_tanggal") val hariTanggal: String,
    val shift: String,
    val petugas: String,
    @ColumnInfo(name = "ttd_pjt") val ttdPjt: String?, // Stores file path to PNG
    @ColumnInfo(name = "ttd_petugas") val ttdPetugas: String?, // Stores file path to PNG
    val keterangan: String = ""
)
