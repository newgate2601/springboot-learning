# Git Init vs Git Remote Add

Tài liệu này giải thích chi tiết 2 câu lệnh Git thường dùng khi bắt đầu một project và đưa code lên GitHub/GitLab.

## 1. `git init`

`git init` dùng để khởi tạo Git repository trong thư mục hiện tại.

```bash
git init
```

Sau khi chạy lệnh này, Git sẽ tạo một thư mục ẩn tên là `.git/`. Thư mục này chứa toàn bộ dữ liệu Git cần để theo dõi lịch sử thay đổi của project.

Ví dụ:

```bash
mkdir my-project
cd my-project
git init
```

Sau đó bạn có thể dùng các lệnh Git cơ bản:

```bash
git status
git add .
git commit -m "Initial commit"
```

Nói ngắn gọn, `git init` biến một thư mục bình thường thành một Git repository local trên máy của bạn.

## 2. `git remote add`

`git remote add` dùng để thêm địa chỉ repository ở xa, thường là GitHub, GitLab hoặc Bitbucket.

```bash
git remote add origin https://github.com/username/my-project.git
```

Ý nghĩa từng phần:

| Thành phần | Ý nghĩa |
| --- | --- |
| `git` | Gọi chương trình Git |
| `remote` | Làm việc với repository ở xa |
| `add` | Thêm remote mới |
| `origin` | Tên alias của remote, thường dùng cho remote chính |
| URL | Địa chỉ repository trên GitHub/GitLab |

Sau khi thêm remote, bạn có thể kiểm tra bằng lệnh:

```bash
git remote -v
```

Ví dụ output:

```bash
origin  https://github.com/username/my-project.git (fetch)
origin  https://github.com/username/my-project.git (push)
```

Sau đó bạn có thể đẩy code lên remote:

```bash
git push -u origin main
```

## So sánh nhanh

| Lệnh | Mục đích |
| --- | --- |
| `git init` | Khởi tạo Git repository local |
| `git remote add` | Kết nối repository local với repository remote |
| `git init` | Tạo thư mục `.git/` |
| `git remote add` | Lưu địa chỉ remote vào config Git |
| `git init` | Dùng khi project chưa được Git quản lý |
| `git remote add` | Dùng khi muốn push/pull với GitHub, GitLab, Bitbucket |

## Workflow thường gặp

Khi bạn có một project mới và muốn đưa lên GitHub:

```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/username/my-project.git
git push -u origin main
```

Giải thích:

1. `git init`: khởi tạo Git trong project.
2. `git add .`: đưa toàn bộ file vào staging area.
3. `git commit -m "Initial commit"`: tạo commit đầu tiên.
4. `git branch -M main`: đổi tên branch hiện tại thành `main`.
5. `git remote add origin ...`: liên kết project local với repository online.
6. `git push -u origin main`: đẩy code lên branch `main` của remote `origin`.

## Ghi nhớ

`git init` là tạo kho Git trên máy local.

`git remote add` là gắn địa chỉ kho Git online để có thể push và pull code.
