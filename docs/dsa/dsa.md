# Data Structures & Algorithms

## Tài liệu tham khảo

- [Google Doc gốc - Data structure](https://docs.google.com/document/d/1_MMvesgeAO0a3vor7xXQdAzXaWpwukJ9C6GUTFhrBCY/edit?tab=t.0)
- [Java Collections Framework](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html)
- [ArrayList - Java API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html)
- [HashSet - Java API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashSet.html)
- [LinkedList - Java API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html)
- [PriorityQueue - Java API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html)
- [GeeksforGeeks - HashSet in Java](https://www.geeksforgeeks.org/hashset-in-java/)
- [GeeksforGeeks - Internal working of HashSet in Java](https://www.geeksforgeeks.org/internal-working-of-sethashset-in-java/)

## DSA Overview

🙂 **Data Structures & Algorithms (DSA)** là nền tảng để tổ chức dữ liệu và xử lý dữ liệu hiệu quả.

- **Data structure** trả lời câu hỏi: dữ liệu nên lưu theo dạng nào?
- **Algorithm** trả lời câu hỏi: xử lý dữ liệu đó bằng các bước nào?
- **Complexity** trả lời câu hỏi: khi input lớn lên, chương trình chậm/tốn memory đến mức nào?

Ví dụ:

- Muốn truy cập phần tử theo index nhanh: dùng Array/ArrayList.
- Muốn kiểm tra phần tử có tồn tại nhanh: dùng HashSet/HashMap.
- Muốn lấy phần tử nhỏ nhất/lớn nhất liên tục: dùng Heap/PriorityQueue.
- Muốn duyệt quan hệ đường đi: dùng Graph + BFS/DFS.
- Muốn tối ưu bài toán có trạng thái lặp lại: dùng Dynamic Programming.

## Big-O Notation (bổ sung)

Big-O dùng để mô tả tốc độ tăng của thời gian chạy hoặc bộ nhớ khi kích thước input `n` tăng lên.

| Big-O | Tên thường gọi | Ví dụ |
| --- | --- | --- |
| `O(1)` | Constant | Truy cập `arr[i]`, push vào stack |
| `O(log n)` | Logarithmic | Binary search |
| `O(n)` | Linear | Duyệt toàn bộ array |
| `O(n log n)` | Linearithmic | Merge sort, heap sort |
| `O(n^2)` | Quadratic | Hai vòng lặp lồng nhau, bubble sort |
| `O(2^n)` | Exponential | Một số bài recursion thử mọi subset |
| `O(n!)` | Factorial | Thử mọi hoán vị |

Quy tắc đọc nhanh:

- Bỏ constant: `O(2n)` -> `O(n)`.
- Giữ phần tăng nhanh nhất: `O(n^2 + n)` -> `O(n^2)`.
- Với nested loop độc lập thường là nhân: `O(n * m)`.
- Với các bước chạy nối tiếp thường là cộng rồi rút gọn.

Ví dụ:

```java
for (int x : nums) {
    System.out.println(x);
}
```

Độ phức tạp thời gian là `O(n)` vì phải duyệt qua `n` phần tử.

```java
for (int i = 0; i < nums.length; i++) {
    for (int j = 0; j < nums.length; j++) {
        System.out.println(nums[i] + nums[j]);
    }
}
```

Độ phức tạp là `O(n^2)` vì với mỗi `i`, vòng `j` lại chạy `n` lần.

## Array vs ArrayList

### Array (Static Array)

Array là cấu trúc dữ liệu lưu các phần tử theo vùng nhớ liên tiếp, có kích thước cố định sau khi tạo.

Đặc điểm:

- Kích thước cố định, không tự tăng/giảm sau khi khởi tạo.
- Trong Java, array có thể lưu primitive value như `int`, `char`, `double` hoặc object reference như `String`, `User`.
- Không hỗ trợ generic theo cách như `List<T>`.
- Random access theo index là `O(1)`.
- Không hỗ trợ insert/delete làm thay đổi size; chỉ có thể ghi đè giá trị hoặc gán `null` với object array.
- Nếu khai báo quá lớn nhưng dùng ít, memory bị lãng phí.

Ví dụ:

```java
int[] numbers = new int[3];
numbers[0] = 10;
numbers[1] = 20;
numbers[2] = 30;

System.out.println(numbers[1]); // 20
```

Vì các phần tử nằm liên tiếp, máy tính có thể tính địa chỉ của phần tử theo công thức:

```text
address(arr[i]) = base_address + i * element_size
```

Ví dụ nếu `arr[0]` có địa chỉ giả định là `1000`, mỗi `int` chiếm 4 byte:

```text
arr[0] -> 1000
arr[1] -> 1004
arr[2] -> 1008
```

Lưu ý trong Java: mình không trực tiếp thao tác địa chỉ bộ nhớ như C/C++, nhưng concept vùng nhớ liên tiếp vẫn giúp hiểu vì sao array access theo index nhanh.

### ArrayList (Dynamic Array)

`ArrayList` là dynamic array trong Java. Bên trong nó dùng một array để lưu phần tử, nhưng cung cấp API để tự tăng capacity khi cần.

Đặc điểm:

- Size động, có thể thêm/xóa phần tử.
- Chỉ lưu object reference; nếu dùng primitive như `int`, Java sẽ autoboxing sang `Integer`.
- Random access theo index là `O(1)` vì bên trong vẫn là array.
- Insert/delete ở giữa hoặc đầu list là `O(n)` vì phải dịch chuyển phần tử.
- Khi capacity không đủ, `ArrayList` tạo array mới lớn hơn rồi copy reference từ array cũ sang array mới.
- Array cũ sau đó có thể được Garbage Collector thu hồi nếu không còn reference.

Ví dụ:

```java
List<Integer> numbers = new ArrayList<>();
numbers.add(10);
numbers.add(20);
numbers.add(1, 15); // chèn vào index 1

System.out.println(numbers); // [10, 15, 20]
```

Trong Java hiện đại, `ArrayList` khi khởi tạo rỗng không cấp ngay array size 10; nó dùng empty array chung và chỉ cấp capacity mặc định khi phần tử đầu tiên được thêm vào. Khi tăng capacity, implementation phổ biến tăng khoảng 1.5 lần capacity cũ.

### So sánh Array và ArrayList

| Tiêu chí | Array | ArrayList |
| --- | --- | --- |
| Size | Cố định | Động |
| Lưu primitive | Có | Không trực tiếp, cần wrapper |
| Generic | Không support generic như collection | Có `ArrayList<T>` |
| Random access | `O(1)` | `O(1)` |
| Insert/delete giữa list | Không hỗ trợ đổi size | `O(n)` |
| Resize | Không resize | Tự resize khi thiếu capacity |
| Overhead | Ít hơn | Nhiều hơn vì wrapper/API/capacity |

Tư duy chọn:

- Dùng array khi size cố định, cần hiệu năng/memory tốt, hoặc làm bài thuật toán.
- Dùng `ArrayList` khi cần collection tiện lợi, size thay đổi, API giàu hơn.

## Set

`Set` là data structure trong đó các element là duy nhất, không cho phép trùng giá trị.

Trong Java, các implementation phổ biến:

- `HashSet`
- `LinkedHashSet`
- `TreeSet`

### HashSet

`HashSet` là implementation phổ biến nhất của `Set`. Bên trong `HashSet` dùng `HashMap`; element của set được lưu như key trong map, còn value là một object constant dùng chung.

Đặc điểm:

- Không đảm bảo thứ tự duyệt.
- Dùng `hashCode()` và `equals()` để xác định element trùng.
- Search/insert/delete trung bình là `O(1)`.
- Không thread-safe.
- Default capacity thường là 16, load factor mặc định là 0.75.
- Khi số lượng element vượt ngưỡng capacity * load factor, HashMap bên trong sẽ resize.

Ví dụ:

```java
Set<String> tags = new HashSet<>();
tags.add("java");
tags.add("spring");
tags.add("java");

System.out.println(tags.size()); // 2
```

Khi dùng object tự định nghĩa trong `HashSet`, cần override đúng `equals()` và `hashCode()`.

```java
class User {
    private Long id;
    private String email;

    // equals() và hashCode() nên dùng cùng field logic
}
```

### LinkedHashSet

`LinkedHashSet` giống `HashSet` nhưng duy trì thứ tự insert.

Đặc điểm:

- Không cho phép trùng.
- Dùng `equals()` và `hashCode()`.
- Duy trì insertion order.
- Search/insert/delete trung bình là `O(1)`.
- Chậm hơn `HashSet` một chút vì phải duy trì linked list nội bộ.

Ví dụ:

```java
Set<String> names = new LinkedHashSet<>();
names.add("A");
names.add("C");
names.add("B");

System.out.println(names); // [A, C, B]
```

### TreeSet

`TreeSet` lưu element theo thứ tự sắp xếp. Bên trong nó dựa trên `TreeMap`, thường là red-black tree.

Đặc điểm:

- Không cho phép trùng theo kết quả compare.
- Duy trì thứ tự tự nhiên hoặc theo `Comparator`.
- Search/insert/delete là `O(log n)`.
- Dùng `compareTo()` hoặc `Comparator.compare()` để sắp xếp và xác định trùng.

Ví dụ:

```java
Set<Integer> numbers = new TreeSet<>();
numbers.add(3);
numbers.add(1);
numbers.add(2);

System.out.println(numbers); // [1, 2, 3]
```

So sánh nhanh:

| Implementation | Thứ tự | Trung bình search/insert/delete | Khi nào dùng |
| --- | --- | --- | --- |
| `HashSet` | Không đảm bảo | `O(1)` | Cần unique và tốc độ |
| `LinkedHashSet` | Theo thứ tự insert | `O(1)` | Cần unique và giữ thứ tự thêm |
| `TreeSet` | Sorted | `O(log n)` | Cần unique và dữ liệu luôn sorted |

Performance thường gặp:

```text
HashSet > LinkedHashSet > TreeSet
```

Nhưng đây chỉ là quy tắc gần đúng. Chọn data structure nên dựa vào yêu cầu thứ tự, uniqueness và loại thao tác chính.

## Linked List

Linked List là data structure được hình thành bởi chuỗi node liên kết với nhau qua reference/pointer.

Một node cơ bản gồm:

```text
value + next
```

Singly linked list:

```text
head -> [10|next] -> [20|next] -> [30|null]
```

Đặc điểm:

- Không yêu cầu vùng nhớ liên tiếp như array.
- Truy cập phần tử thứ `k` là `O(n)` vì phải đi từ head qua từng node.
- Insert/delete ở đầu list là `O(1)`.
- Insert/delete ở giữa là `O(n)` nếu chưa có reference tới node cần thao tác.
- Tốn thêm memory để lưu reference/pointer.
- Có thể dùng làm nền tảng để triển khai stack, queue, deque.

Ví dụ node tự viết:

```java
class Node {
    int value;
    Node next;

    Node(int value) {
        this.value = value;
    }
}
```

### Doubly Linked List

Doubly Linked List là biến thể trong đó mỗi node có hai reference:

```text
prev + value + next
```

```text
null <- [10] <-> [20] <-> [30] -> null
```

Ưu điểm:

- Duyệt được hai chiều.
- Delete node nhanh hơn nếu đã có reference tới node đó.

Nhược điểm:

- Tốn thêm memory cho `prev`.
- Cập nhật insert/delete phức tạp hơn vì phải sửa cả `prev` và `next`.

Trong Java, `LinkedList` là doubly linked list và implements cả `List` lẫn `Deque`.

```java
LinkedList<Integer> list = new LinkedList<>();
list.addFirst(1);
list.addLast(2);
list.removeFirst();
```

### Circular Linked List

Circular Linked List là list mà node cuối trỏ lại node đầu.

```text
[10] -> [20] -> [30]
 ^              |
 |--------------|
```

Thường gặp trong bài toán vòng tròn, round-robin, hoặc Josephus problem.

## Stack (bổ sung)

Stack là data structure theo nguyên tắc **LIFO**: Last In, First Out.

```text
push 1
push 2
push 3
pop -> 3
```

Các thao tác chính:

| Operation | Ý nghĩa | Complexity |
| --- | --- | --- |
| `push` | Thêm vào đỉnh stack | `O(1)` |
| `pop` | Lấy và xóa phần tử đỉnh | `O(1)` |
| `peek` | Xem phần tử đỉnh | `O(1)` |
| `isEmpty` | Kiểm tra rỗng | `O(1)` |

Trong Java, nên dùng `ArrayDeque` thay vì `Stack` legacy.

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(10);
stack.push(20);

System.out.println(stack.pop()); // 20
```

Use case phổ biến:

- Undo/redo.
- Call stack.
- Kiểm tra dấu ngoặc hợp lệ.
- DFS iterative.
- Monotonic stack.

Ví dụ kiểm tra ngoặc:

```java
boolean isValid(String s) {
    Deque<Character> stack = new ArrayDeque<>();

    for (char c : s.toCharArray()) {
        if (c == '(') {
            stack.push(c);
        } else if (c == ')') {
            if (stack.isEmpty()) return false;
            stack.pop();
        }
    }

    return stack.isEmpty();
}
```

## Queue và Deque (bổ sung)

Queue là data structure theo nguyên tắc **FIFO**: First In, First Out.

```text
offer 1
offer 2
poll -> 1
```

| Operation | Ý nghĩa | Complexity |
| --- | --- | --- |
| `offer` | Thêm vào cuối queue | `O(1)` |
| `poll` | Lấy và xóa phần tử đầu | `O(1)` |
| `peek` | Xem phần tử đầu | `O(1)` |

Trong Java:

```java
Queue<Integer> queue = new ArrayDeque<>();
queue.offer(10);
queue.offer(20);

System.out.println(queue.poll()); // 10
```

Deque là double-ended queue, cho phép thêm/xóa ở cả hai đầu.

```java
Deque<Integer> deque = new ArrayDeque<>();
deque.addFirst(1);
deque.addLast(2);
deque.removeFirst();
deque.removeLast();
```

Use case:

- BFS dùng queue.
- Sliding window maximum dùng deque.
- Task scheduling.
- Producer-consumer dùng blocking queue như `ArrayBlockingQueue`, `LinkedBlockingQueue`.

## Map và Hash Table (bổ sung)

`Map` lưu dữ liệu theo dạng key-value. Key không trùng; value có thể trùng.

Implementation phổ biến:

| Implementation | Thứ tự | Complexity trung bình | Ghi chú |
| --- | --- | --- | --- |
| `HashMap` | Không đảm bảo | `O(1)` | Phổ biến nhất |
| `LinkedHashMap` | Insertion/access order | `O(1)` | Hợp làm LRU cache đơn giản |
| `TreeMap` | Sorted theo key | `O(log n)` | Dựa trên tree |
| `ConcurrentHashMap` | Không đảm bảo | `O(1)` trung bình | Thread-safe cho concurrent access |

### HashMap hoạt động như thế nào?

Khi `put(key, value)`:

1. Tính `hashCode()` của key.
2. Trộn hash để giảm collision.
3. Tìm bucket index trong array nội bộ.
4. Nếu bucket trống, thêm entry.
5. Nếu bucket có entry, dùng `equals()` để update key cũ hoặc thêm node mới.

```java
Map<String, Integer> count = new HashMap<>();
count.put("java", 1);
count.put("spring", 2);

System.out.println(count.get("java")); // 1
```

Collision xảy ra khi nhiều key rơi vào cùng bucket. Từ Java 8, nếu một bucket quá dài và map đủ lớn, linked list trong bucket có thể chuyển thành red-black tree để lookup tốt hơn.

Điểm cần nhớ:

- `HashMap` không thread-safe.
- Cho phép một key `null`.
- Không đảm bảo thứ tự duyệt.
- Không nên dùng object mutable làm key nếu field dùng trong `equals/hashCode` có thể thay đổi.

## Heap và PriorityQueue (bổ sung)

Heap là binary tree đặc biệt thường được lưu bằng array.

Có hai loại phổ biến:

- **Min-heap:** parent luôn nhỏ hơn hoặc bằng child; root là phần tử nhỏ nhất.
- **Max-heap:** parent luôn lớn hơn hoặc bằng child; root là phần tử lớn nhất.

Trong Java, `PriorityQueue` mặc định là min-heap.

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(5);
pq.offer(1);
pq.offer(3);

System.out.println(pq.poll()); // 1
```

Max-heap:

```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
```

Complexity:

| Operation | Complexity |
| --- | --- |
| Peek min/max | `O(1)` |
| Insert | `O(log n)` |
| Poll min/max | `O(log n)` |
| Build heap từ n phần tử | `O(n)` |

Use case:

- Top K elements.
- K-way merge.
- Dijkstra.
- Scheduler theo priority.
- Median stream với 2 heap.

## Tree (bổ sung)

Tree là data structure phân cấp gồm các node. Node trên cùng là root; node không có child là leaf.

Thuật ngữ:

| Thuật ngữ | Ý nghĩa |
| --- | --- |
| Root | Node gốc |
| Parent/Child | Quan hệ cha/con |
| Leaf | Node không có child |
| Height | Chiều cao cây |
| Depth | Độ sâu của node |
| Subtree | Cây con |

### Binary Tree

Binary tree là tree mà mỗi node có tối đa 2 child: left và right.

```java
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
}
```

Các kiểu duyệt:

| Traversal | Thứ tự |
| --- | --- |
| Preorder | Root -> Left -> Right |
| Inorder | Left -> Root -> Right |
| Postorder | Left -> Right -> Root |
| Level-order | Duyệt theo tầng, dùng queue |

Ví dụ inorder:

```java
void inorder(TreeNode root) {
    if (root == null) return;
    inorder(root.left);
    System.out.println(root.val);
    inorder(root.right);
}
```

### Binary Search Tree

Binary Search Tree (BST) là binary tree có rule:

```text
left subtree < root < right subtree
```

Nếu cây cân bằng:

- Search: `O(log n)`
- Insert: `O(log n)`
- Delete: `O(log n)`

Nếu cây lệch như linked list, complexity có thể thành `O(n)`.

### Balanced Tree

Balanced tree giữ chiều cao cây gần `log n`, giúp thao tác ổn định hơn.

Ví dụ:

- AVL Tree.
- Red-black Tree.
- B-Tree/B+Tree trong database/index.

Trong Java, `TreeMap` và `TreeSet` dựa trên red-black tree.

## Trie (bổ sung)

Trie là tree dùng để lưu string theo từng ký tự, rất hợp cho prefix search.

Ví dụ lưu `cat`, `car`, `dog`:

```text
root
 ├─ c ─ a ─ t
 │       └─ r
 └─ d ─ o ─ g
```

Use case:

- Autocomplete.
- Search theo prefix.
- Word dictionary.
- IP routing hoặc bitwise trie trong một số bài toán.

Node cơ bản:

```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isWord;
}
```

Search/insert một word dài `L` thường là `O(L)`, không phụ thuộc trực tiếp vào số lượng word đã lưu, nhưng tốn memory hơn HashSet vì nhiều node.

## Graph (bổ sung)

Graph gồm các vertex/node và edge/cạnh.

Loại graph phổ biến:

| Loại | Ý nghĩa |
| --- | --- |
| Undirected graph | Cạnh không có hướng |
| Directed graph | Cạnh có hướng |
| Weighted graph | Cạnh có trọng số |
| Unweighted graph | Cạnh không trọng số |
| Cyclic graph | Có chu trình |
| DAG | Directed Acyclic Graph, đồ thị có hướng không chu trình |

Cách biểu diễn:

### Adjacency Matrix

Dùng ma trận `n x n`.

- Kiểm tra cạnh giữa `u` và `v`: `O(1)`.
- Tốn memory `O(V^2)`.
- Hợp với graph dày.

### Adjacency List

Dùng list các neighbor của từng node.

- Tốn memory `O(V + E)`.
- Duyệt neighbor nhanh.
- Hợp với graph thưa.

```java
Map<Integer, List<Integer>> graph = new HashMap<>();
graph.computeIfAbsent(1, k -> new ArrayList<>()).add(2);
graph.computeIfAbsent(2, k -> new ArrayList<>()).add(3);
```

## BFS và DFS (bổ sung)

### BFS

BFS duyệt graph/tree theo từng tầng, thường dùng queue.

```java
void bfs(Map<Integer, List<Integer>> graph, int start) {
    Set<Integer> visited = new HashSet<>();
    Queue<Integer> queue = new ArrayDeque<>();

    visited.add(start);
    queue.offer(start);

    while (!queue.isEmpty()) {
        int node = queue.poll();
        for (int next : graph.getOrDefault(node, List.of())) {
            if (visited.add(next)) {
                queue.offer(next);
            }
        }
    }
}
```

Use case:

- Shortest path trong unweighted graph.
- Duyệt level-order tree.
- Tìm khoảng cách ít bước nhất.

Complexity: `O(V + E)`.

### DFS

DFS duyệt sâu trước, có thể dùng recursion hoặc stack.

```java
void dfs(Map<Integer, List<Integer>> graph, int node, Set<Integer> visited) {
    if (!visited.add(node)) return;

    for (int next : graph.getOrDefault(node, List.of())) {
        dfs(graph, next, visited);
    }
}
```

Use case:

- Detect cycle.
- Connected components.
- Topological sort.
- Backtracking.

Complexity: `O(V + E)`.

## Sorting Algorithms (bổ sung)

| Algorithm | Average | Worst | Stable? | Ghi chú |
| --- | --- | --- | --- | --- |
| Bubble sort | `O(n^2)` | `O(n^2)` | Có | Dễ hiểu, ít dùng thực tế |
| Selection sort | `O(n^2)` | `O(n^2)` | Không mặc định | Ít swap |
| Insertion sort | `O(n^2)` | `O(n^2)` | Có | Tốt với dữ liệu nhỏ/gần sorted |
| Merge sort | `O(n log n)` | `O(n log n)` | Có | Cần thêm memory |
| Quick sort | `O(n log n)` | `O(n^2)` | Không | Nhanh thực tế nếu pivot tốt |
| Heap sort | `O(n log n)` | `O(n log n)` | Không | Dựa trên heap |
| Counting sort | `O(n + k)` | `O(n + k)` | Có thể stable | Hợp khi range `k` nhỏ |

Trong Java:

- `Arrays.sort(int[])` dùng thuật toán tối ưu cho primitive array.
- `Arrays.sort(Object[])` và `Collections.sort()` stable, dựa trên TimSort.

### Stable sort

Stable sort giữ nguyên thứ tự tương đối của các phần tử bằng nhau.

Ví dụ sort danh sách user theo `age`, nếu hai user cùng age thì stable sort giữ thứ tự ban đầu của hai user đó.

Stable sort quan trọng khi sort nhiều tiêu chí theo từng bước.

## Searching Algorithms (bổ sung)

| Algorithm | Điều kiện | Complexity |
| --- | --- | --- |
| Linear search | Không cần sorted | `O(n)` |
| Binary search | Dữ liệu đã sorted | `O(log n)` |
| Hash lookup | Có hash table | Trung bình `O(1)` |
| BFS | Graph/tree | `O(V + E)` |
| DFS | Graph/tree | `O(V + E)` |

Binary search:

```java
int binarySearch(int[] nums, int target) {
    int left = 0;
    int right = nums.length - 1;

    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) return mid;
        if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }

    return -1;
}
```

Dùng `left + (right - left) / 2` để tránh overflow so với `(left + right) / 2`.

## Recursion và Backtracking (bổ sung)

Recursion là khi function tự gọi lại chính nó.

Một recursion tốt cần:

- Base case để dừng.
- Recursive case để tiến gần tới base case.

```java
int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}
```

Backtracking là kỹ thuật thử một lựa chọn, đi tiếp, nếu không hợp lệ thì quay lui.

Use case:

- Generate subsets/permutations.
- N-Queens.
- Sudoku.
- Combination sum.

Skeleton:

```java
void backtrack(List<Integer> path, int start) {
    // ghi nhận kết quả nếu cần

    for (int i = start; i < nums.length; i++) {
        path.add(nums[i]);
        backtrack(path, i + 1);
        path.remove(path.size() - 1);
    }
}
```

## Two Pointers (bổ sung)

Two pointers dùng hai con trỏ để duyệt dữ liệu, thường giúp giảm nested loop từ `O(n^2)` xuống `O(n)`.

Phù hợp khi:

- Array/string đã sorted.
- Cần tìm pair/triplet.
- Cần reverse/in-place transform.
- Cần xử lý window hai đầu.

Ví dụ kiểm tra palindrome:

```java
boolean isPalindrome(String s) {
    int left = 0;
    int right = s.length() - 1;

    while (left < right) {
        if (s.charAt(left) != s.charAt(right)) return false;
        left++;
        right--;
    }

    return true;
}
```

Ví dụ two sum sorted:

```java
int[] twoSumSorted(int[] nums, int target) {
    int left = 0;
    int right = nums.length - 1;

    while (left < right) {
        int sum = nums[left] + nums[right];
        if (sum == target) return new int[] {left, right};
        if (sum < target) left++;
        else right--;
    }

    return new int[] {-1, -1};
}
```

## Sliding Window (bổ sung)

Sliding window dùng một cửa sổ `[left, right]` để xử lý subarray/substring liên tiếp.

Phù hợp khi bài toán hỏi:

- Subarray/substring liên tiếp.
- Tổng/độ dài lớn nhất hoặc nhỏ nhất.
- Điều kiện có thể update khi thêm/xóa phần tử ở hai đầu.

Ví dụ tìm tổng lớn nhất của subarray có độ dài `k`:

```java
int maxSum(int[] nums, int k) {
    int window = 0;

    for (int i = 0; i < k; i++) {
        window += nums[i];
    }

    int best = window;
    for (int right = k; right < nums.length; right++) {
        window += nums[right];
        window -= nums[right - k];
        best = Math.max(best, window);
    }

    return best;
}
```

Complexity là `O(n)` vì mỗi phần tử được thêm/xóa khỏi window số lần hữu hạn.

## Prefix Sum (bổ sung)

Prefix sum giúp tính tổng đoạn nhanh.

```text
prefix[i] = tổng nums[0..i-1]
sum(l, r) = prefix[r + 1] - prefix[l]
```

Ví dụ:

```java
int[] buildPrefix(int[] nums) {
    int[] prefix = new int[nums.length + 1];
    for (int i = 0; i < nums.length; i++) {
        prefix[i + 1] = prefix[i] + nums[i];
    }
    return prefix;
}

int rangeSum(int[] prefix, int left, int right) {
    return prefix[right + 1] - prefix[left];
}
```

Use case:

- Query sum nhiều lần.
- Subarray sum.
- Difference array/range update.
- 2D prefix sum cho matrix.

## Dynamic Programming (bổ sung)

Dynamic Programming (DP) dùng để tối ưu bài toán có:

- **Overlapping subproblems:** bài toán con bị tính lặp lại.
- **Optimal substructure:** lời giải tối ưu của bài lớn xây từ lời giải tối ưu của bài nhỏ.

Hai cách triển khai:

| Cách | Ý nghĩa |
| --- | --- |
| Top-down + memoization | Recursion, lưu kết quả đã tính |
| Bottom-up + tabulation | Tính từ bài nhỏ lên bài lớn |

Ví dụ Fibonacci top-down:

```java
int fib(int n, Map<Integer, Integer> memo) {
    if (n <= 1) return n;
    if (memo.containsKey(n)) return memo.get(n);

    int value = fib(n - 1, memo) + fib(n - 2, memo);
    memo.put(n, value);
    return value;
}
```

Ví dụ bottom-up:

```java
int fib(int n) {
    if (n <= 1) return n;

    int prev2 = 0;
    int prev1 = 1;

    for (int i = 2; i <= n; i++) {
        int cur = prev1 + prev2;
        prev2 = prev1;
        prev1 = cur;
    }

    return prev1;
}
```

Các dạng DP phổ biến:

- 1D DP: climbing stairs, house robber.
- 2D DP: unique paths, grid path.
- Knapsack.
- Longest Common Subsequence.
- Longest Increasing Subsequence.
- Interval DP.

## Greedy (bổ sung)

Greedy chọn phương án tốt nhất tại thời điểm hiện tại với hy vọng tạo ra lời giải tối ưu toàn cục.

Greedy đúng khi bài toán có tính chất cho phép local optimal dẫn tới global optimal. Không phải cứ chọn tham lam là đúng.

Use case phổ biến:

- Activity selection.
- Interval scheduling.
- Huffman coding.
- Dijkstra với edge weight không âm.
- Một số bài sort + chọn.

Ví dụ chọn interval không overlap nhiều nhất:

```java
intervals.sort(Comparator.comparingInt(a -> a.end));

int count = 0;
int lastEnd = Integer.MIN_VALUE;

for (Interval interval : intervals) {
    if (interval.start >= lastEnd) {
        count++;
        lastEnd = interval.end;
    }
}
```

Tư duy chứng minh greedy thường cần exchange argument: nếu có lời giải tối ưu khác, có thể đổi lựa chọn đầu tiên sang lựa chọn greedy mà không làm kết quả xấu đi.

## Union-Find / Disjoint Set Union (bổ sung)

Union-Find dùng để quản lý các tập rời nhau, hỗ trợ nhanh hai thao tác:

- `find(x)`: tìm representative/root của tập chứa `x`.
- `union(a, b)`: gộp hai tập.

Tối ưu quan trọng:

- Path compression.
- Union by rank/size.

```java
class DSU {
    int[] parent;
    int[] size;

    DSU(int n) {
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    boolean union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) return false;

        if (size[rootA] < size[rootB]) {
            int temp = rootA;
            rootA = rootB;
            rootB = temp;
        }

        parent[rootB] = rootA;
        size[rootA] += size[rootB];
        return true;
    }
}
```

Use case:

- Connected components.
- Detect cycle trong undirected graph.
- Kruskal Minimum Spanning Tree.
- Grouping/merge accounts.

## Topological Sort (bổ sung)

Topological sort sắp xếp các node trong DAG sao cho nếu có cạnh `u -> v`, thì `u` đứng trước `v`.

Use case:

- Course schedule.
- Build dependency.
- Task dependency.
- Pipeline execution.

Kahn's algorithm dùng indegree + queue:

```java
List<Integer> topoSort(int n, List<List<Integer>> graph) {
    int[] indegree = new int[n];
    for (int u = 0; u < n; u++) {
        for (int v : graph.get(u)) {
            indegree[v]++;
        }
    }

    Queue<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < n; i++) {
        if (indegree[i] == 0) queue.offer(i);
    }

    List<Integer> order = new ArrayList<>();
    while (!queue.isEmpty()) {
        int u = queue.poll();
        order.add(u);

        for (int v : graph.get(u)) {
            indegree[v]--;
            if (indegree[v] == 0) queue.offer(v);
        }
    }

    return order;
}
```

Nếu kết quả có ít hơn `n` node, graph có cycle.

## Shortest Path (bổ sung)

| Algorithm | Dùng khi nào | Complexity gần đúng |
| --- | --- | --- |
| BFS | Graph không trọng số | `O(V + E)` |
| Dijkstra | Trọng số không âm | `O((V + E) log V)` với priority queue |
| Bellman-Ford | Có cạnh âm, detect negative cycle | `O(VE)` |
| Floyd-Warshall | Shortest path mọi cặp node | `O(V^3)` |

Quy tắc chọn:

- Không trọng số: BFS.
- Trọng số không âm: Dijkstra.
- Có cạnh âm: Bellman-Ford.
- Cần mọi cặp và graph nhỏ: Floyd-Warshall.

Không dùng Dijkstra nếu graph có cạnh âm vì kết quả có thể sai.

## Complexity Cheat Sheet

| Data structure | Access | Search | Insert | Delete | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| Array | `O(1)` | `O(n)` | `O(n)` | `O(n)` | Insert/delete nếu phải shift |
| ArrayList | `O(1)` | `O(n)` | `O(n)` | `O(n)` | Add cuối amortized `O(1)` |
| LinkedList | `O(n)` | `O(n)` | `O(1)` nếu đã có node | `O(1)` nếu đã có node | Tìm node vẫn `O(n)` |
| Stack | `O(n)` | `O(n)` | `O(1)` | `O(1)` | Insert/delete ở top |
| Queue | `O(n)` | `O(n)` | `O(1)` | `O(1)` | Offer/poll |
| HashSet | Không theo index | `O(1)` avg | `O(1)` avg | `O(1)` avg | Worst-case có thể xấu hơn |
| TreeSet | Không theo index | `O(log n)` | `O(log n)` | `O(log n)` | Luôn sorted |
| HashMap | Theo key `O(1)` avg | `O(1)` avg | `O(1)` avg | `O(1)` avg | Không giữ thứ tự |
| TreeMap | Theo key `O(log n)` | `O(log n)` | `O(log n)` | `O(log n)` | Sorted key |
| Heap | Peek root `O(1)` | `O(n)` | `O(log n)` | Poll root `O(log n)` | Hợp top K/priority |

## Checklist chọn Data Structure

- Cần random access theo index: Array/ArrayList.
- Cần thêm/xóa hai đầu: ArrayDeque.
- Cần LIFO: Stack bằng ArrayDeque.
- Cần FIFO: Queue bằng ArrayDeque.
- Cần unique không quan tâm thứ tự: HashSet.
- Cần unique và giữ thứ tự insert: LinkedHashSet.
- Cần unique và sorted: TreeSet.
- Cần key-value lookup nhanh: HashMap.
- Cần key-value sorted theo key: TreeMap.
- Cần lấy min/max liên tục: PriorityQueue.
- Cần prefix search: Trie.
- Cần quan hệ nhiều node/cạnh: Graph.
- Cần group/connected component động: Union-Find.

## Những phần cần bổ sung/sửa sau

- Bổ sung hình minh họa cho Array, Linked List, Heap, Tree, Graph.
- Bổ sung bài tập mẫu cho từng pattern: two pointers, sliding window, prefix sum, BFS/DFS, DP.
- Bổ sung riêng một file sâu hơn cho Graph algorithms.
- Bổ sung riêng một file sâu hơn cho Dynamic Programming.
- Bổ sung Java implementation đầy đủ cho Trie, Heap custom, LRU cache, topological sort và Dijkstra.
