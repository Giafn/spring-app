# Spring App — Product & Auth API

REST API untuk manajemen produk dan autentikasi, dikerjakan berdasarkan spesifikasi

Fitur:

- **Produk**: CRUD dengan upload multi-file gambar, filter (search, kategori), pagination, cache Redis
- **Autentikasi**: Register, Login (JWT access + refresh token), rate limiting
- **Kategori**: Dropdown kategori (terintegrasi dengan produk via foreign key)
- **Seeder**: Data awal kategori & admin user, dijalankan saat migrasi Flyway

## Prerequisites

- Java 21+
- PostgreSQL
- Redis
- Maven (atau gunakan `./mvnw`)

## Setup

### 1. Clone & masuk direktori

```bash
git clone <repo-url>
cd spring-app
```

### 2. Konfigurasi environment

Salin `.env.example` ke `.env` dan sesuaikan:

```bash
cp .env.example .env
```

Isi file `.env`:

```env
DB_NAME=nama_database
DB_USERNAME=user_postgres
DB_PASSWORD=password_postgres
JWT_SECRET=minimal-32-karakter-untuk-hs256
```

### 3. Buat database

```sql
CREATE DATABASE spring_app_db;
```

### 4. Jalankan migrasi Flyway

Migrasi sudah dikonfigurasi di `src/main/resources/application.properties` dan memakai script di `src/main/resources/db/migration/`.

Urutan migrasi saat ini:

- `V1__create_users_table.sql`
- `V2__create_refresh_tokens_table.sql`
- `V3__create_categories_table.sql`
- `V4__create_products_table.sql`
- `V5__create_product_images_table.sql`
- `V6__seed_initial_data.sql`

```bash
make migrate
```

Atau manual:

```bash
mvn flyway:migrate -Dflyway.user=... -Dflyway.password=...
```

Jika database memakai kredensial default dari `.env`, `make migrate` cukup. Target `migrate-manual` di Makefile setara dengan `migrate`.

## Seeder Data Awal

Seeder dijalankan lewat migrasi Flyway.

Data yang di-seed:

| Data | Detail |
|------|--------|
| **Kategori** | Clothes, Electronics, Food, Furniture |
| **Admin user** | Username: `admin`, Password: `admin123` |

Seeder idempoten — aman dijalankan berulang (tidak duplikasi data).

## Menjalankan Aplikasi

```bash
make run
```

Atau:

```bash
source .env && mvn spring-boot:run
```

Aplikasi berjalan di `http://localhost:8080`.

## Perintah Lain

| Perintah | Kegunaan |
|----------|----------|
| `make compile` | Kompilasi kode |
| `make test` | Jalankan unit test |
| `make migrate` | Jalankan migrasi database |
| `make migrate-info` | Lihat status migrasi |

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

OpenAPI spec: `http://localhost:8080/v3/api-docs`

### Autentikasi di Swagger

1. Akses `/api/auth/login` untuk mendapatkan token
2. Klik **Authorize** (tombol di kanan atas Swagger UI)
3. Masukkan token: `Bearer <accessToken>`
4. Sekarang endpoint yang terproteksi bisa diakses dari Swagger

### Endpoints

| Method | Path | Auth | Rate Limit | Catatan |
|--------|------|------|------------|---------|
| POST | `/api/auth/register` | - | 3x / 60 detik | Register user baru |
| POST | `/api/auth/login` | - | 3x / 60 detik | Login, dapatkan JWT |
| POST | `/api/auth/refresh` | - | - | Refresh token |
| GET | `/api/categories` | Bearer | - | Dropdown kategori produk |
| GET | `/api/products` | Bearer | - | List produk (filter: `?search=`, `?categoryId=`, `?page=&size=`) |
| GET | `/api/products/{id}` | Bearer | - | Detail produk |
| POST | `/api/products` | Bearer | 1x / 5 detik | Create produk (multipart: `product` JSON + `images` files) |
| PUT | `/api/products/{id}` | Bearer | 1x / 5 detik | Update produk (multipart) |
| DELETE | `/api/products/{id}` | Bearer | 1x / 5 detik | Hapus produk |

### Struktur Response

Semua response menggunakan `ApiResponse<T>` sehingga konsisten.:

```json
// Sukses
{ "code": 200, "status": "success", "message": "...", "data": {...}, "timestamp": "..." }

// Error validasi
{ "code": 400, "status": "Validation Failed", "message": "Invalid request", "data": { "field": "error" }, "timestamp": "..." }

// Not Found
{ "code": 404, "status": "Not Found", "message": "Product not found with id: 99", "timestamp": "..." }

// Rate Limited
{ "code": 429, "status": "Too Many Requests", "message": "...", "timestamp": "..." }
```

### File Upload

Endpoint `POST` dan `PUT` `/api/products` menerima **multipart/form-data**:

- **`product`** — JSON string berisi `title`, `price`, `description`, `categoryId`
- **`images`** — satu atau lebih file gambar (maks 10MB per file, 50MB total request)

File disimpan di `uploads/` dan bisa diakses via `/uploads/{filename}`.

## Struktur Folder

```
src/main/java/com/tecnicaltest/spring_app/
├── config/           # Konfigurasi (Security, Redis, OpenAPI, Web)
├── dto/              # DTO shared (ApiResponse)
├── entity/           # JPA entities (Product, Category, ProductImage, User, RefreshToken)
├── exception/        # Custom exceptions + global handler
├── feature/
│   ├── auth/         # Autentikasi JWT (register, login, refresh, filter, utils)
│   ├── product/      # CRUD produk (controller, dto, service)
├── repository/       # Spring Data JPA repositories
└── utils/            # Utility global (FileStorageService)
```

```
src/main/resources/
├── db/migration/     # Script migrasi Flyway (V1-V6)
└── application.properties
```

## Tech Stack

- Java 21, Spring Boot 4.x
- PostgreSQL, H2 (test)
- Redis (caching)
- Flyway (migrasi database)
- Spring Security + JWT (jjwt 0.12.x)
- SpringDoc OpenAPI (Swagger UI)
- Lombok
