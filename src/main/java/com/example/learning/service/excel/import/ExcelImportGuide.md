# Import Excel

## Muc tieu

So sanh 2 API import Excel:

- `Apache POI`
- `FastExcel`

Hai API nay doc toan bo row trong file upload.
Khong co logic fix cung 5k row o phan import.

App khong tu do benchmark trong code nua.
Phan do se do:

- `Spring Boot Actuator`
- `Prometheus`
- `Grafana`

## Thanh phan chinh

Table:

- `excel_import_records`

Code chinh:

- `ExcelImportController`
- `ExcelImportService`
- `ExcelImportRecordEntity`
- `ExcelImportRecordRepository`

## Endpoint

Tai template:

- `GET /api/v1/excel-import/template`

Lay cac moc sample ho tro:

- `GET /api/v1/excel-import/sample-files`

Tai file sample theo so row:

- `GET /api/v1/excel-import/sample-files/{rowCount}`

Import bang POI:

- `POST /api/v1/excel-import/poi`

Import bang FastExcel:

- `POST /api/v1/excel-import/fastexcel`

Metrics scrape:

- `GET /actuator/prometheus`

## Cac moc sample dang ho tro

- `20000`
- `50000`
- `100000`
- `200000`
- `500000`
- `700000`
- `1000000`

## Cach tai sample

### 1. Lay danh sach moc

```powershell
curl.exe http://localhost:8085/api/v1/excel-import/sample-files
```

### 2. Tai file sample

```powershell
curl.exe -L "http://localhost:8085/api/v1/excel-import/sample-files/50000" --output excel-import-sample-50000.xlsx
```

## Cach test import

### 1. Goi API POI

```powershell
curl.exe -X POST "http://localhost:8085/api/v1/excel-import/poi?resetTable=true" -F "file=@excel-import-sample-50000.xlsx"
```

### 2. Goi API FastExcel

```powershell
curl.exe -X POST "http://localhost:8085/api/v1/excel-import/fastexcel?resetTable=true" -F "file=@excel-import-sample-50000.xlsx"
```

### 3. Xem trong Grafana

Vao dashboard `Excel Import Benchmark`.

## Du doan RAM de tham khao

Bang nay la du doan thuc dung cho bo sample hien tai:

- 8 cot du lieu don gian
- string ngan
- khong formula
- khong style phuc tap
- 1 sheet

Day khong phai cam ket chinh xac. So thuc te se doi theo:

- POI hay FastExcel
- heap size JVM
- GC
- PostgreSQL insert batch
- traffic nen trong app
- Docker host va RAM may

Uoc luong tang them cua process trong luc import:

- `20000 row`: thuong thay tang them khoang `20 MB` den `60 MB`
- `50000 row`: thuong thay tang them khoang `50 MB` den `140 MB`
- `100000 row`: thuong thay tang them khoang `100 MB` den `280 MB`
- `200000 row`: thuong thay tang them khoang `220 MB` den `550 MB`
- `500000 row`: thuong thay tang them khoang `500 MB` den `1.2 GB`
- `700000 row`: thuong thay tang them khoang `750 MB` den `1.6 GB`
- `1000000 row`: thuong thay tang them khoang `1.0 GB` den `2.2 GB`

Neu app dung kieu doc tat ca vao `List<ExcelImportRecordEntity>` roi moi `saveAll`, thi:

- `POI` thuong la diem nong RAM ro nhat
- `FastExcel` co the doc nhe hon o pha parse, nhung tong RAM van cao vi ban van giu toan bo rows trong memory

## Goc nhin dung khi benchmark

Muon benchmark co y nghia:

- dung cung mot file cho POI va FastExcel
- chi chay 1 API tai 1 thoi diem
- dung `resetTable=true` neu muon so cong bang
- chay moi API 3 den 5 lan
- so sanh `avg`, `p95`, va xu huong heap toan process

Khong nen doc panel heap nhu "RAM rieng cua API".
Do la heap cua ca JVM process.


http://localhost:8085/api/v1/excel-import/fastexcel?resetTable=true
+ RAM: 266MB all service
+ time: 54.73 s

chunk:
+ RAM: 170MB all service
+ time: 54.81 s

http://localhost:8085/api/v1/excel-import/poi?resetTable=true with Workbook workbook = new XSSFWorkbook(inputStream)
+ RAM: 1.2GB do load toàn bộ sheet lên RAM
+ time: 1 m 8.94 s

http://localhost:8085/api/v1/excel-import/poi-sax?resetTable=true
+ RAM: 289MB
+ time: 58.68 s

chunk
+ RAM: 300MB
+ time: 55.85 s



