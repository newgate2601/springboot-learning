# Monitoring Stack

## Stack

- Spring Boot Actuator
- Prometheus
- Grafana

## Cach chay

Tu root project:

```powershell
docker compose up --build -d
```

## URL

- App API: `http://localhost:8085`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Database

Container app dang dung PostgreSQL tren may local qua:

- `jdbc:postgresql://host.docker.internal:5432/file_performance`

Ban can dam bao PostgreSQL local:

- dang chay
- mo cong `5432`
- co database `file_performance`
- user/password khop voi `docker-compose.yml`

## Grafana login

- Username: `admin`
- Password: `admin`

## Dashboard

Grafana tu provision dashboard:

- `Excel Import Benchmark`

## Muc sample dang ho tro

API sample sinh dong theo `rowCount`.
Backend dang ho tro cac moc:

- `20000`
- `50000`
- `100000`
- `200000`
- `500000`
- `700000`
- `1000000`

Lay danh sach moc:

```powershell
curl.exe http://localhost:8085/api/v1/excel-import/sample-files
```

Tai file sample:

```powershell
curl.exe -L "http://localhost:8085/api/v1/excel-import/sample-files/20000" --output excel-import-sample-20000.xlsx
```

## Cach test import va xem benchmark

1. Mo `http://localhost:3000`
2. Vao dashboard `Excel Import Benchmark`
3. Tai file sample theo moc can test, vi du `100000`
4. Goi API import bang POI:

```powershell
curl.exe -X POST "http://localhost:8085/api/v1/excel-import/poi?resetTable=true" `
  -F "file=@excel-import-sample-100000.xlsx"
```

5. Goi API import bang FastExcel:

```powershell
curl.exe -X POST "http://localhost:8085/api/v1/excel-import/fastexcel?resetTable=true" `
  -F "file=@excel-import-sample-100000.xlsx"
```

6. Quay lai Grafana de xem so lieu

## Ghi chu quan trong

- Hai API import doc toan bo so row co trong file upload, khong bi fix cung 5k
- Prometheus scrape app qua `http://api-service:8086/actuator/prometheus`
- App ben trong container dung datasource tro toi PostgreSQL local qua `host.docker.internal`
- App khong con tu do benchmark bang code; phan do do Prometheus + Grafana dam nhiem
