# Git Branch Commands

Tài liệu này giải thích các câu lệnh `git branch` thường dùng khi làm việc với nhánh trong Git.

## 1. Branch là gì?

Branch là một nhánh phát triển độc lập trong repository.

Ví dụ:

- `main`: nhánh chính của project.
- `develop`: nhánh phát triển chung.
- `feature/login`: nhánh làm tính năng đăng nhập.
- `fix/payment-error`: nhánh sửa lỗi thanh toán.

Branch giúp bạn làm tính năng mới hoặc sửa lỗi mà không ảnh hưởng trực tiếp đến code chính.

## 2. Xem danh sách branch

```bash
git branch
```

Lệnh này hiển thị danh sách branch local.

Ví dụ output:

```bash
  develop
* main
  feature/login
```

Dấu `*` cho biết bạn đang đứng ở branch nào.

Trong ví dụ trên, branch hiện tại là `main`.

## 3. Xem tất cả branch local và remote

```bash
git branch -a
```

Lệnh này hiển thị cả branch local và branch remote.

Ví dụ:

```bash
* main
  feature/login
  remotes/origin/main
  remotes/origin/develop
```

Trong đó:

| Loại branch | Ý nghĩa |
| --- | --- |
| `main` | Branch local |
| `feature/login` | Branch local |
| `remotes/origin/main` | Branch trên remote `origin` |
| `remotes/origin/develop` | Branch trên remote `origin` |

## 4. Tạo branch mới

```bash
git branch feature/login
```

Lệnh này tạo branch mới tên là `feature/login`.

Lưu ý: lệnh này chỉ tạo branch, chưa chuyển sang branch đó.

Sau khi tạo, bạn vẫn đang đứng ở branch hiện tại.

## 5. Chuyển sang branch khác

```bash
git switch feature/login
```

Hoặc dùng cú pháp cũ:

```bash
git checkout feature/login
```

Lệnh này chuyển bạn sang branch `feature/login`.

Bạn có thể kiểm tra branch hiện tại bằng:

```bash
git branch
```

## 6. Tạo branch mới và chuyển sang branch đó

Cách khuyến nghị:

```bash
git switch -c feature/login
```

Cú pháp cũ:

```bash
git checkout -b feature/login
```

Lệnh này vừa tạo branch `feature/login`, vừa chuyển sang branch đó.

Đây là cách thường dùng nhất khi bắt đầu làm một tính năng mới.

## 7. Đổi tên branch

Đổi tên branch hiện tại:

```bash
git branch -m new-branch-name
```

Ví dụ:

```bash
git branch -m main
```

Lệnh này đổi tên branch hiện tại thành `main`.

Đổi tên một branch cụ thể:

```bash
git branch -m old-name new-name
```

Ví dụ:

```bash
git branch -m master main
```

Lệnh này đổi tên branch `master` thành `main`.

## 8. Xóa branch local

Xóa branch đã merge:

```bash
git branch -d feature/login
```

`-d` là cách xóa an toàn. Git sẽ không cho xóa nếu branch đó chưa được merge.

Xóa bắt buộc:

```bash
git branch -D feature/login
```

`-D` sẽ xóa branch ngay cả khi branch đó chưa được merge.

Cần cẩn thận khi dùng `-D`, vì bạn có thể mất commit nếu commit đó chưa nằm ở branch khác.

## 9. Xem branch kèm commit mới nhất

```bash
git branch -v
```

Ví dụ output:

```bash
  develop       8ab12cd Add user API
* main          3f91abc Initial commit
  feature/login 7d22ef1 Add login form
```

Lệnh này giúp xem mỗi branch đang trỏ tới commit nào.

## 10. Xem branch đã merge

```bash
git branch --merged
```

Lệnh này hiển thị các branch đã được merge vào branch hiện tại.

Thường dùng trước khi xóa branch:

```bash
git branch --merged
git branch -d feature/login
```

## 11. Xem branch chưa merge

```bash
git branch --no-merged
```

Lệnh này hiển thị các branch chưa được merge vào branch hiện tại.

Nên kiểm tra lệnh này trước khi xóa branch bằng `-D`.

## 12. Push branch mới lên remote

Sau khi tạo branch local, bạn có thể push lên remote:

```bash
git push -u origin feature/login
```

Ý nghĩa:

| Thành phần | Ý nghĩa |
| --- | --- |
| `git push` | Đẩy code lên remote |
| `-u` | Thiết lập upstream cho branch |
| `origin` | Tên remote |
| `feature/login` | Tên branch cần push |

Sau khi dùng `-u`, những lần sau bạn chỉ cần:

```bash
git push
```

hoặc:

```bash
git pull
```

Git sẽ tự hiểu branch local đang liên kết với branch remote nào.

## 13. Xóa branch trên remote

```bash
git push origin --delete feature/login
```

Lệnh này xóa branch `feature/login` trên remote `origin`.

Lưu ý: lệnh này không xóa branch local trên máy bạn.

Nếu muốn xóa cả branch local:

```bash
git branch -d feature/login
```

## 14. Workflow thường gặp khi làm feature mới

```bash
git switch main
git pull
git switch -c feature/login
```

Sau khi code xong:

```bash
git add .
git commit -m "Add login feature"
git push -u origin feature/login
```

Sau đó bạn có thể tạo Pull Request hoặc Merge Request trên GitHub/GitLab.

## 15. Ghi nhớ nhanh

| Lệnh | Ý nghĩa |
| --- | --- |
| `git branch` | Xem branch local |
| `git branch -a` | Xem tất cả branch local và remote |
| `git branch branch-name` | Tạo branch mới |
| `git switch branch-name` | Chuyển sang branch khác |
| `git switch -c branch-name` | Tạo branch mới và chuyển sang branch đó |
| `git branch -m new-name` | Đổi tên branch hiện tại |
| `git branch -d branch-name` | Xóa branch local an toàn |
| `git branch -D branch-name` | Xóa branch local bắt buộc |
| `git branch -v` | Xem branch kèm commit mới nhất |
| `git branch --merged` | Xem branch đã merge |
| `git branch --no-merged` | Xem branch chưa merge |
| `git push -u origin branch-name` | Push branch mới lên remote |
| `git push origin --delete branch-name` | Xóa branch trên remote |

## Kết luận

`git branch` dùng để quản lý nhánh trong Git.

Các thao tác chính gồm:

1. Xem branch.
2. Tạo branch.
3. Đổi tên branch.
4. Xóa branch.
5. Kiểm tra branch đã merge hoặc chưa merge.

Khi làm việc thực tế, bạn thường kết hợp `git branch` với `git switch`, `git push`, `git pull` và Pull Request.
