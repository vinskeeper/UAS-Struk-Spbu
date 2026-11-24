# POM Bensin

Aplikasi desktop JavaFX untuk mengelola akun petugas, produk BBM, transaksi pengisian, riwayat, dan invoice.

## Fitur

- Login Admin/Petugas dengan verifikasi password BCrypt dan hak akses berdasarkan role.
- Admin dapat mengelola akun, produk BBM, dan transaksi.
- Petugas hanya dapat membuka tab Transaksi.
- Admin dapat membuat akun Admin atau Petugas; admin aktif terakhir tidak dapat dinonaktifkan atau dihapus.
- Kelola akun: tambah, ubah, aktifkan, nonaktifkan, dan hapus jika belum terhubung ke transaksi.
- Kelola produk bensin dan solar, termasuk harga, oktan/cetane, status aktif, dan penghapusan jika belum dipakai.
- Catat pengisian dengan pembayaran tunai, QRIS, atau debit.
- Hitung total dan kembalian tunai.
- Cari, lihat, cetak, dan hapus riwayat transaksi.
- Buat nomor invoice berdasarkan ID transaksi, misalnya `SPBU-000001`.
- Gunakan font Cooper yang disertakan di proyek.

## Kebutuhan

- JDK 26
- JavaFX SDK di `C:\javafx-sdk\lib`
- jBCrypt di `C:\javafx-sdk\lib\jbcrypt-0.4.jar`
- PostgreSQL JDBC driver di `lib/postgresql-42.7.14.jar`
- Database PostgreSQL `pombensin` yang sudah disiapkan

## Database

Buat database kosong bernama `pombensin`, lalu muat [skema PostgreSQL](database/pombensin_schema.sql):

```powershell
psql -d pombensin -v ON_ERROR_STOP=1 -f database/pombensin_schema.sql
```

Buat `database/local.properties` dari `database/local.properties.example`, lalu isi username dan password PostgreSQL lokal. Jalankan aplikasi dari folder proyek agar file tersebut terbaca. Skema mencakup tabel dan relasi tanpa data dari database pengembangan.

Untuk membuat akun Admin pertama, jalankan konfigurasi **Create First Admin** di VS Code dan ikuti prompt terminal. Setelah login, Admin bisa menambah akun dan produk melalui aplikasi.

## Menjalankan aplikasi

1. Buka folder proyek di VS Code dan tunggu ekstensi Java selesai mengimpor proyek.
2. Pilih **App (workspace biasa)** di Run and Debug. Untuk instalasi lokal yang punya junction `POMBensin-UAS` ke folder proyek, pilih **App**.
3. Tekan F5. Entry point adalah `src/main/java/App.java` dan kompilasi dikelola oleh ekstensi Java.
