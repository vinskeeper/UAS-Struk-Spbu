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
- PostgreSQL dengan database `pombensin` (lihat bagian Database)

## Database

Repo menyediakan backup PostgreSQL `pombensin.sql` dalam format custom `pg_dump`. Backup ini berisi skema dan data, dan dibuat dari database `pombensin`. Aplikasi terhubung ke `localhost:5432/pombensin` menggunakan user `vinsky` dan password `1`, sesuai nilai di `src/main/java/dao/PostgreSQLConnection.java`.

Untuk menyiapkan database baru, pastikan role `vinsky` tersedia dengan password `1`. Jika belum ada, buat role dan database kosong:

```powershell
psql -U postgres -c "CREATE ROLE vinsky WITH LOGIN PASSWORD '1';"
createdb -U postgres -O vinsky pombensin
```

Jika role `vinsky` sudah ada, atur password-nya agar sesuai dengan koneksi aplikasi:

```powershell
psql -U postgres -c "ALTER ROLE vinsky WITH LOGIN PASSWORD '1';"
```

Restore backup ke database kosong sebagai `vinsky`:

```powershell
pg_restore -U vinsky -d pombensin --no-owner --no-privileges pombensin.sql
```

Perintah pertama hanya dijalankan jika role belum ada; kalau sudah ada, gunakan perintah `ALTER ROLE`. Jangan jalankan `createdb` jika database `pombensin` sudah ada. Pastikan perintah `psql`, `createdb`, dan `pg_restore` tersedia dari instalasi PostgreSQL. Jika username atau password lokal berbeda, ubah nilai `URL`, `USER`, dan `PASSWORD` pada `PostgreSQLConnection.java`.

Backup memuat data akun yang ada saat backup dibuat. Setelah restore, gunakan akun yang tersimpan di database tersebut untuk login.

## Menjalankan aplikasi

1. Buka folder proyek di VS Code dan tunggu ekstensi Java selesai mengimpor proyek.
2. Pilih **App (workspace biasa)** di Run and Debug. Untuk instalasi lokal yang punya junction `POMBensin-UAS` ke folder proyek, pilih **App**.
3. Tekan F5. Entry point adalah `src/main/java/App.java` dan kompilasi dikelola oleh ekstensi Java.
