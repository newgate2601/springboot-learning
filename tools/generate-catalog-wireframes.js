const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const outDir = path.join(root, "assets", "catalog-wireframes");
fs.mkdirSync(outDir, { recursive: true });

const esc = (s) =>
  String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");

function svg(name, width, height, body) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(name)}">
  <defs>
    <filter id="shadow" x="-10%" y="-10%" width="120%" height="130%">
      <feDropShadow dx="0" dy="8" stdDeviation="12" flood-color="#172033" flood-opacity="0.12"/>
    </filter>
    <linearGradient id="product" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#e8f3ff"/>
      <stop offset="1" stop-color="#f4e9ff"/>
    </linearGradient>
  </defs>
  <rect width="100%" height="100%" fill="#f5f7fb"/>
  ${body}
</svg>
`;
}

function rect(x, y, w, h, fill = "#ffffff", stroke = "#d9e0ea", r = 10, extra = "") {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}" stroke="${stroke}" ${extra}/>`;
}

function text(x, y, value, size = 16, weight = 500, fill = "#172033", extra = "") {
  return `<text x="${x}" y="${y}" font-family="Inter, Arial, sans-serif" font-size="${size}" font-weight="${weight}" fill="${fill}" ${extra}>${esc(value)}</text>`;
}

function button(x, y, w, h, label, fill = "#2563eb", fg = "#ffffff") {
  return `${rect(x, y, w, h, fill, "none", 8)}${text(x + 16, y + h / 2 + 5, label, 14, 700, fg)}`;
}

function input(x, y, w, h, label = "") {
  return `${rect(x, y, w, h, "#ffffff", "#c9d4e3", 8)}${label ? text(x + 14, y + h / 2 + 5, label, 14, 500, "#667085") : ""}`;
}

function chip(x, y, label, active = false) {
  const w = Math.max(78, label.length * 9 + 28);
  return `${rect(x, y, w, 34, active ? "#e7f0ff" : "#ffffff", active ? "#2563eb" : "#c9d4e3", 17)}${text(x + 14, y + 22, label, 13, 700, active ? "#1d4ed8" : "#344054")}`;
}

function header(title = "LOGO", admin = false) {
  return `${rect(40, 32, 1120, 72, "#ffffff", "#dfe6ef", 14, 'filter="url(#shadow)"')}
${text(68, 76, title, 22, 800, admin ? "#7c3aed" : "#2563eb")}
${input(240, 50, 520, 36, "Tìm kiếm sản phẩm...")}
${text(940, 75, "Cart", 15, 700, "#344054")}
${text(1010, 75, "Login", 15, 700, "#344054")}`;
}

function productCard(x, y, title, price = "199.000đ", badge = "Còn hàng") {
  return `${rect(x, y, 190, 228, "#ffffff", "#dde5ef", 12)}
${rect(x + 16, y + 16, 158, 108, "url(#product)", "#d7e2ef", 10)}
${text(x + 18, y + 152, title, 15, 800)}
${text(x + 18, y + 178, "Giá từ " + price, 14, 700, "#dc2626")}
${text(x + 18, y + 202, badge, 13, 700, badge === "Hết hàng" ? "#b42318" : "#087443")}`;
}

function table(x, y, widths, rows, headerFill = "#f1f5f9") {
  const rowH = 44;
  const total = widths.reduce((a, b) => a + b, 0);
  let out = rect(x, y, total, rowH * rows.length, "#ffffff", "#d9e0ea", 10);
  rows.forEach((row, ri) => {
    const yy = y + ri * rowH;
    out += `<rect x="${x}" y="${yy}" width="${total}" height="${rowH}" fill="${ri === 0 ? headerFill : "#ffffff"}" opacity="${ri === 0 ? 1 : 0}"/>`;
    let xx = x;
    row.forEach((cell, ci) => {
      out += text(xx + 12, yy + 28, cell, ri === 0 ? 13 : 14, ri === 0 ? 800 : 500, ri === 0 ? "#344054" : "#172033");
      xx += widths[ci];
    });
  });
  return out;
}

function frame(title, inner, h = 720) {
  return svg(title, 1200, h, `${rect(28, 22, 1144, h - 44, "#eef3f8", "none", 18)}
${text(52, 62, title, 24, 900)}
${inner}`);
}

const screens = [
  ["home-product-highlights", "6.1. Màn Home / Product Highlights", frame("Home / Product Highlights", `${header()}
${rect(60, 128, 1080, 126, "#fff7ed", "#fed7aa", 18)}${text(92, 178, "Banner khuyến mãi / campaign", 28, 900, "#9a3412")}${text(94, 214, "Ưu đãi hôm nay cho sản phẩm mới và bán chạy", 16, 600, "#9a3412")}
${text(60, 298, "Danh mục nổi bật", 20, 900)}${chip(60, 320, "Thời trang", true)}${chip(190, 320, "Điện thoại")}${chip(328, 320, "Mỹ phẩm")}${chip(450, 320, "Gia dụng")}
${text(60, 410, "Sản phẩm mới", 20, 900)}${productCard(60, 434, "Áo thun basic")}${productCard(280, 434, "Sneaker runner", "799.000đ")}${productCard(500, 434, "iPhone 15", "19.990.000đ")}${productCard(720, 434, "Serum dưỡng da", "299.000đ")}
${text(940, 410, "Bán chạy", 20, 900)}${productCard(940, 434, "Tai nghe Pro", "990.000đ")}`, 710)],
  ["product-listing", "6.2. Màn Product Listing", frame("Product Listing", `${header()}
${text(60, 134, "Home > Thời trang > Áo thun", 14, 700, "#667085")}
${rect(60, 166, 250, 492, "#ffffff", "#d9e0ea", 14)}${text(84, 204, "Bộ lọc", 20, 900)}${text(84, 246, "Category", 14, 800)}${text(84, 278, "☐ Áo thun", 14)}${text(84, 306, "☐ Áo sơ mi", 14)}${text(84, 354, "Brand", 14, 800)}${text(84, 386, "☐ Nike", 14)}${text(84, 414, "☐ Adidas", 14)}${text(84, 462, "Price", 14, 800)}${input(84, 482, 78, 34, "Min")}${input(172, 482, 78, 34, "Max")}${text(84, 560, "☐ Còn hàng", 14)}
${text(350, 202, "Kết quả tìm kiếm", 22, 900)}${input(910, 176, 190, 38, "Sort: Mới nhất v")}
${productCard(350, 236, "Áo thun nam")}${productCard(570, 236, "Áo thể thao", "249.000đ")}${productCard(790, 236, "Áo polo", "359.000đ", "Hết hàng")}
${productCard(350, 490, "Hoodie cotton", "499.000đ")}${productCard(570, 490, "Quần jeans", "599.000đ")}${productCard(790, 490, "Sneaker", "799.000đ")}
${text(350, 690, "Page: < 1 2 3 >", 15, 800, "#2563eb")}`, 740)],
  ["product-detail", "6.3. Màn Product Detail", frame("Product Detail", `${header()}
${text(60, 134, "Home > Thời trang > Áo thun > Áo thun nam basic", 14, 700, "#667085")}
${rect(60, 166, 500, 360, "#ffffff", "#d9e0ea", 14)}${text(84, 204, "Gallery ảnh", 18, 900)}${rect(92, 230, 410, 210, "url(#product)", "#d7e2ef", 12)}${rect(92, 458, 70, 54, "#eef6ff", "#bfdbfe", 8)}${rect(176, 458, 70, 54, "#fef3c7", "#fde68a", 8)}${rect(260, 458, 70, 54, "#fce7f3", "#f9a8d4", 8)}
${rect(590, 166, 550, 360, "#ffffff", "#d9e0ea", 14)}${text(622, 210, "Áo thun nam basic", 28, 900)}${text(622, 246, "Brand: Nike", 15, 700, "#667085")}${text(622, 278, "Rating 4.8 / Đã bán 1.2k", 15, 700)}${text(622, 324, "Giá: 199.000đ", 26, 900, "#dc2626")}
${text(622, 374, "Màu:", 15, 800)}${chip(680, 352, "Đen", true)}${chip(770, 352, "Trắng")}${text(622, 424, "Size:", 15, 800)}${chip(680, 402, "M", true)}${chip(760, 402, "L")}${chip(840, 402, "XL")}${text(622, 474, "Tồn kho: Còn hàng", 15, 800, "#087443")}${input(790, 450, 120, 38, "-   1   +")}${button(622, 548, 180, 46, "Thêm vào giỏ")}${button(820, 548, 140, 46, "Mua ngay", "#f97316")}
${rect(60, 556, 1080, 116, "#ffffff", "#d9e0ea", 14)}${text(86, 596, "Mô tả sản phẩm", 18, 900)}${text(86, 632, "Thông số kỹ thuật · Chính sách đổi trả · Sản phẩm liên quan", 15, 600, "#667085")}`, 710)],
  ["search-suggestion", "6.4. Màn Search Suggestion", frame("Search Suggestion", `${rect(330, 110, 540, 460, "#ffffff", "#d9e0ea", 16, 'filter="url(#shadow)"')}${input(360, 142, 480, 46, "Search: áo th")}
${text(360, 228, "Gợi ý", 18, 900)}${text(382, 264, "áo thun nam", 15)}${text(382, 298, "áo thun nữ", 15)}${text(382, 332, "áo thể thao", 15)}
${text(360, 390, "Sản phẩm", 18, 900)}${rect(382, 416, 42, 42, "url(#product)", "#d7e2ef", 8)}${text(444, 444, "Áo thun nam basic", 15, 800)}${rect(382, 476, 42, 42, "url(#product)", "#d7e2ef", 8)}${text(444, 504, "Áo thể thao running", 15, 800)}`, 650)],
  ["admin-product-list", "7.1. Màn Admin Product List", frame("Admin Product List", `${header("Admin > Catalog > Products", true)}
${button(60, 136, 140, 42, "Tạo sản phẩm")}${input(60, 198, 360, 40, "Search...")}${input(438, 198, 170, 40, "Status: All v")}${button(624, 198, 96, 40, "Filter", "#344054")}
${table(60, 270, [88, 80, 270, 170, 140, 130, 180], [["Checkbox", "Ảnh", "Tên", "Category", "Brand", "Status", "Actions"], ["☐", "img", "Áo thun nam basic", "Áo thun", "Nike", "ACTIVE", "View  Edit"], ["☐", "img", "Quần jeans slim", "Quần nam", "Levis", "DRAFT", "View  Edit"]])}
${text(60, 440, "< 1 2 3 >", 15, 800, "#2563eb")}`, 560)],
  ["create-product", "7.2. Màn Create Product", frame("Create Product", `${header("Admin > Catalog > Create Product", true)}
${rect(120, 138, 960, 470, "#ffffff", "#d9e0ea", 16)}${text(156, 184, "Thông tin cơ bản", 22, 900)}
${text(156, 240, "Tên sản phẩm *", 15, 800)}${input(360, 214, 560, 42, "Áo thun nam basic")}
${text(156, 296, "Slug", 15, 800)}${input(360, 270, 560, 42, "ao-thun-nam-basic")}
${text(156, 352, "Brand", 15, 800)}${input(360, 326, 300, 42, "Select brand v")}
${text(156, 408, "Category *", 15, 800)}${input(360, 382, 300, 42, "Select category v")}
${text(156, 464, "Mô tả ngắn", 15, 800)}${input(360, 438, 560, 42, "Mô tả ngắn...")}
${text(156, 520, "Mô tả chi tiết", 15, 800)}${rect(360, 494, 560, 74, "#ffffff", "#c9d4e3", 8)}${text(380, 534, "textarea...", 14, 500, "#667085")}
${button(360, 632, 120, 42, "Lưu nháp", "#344054")}${button(496, 632, 250, 42, "Lưu và tiếp tục thêm SKU")}`, 720)],
  ["edit-basic-info", "7.3. Màn Edit Product - Tab Basic Info", frame("Edit Product - Basic Info", `${rect(60, 92, 1080, 70, "#ffffff", "#d9e0ea", 14)}${text(88, 136, "Edit Product: Áo thun nam basic", 22, 900)}${text(910, 136, "Status: DRAFT", 16, 900, "#f97316")}
${rect(60, 184, 1080, 54, "#ffffff", "#d9e0ea", 12)}${chip(88, 194, "Basic", true)}${chip(180, 194, "Images")}${chip(286, 194, "Variants/SKU")}${chip(430, 194, "Price")}${chip(520, 194, "Preview")}
${rect(120, 268, 960, 330, "#ffffff", "#d9e0ea", 16)}${text(156, 320, "Tên sản phẩm *", 15, 800)}${input(360, 294, 560, 42, "Áo thun nam basic")}
${text(156, 376, "Slug", 15, 800)}${input(360, 350, 560, 42, "ao-thun-nam-basic")}
${text(156, 432, "Brand", 15, 800)}${input(360, 406, 300, 42, "Nike v")}${text(156, 488, "Category *", 15, 800)}${input(360, 462, 300, 42, "Áo thun v")}
${button(360, 630, 150, 42, "Lưu thay đổi")}${button(528, 630, 110, 42, "Preview", "#344054")}${button(656, 630, 110, 42, "Publish", "#087443")}`, 720)],
  ["edit-images", "7.4. Màn Edit Product - Tab Images", frame("Product Images", `${rect(100, 120, 1000, 520, "#ffffff", "#d9e0ea", 16)}${text(136, 168, "Product Images", 24, 900)}${button(136, 196, 120, 42, "Upload ảnh")}
${text(136, 286, "Gallery", 18, 900)}
${rect(136, 314, 100, 76, "url(#product)", "#d7e2ef", 10)}${text(260, 360, "Main", 14, 900, "#087443")}${button(330, 332, 100, 36, "Set main", "#344054")}${button(448, 332, 86, 36, "Delete", "#dc2626")}
${rect(136, 410, 100, 76, "url(#product)", "#d7e2ef", 10)}${button(330, 430, 100, 36, "Set main", "#344054")}${button(448, 430, 86, 36, "Delete", "#dc2626")}
${rect(136, 506, 100, 76, "url(#product)", "#d7e2ef", 10)}${button(330, 526, 100, 36, "Set main", "#344054")}${button(448, 526, 86, 36, "Delete", "#dc2626")}
${text(640, 360, "Kéo thả để sắp xếp thứ tự ảnh", 16, 700, "#667085")}${button(640, 402, 120, 42, "Lưu thứ tự")}`, 700)],
  ["variants-sku", "7.5. Màn Edit Product - Tab Variants/SKU", frame("Variants / SKU", `${rect(60, 96, 1080, 220, "#ffffff", "#d9e0ea", 16)}${text(94, 142, "Thuộc tính variant", 22, 900)}${text(94, 192, "Color:", 15, 800)}${chip(170, 170, "Đen", true)}${chip(260, 170, "Trắng")}${chip(370, 170, "+ Thêm màu")}${text(94, 248, "Size:", 15, 800)}${chip(170, 226, "M", true)}${chip(250, 226, "L")}${chip(330, 226, "XL")}${chip(420, 226, "+ Thêm size")}${button(760, 202, 220, 42, "Generate SKU từ variant")}
${rect(60, 348, 1080, 250, "#ffffff", "#d9e0ea", 16)}${text(94, 394, "SKU list", 22, 900)}${table(94, 420, [160, 120, 100, 160, 140, 120], [["SKU Code", "Color", "Size", "Barcode", "Status", "Actions"], ["TS-BLK-M", "Đen", "M", "...", "ACTIVE", "Edit"], ["TS-BLK-L", "Đen", "L", "...", "ACTIVE", "Edit"], ["TS-WHT-M", "Trắng", "M", "...", "ACTIVE", "Edit"]])}
${button(94, 630, 170, 42, "Thêm SKU thủ công", "#344054")}${button(282, 630, 110, 42, "Lưu SKU")}`, 720)],
  ["edit-sku-detail", "7.6. Màn Edit SKU Detail", frame("Edit SKU Detail", `${rect(160, 104, 880, 520, "#ffffff", "#d9e0ea", 16)}${text(196, 154, "Edit SKU: TS-BLK-M", 24, 900)}
${text(196, 214, "SKU Code *", 15, 800)}${input(390, 188, 420, 42, "TS-BLK-M")}
${text(196, 270, "Barcode", 15, 800)}${input(390, 244, 420, 42, "893...")}
${text(196, 326, "Color", 15, 800)}${input(390, 300, 220, 42, "Đen v")}${text(196, 382, "Size", 15, 800)}${input(390, 356, 220, 42, "M v")}
${text(196, 438, "Weight", 15, 800)}${input(390, 412, 420, 42, "200 gram")}
${text(196, 494, "Package size", 15, 800)}${input(390, 468, 420, 42, "20 x 15 x 3 cm")}
${text(196, 550, "Status", 15, 800)}${input(390, 524, 220, 42, "ACTIVE v")}${button(650, 524, 120, 42, "Upload", "#344054")}
${button(390, 648, 88, 42, "Lưu")}${button(496, 648, 150, 42, "Ngưng bán SKU", "#dc2626")}`, 720)],
  ["pricing", "7.7. Màn Edit Product - Tab Price", frame("Pricing", `${rect(80, 116, 1040, 430, "#ffffff", "#d9e0ea", 16)}${text(116, 166, "Pricing", 24, 900)}
${table(116, 204, [180, 180, 180, 190, 150], [["SKU Code", "Variant", "Sale Price", "Compare Price", "Status"], ["TS-BLK-M", "Đen / M", "199000", "249000", "ACTIVE"], ["TS-BLK-L", "Đen / L", "199000", "249000", "ACTIVE"], ["TS-WHT-M", "Trắng / M", "209000", "259000", "ACTIVE"]])}
${button(116, 430, 210, 42, "Apply same price to all", "#344054")}${button(344, 430, 110, 42, "Lưu giá")}`, 620)],
  ["product-preview", "7.8. Màn Product Preview", frame("Product Preview", `${rect(80, 112, 1040, 420, "#ffffff", "#d9e0ea", 16)}${text(116, 162, "Preview Mode - Product chưa public", 24, 900, "#7c3aed")}
${rect(116, 204, 360, 210, "url(#product)", "#d7e2ef", 12)}${text(520, 242, "Giao diện giống Product Detail của customer", 20, 900)}${text(520, 284, "Warning: thiếu ảnh chính, chưa có đủ SKU hoặc giá.", 15, 700, "#b45309")}
${button(520, 360, 170, 42, "Quay lại chỉnh sửa", "#344054")}${button(708, 360, 110, 42, "Publish", "#087443")}`, 620)],
  ["categories", "7.9. Màn Category Management", frame("Category Management", `${header("Admin > Catalog > Categories", true)}
${button(60, 136, 170, 42, "Tạo category gốc")}
${rect(60, 210, 1080, 360, "#ffffff", "#d9e0ea", 16)}${text(96, 262, "Thời trang", 18, 900)}${button(840, 236, 80, 36, "Edit", "#344054")}${button(934, 236, 116, 36, "Add child", "#2563eb")}
${text(128, 316, "Áo nam", 16, 800)}${button(840, 290, 80, 36, "Edit", "#344054")}${button(934, 290, 116, 36, "Add child", "#2563eb")}
${text(160, 370, "Áo thun", 16, 700)}${button(840, 344, 80, 36, "Edit", "#344054")}${button(934, 344, 116, 36, "Disable", "#dc2626")}
${text(128, 424, "Quần nam", 16, 800)}${button(840, 398, 80, 36, "Edit", "#344054")}${button(934, 398, 116, 36, "Disable", "#dc2626")}
${text(96, 486, "Điện thoại", 18, 900)}${button(840, 460, 80, 36, "Edit", "#344054")}${button(934, 460, 116, 36, "Add child", "#2563eb")}`, 640)],
  ["brands", "7.10. Màn Brand Management", frame("Brand Management", `${header("Admin > Catalog > Brands", true)}
${button(60, 136, 110, 42, "Tạo brand")}${input(190, 136, 300, 42, "Search...")}
${table(60, 218, [110, 260, 220, 180, 260], [["Logo", "Name", "Slug", "Status", "Actions"], ["img", "Nike", "nike", "ACTIVE", "Edit  Disable"], ["img", "Apple", "apple", "ACTIVE", "Edit  Disable"]])}`, 520)]
];

for (const [file, , content] of screens) {
  fs.writeFileSync(path.join(outDir, `${file}.svg`), content, "utf8");
}

const mdPath = path.join(root, "CATALOG_BUSINESS_AND_SCREEN_SPEC.md");
let md = fs.readFileSync(mdPath, "utf8");

function insertAfterWireframe(markdown, heading, file) {
  const headingIndex = markdown.indexOf(`## ${heading}`);
  if (headingIndex === -1) return markdown;
  const wireframeIndex = markdown.indexOf("Wireframe:", headingIndex);
  if (wireframeIndex === -1) return markdown;
  const fenceStart = markdown.indexOf("```text", wireframeIndex);
  if (fenceStart === -1) return markdown;
  const fenceEnd = markdown.indexOf("```", fenceStart + 7);
  if (fenceEnd === -1) return markdown;
  const insertAt = markdown.indexOf("\n", fenceEnd + 3) + 1;
  const imagePath = `assets/catalog-wireframes/${file}.svg`;
  if (markdown.slice(fenceEnd, fenceEnd + 500).includes(imagePath)) return markdown;
  const block = `\nGiao diện minh họa:\n\n![${heading}](${imagePath})\n`;
  return markdown.slice(0, insertAt) + block + markdown.slice(insertAt);
}

for (const [file, heading] of screens) {
  md = insertAfterWireframe(md, heading, file);
}

fs.writeFileSync(mdPath, md, "utf8");
console.log(`Generated ${screens.length} SVG wireframes and updated ${path.basename(mdPath)}.`);
