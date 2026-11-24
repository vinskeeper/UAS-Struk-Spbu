# UAS Struk SPBU

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

Repo menyediakan backup PostgreSQL bernama `pombensin.sql`. File ini berformat custom dari `pg_dump` dan berisi struktur serta data saat backup dibuat. Untuk menggunakannya, buat database kosong bernama `pombensin`, lalu restore dari terminal:

```powershell
createdb -U postgres pombensin
pg_restore -U postgres -d pombensin --no-owner --no-privileges pombensin.sql
```

Sesuaikan `postgres` dengan username PostgreSQL lokal. Pastikan `pg_restore` tersedia melalui instalasi PostgreSQL.

Aplikasi membaca pengaturan koneksi dari `database/local.properties` jika file itu tersedia. Buat folder `database` dan file tersebut dengan isi berikut, lalu ganti username dan password sesuai PostgreSQL lokal:

```properties
url=jdbc:postgresql://localhost:5432/pombensin
user=postgres
password=
```

Jika file konfigurasi belum dibuat, aplikasi memakai database `pombensin`, user `postgres`, dan password kosong.

Untuk database baru tanpa akun, jalankan konfigurasi **Create First Admin** di VS Code dan ikuti prompt terminal. Setelah login, Admin bisa menambah akun dan produk dari aplikasi.

## Menjalankan aplikasi

1. Buka folder proyek di VS Code dan tunggu ekstensi Java selesai mengimpor proyek.
2. Pilih **App (workspace biasa)** di Run and Debug. Untuk instalasi lokal yang punya junction `POMBensin-UAS` ke folder proyek, pilih **App**.
3. Tekan F5. Entry point adalah `src/main/java/App.java` dan kompilasi dikelola oleh ekstensi Java.
