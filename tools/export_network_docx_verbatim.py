from __future__ import annotations

import os
import re
import sys
from pathlib import Path

from docx import Document
from docx.oxml.ns import qn


SOURCE = Path(os.environ["TEMP"]) / "network-source.docx"
ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "network"
ASSET_DIR = OUTPUT_DIR / "assets"
OUTPUT_FILE = OUTPUT_DIR / "computer-network.md"


# Các đoạn này vốn là tiêu đề trong Google Docs nhưng không được gán Heading style.
# Chỉ thay đổi cấp trình bày; câu chữ tiêu đề vẫn lấy nguyên văn từ tài liệu.
H1 = {6}
H2 = {
    25, 68, 76, 105, 175, 248, 333, 352, 407, 470, 493, 567,
    605, 616, 644, 681, 696,
}
H3 = {
    13, 17, 323, 336, 348, 485, 568, 575, 582, 617, 649, 658, 667,
    675, 697,
}

SECTION_BEFORE = {
    44: "VPN (Virtual Private Network)",
    92: "Circuit switching",
    98: "Packet switching",
    106: "Repeater",
    114: "Hub",
    121: "Bridge",
    134: "Switch",
    143: "Router",
    149: "Forwarding, store-and-forward và queue",
    167: "Network Firewall",
    176: "Địa chỉ IP",
    188: "ISP",
    196: "Public IP và Private IP",
    208: "Các dải địa chỉ IPv4",
    210: "NAT (Network Address Translation)",
    222: "Default gateway",
    229: "Subnet, subnet mask và CIDR",
    249: "UDP",
    256: "TCP",
    274: "Socket",
    280: "Stream Socket",
    294: "Datagram Socket",
    298: "Channel vật lý và Channel logic",
    305: "Simplex, Half-duplex và Full-duplex",
    309: "Multiplexing",
    317: "Demultiplexing",
    353: "Protocol",
    365: "Syntax, Semantics và Synchronization",
    376: "Các dạng truyền dữ liệu",
    388: "Bandwidth, Throughput, Delay và Packet loss",
    408: "Application Layer",
    418: "Transport Layer",
    428: "Network Layer",
    438: "Link Layer",
    448: "Physical Layer",
    455: "Workflow khi truy cập một website",
    494: "Web page, Website và Object",
    502: "Hosting",
    511: "Domain",
    523: "HTTP",
    530: "HTTP/0.9",
    537: "HTTP/1.0",
    545: "HTTP/1.1",
    557: "HTTP/2",
    590: "SMTP",
    600: "IMAP và POP3",
    606: "Cách DNS hoạt động",
    627: "DASH",
    682: "MAC Address",
    693: "ARP",
    702: "IP spoofing",
    708: "DoS",
}

TABLES = {
    14: {
        "indices": [14, 15],
        "headers": ["Loại node", "Nội dung nguyên bản"],
        "labels": ["End nodes", "Intermediary nodes"],
    },
    19: {
        "indices": [19, 20, 21, 22],
        "headers": ["Loại network", "Nội dung nguyên bản"],
        "labels": [
            "Home network",
            "Enterprise network",
            "Mobile network",
            "Content provider network",
        ],
    },
    45: {
        "indices": [45, 46, 47],
        "headers": ["Thành phần", "Nội dung nguyên bản"],
        "labels": ["VPN Client", "VPN Server", "VPN protocol"],
    },
}

TABLE_MEMBER_INDICES = {
    index for table in TABLES.values() for index in table["indices"]
}


IMAGE_CAPTIONS = {
    18: "Phân loại các network trong Internet",
    58: "Mô hình truy cập mạng công ty qua VPN",
    62: "Luồng Remote Desktop qua VPN",
    72: "Access network",
    77: "Network Core và các access network",
    80: "Kết nối thông qua Global ISP",
    83: "Các ISP và Internet Exchange Point",
    84: "Mô hình phân cấp ISP",
    89: "Các router trong Network Core",
    115: "Hub và miền truyền tín hiệu",
    122: "Bridge trong mạng",
    135: "Switch trong mạng LAN",
    143: "Router kết nối LAN với Internet",
    144: "Routing và forwarding",
    151: "Queue tại router",
    168: "Network Firewall",
    209: "Các dải địa chỉ IPv4",
    255: "TCP three-way handshake",
    274: "Socket giữa hai process",
    280: "Stream socket",
    294: "Datagram socket",
    336: "Mạng có nhiều đường đi dự phòng",
    358: "Internet là network of networks",
    446: "Link Layer",
    486: "Yêu cầu truyền tải của các ứng dụng",
    499: "Cấu trúc URL",
    526: "HTTP status code",
    558: "So sánh HTTP/1.1 và HTTP/2",
    569: "Hệ thống thư điện tử",
    610: "Hệ thống DNS phân cấp",
    650: "Bus topology",
    659: "Ring topology",
    668: "Star topology",
    676: "Mesh topology",
    694: "ARP",
    698: "Packet sniffing",
    703: "IP spoofing",
}


def images_in_paragraph(document: Document, paragraph) -> list[tuple[str, bytes]]:
    images: list[tuple[str, bytes]] = []
    for blip in paragraph._p.xpath(".//a:blip"):
        relationship_id = blip.get(qn("r:embed"))
        part = document.part.related_parts[relationship_id]
        images.append((Path(str(part.partname)).name, part.blob))
    return images


def inline_markup(text: str) -> str:
    """Chỉ thêm Markdown emphasis; không sửa câu chữ nguồn."""
    match = re.match(r"^(B\d+)\s*:\s*(.*)$", text, flags=re.IGNORECASE)
    if match:
        return f"**{match.group(1)}:** {match.group(2)}"

    label, separator, value = text.partition(":")
    if (
        separator
        and 0 < len(label) <= 65
        and not label.lower().startswith(("http", "https"))
        and not re.match(r"^[A-Za-z]:\\", text)
    ):
        return f"**{label}:**{value}"

    return text


def paragraph_markdown(index: int, text: str) -> str:
    text = text.strip()
    if not text:
        return ""
    if index in H1:
        return f"# {text}"
    if index in H2:
        return f"## {text}"
    if index in H3:
        return f"### {text}"

    marked = inline_markup(text)
    if re.match(r"^B\d+\s*:", text, flags=re.IGNORECASE):
        return f"1. {marked}"
    return f"- {marked}"


def escape_table_cell(text: str) -> str:
    return text.replace("|", "\\|").replace("\r", " ").replace("\n", "<br>")


def table_markdown(document: Document, config: dict) -> list[str]:
    headers = config["headers"]
    result = [
        f"| {headers[0]} | {headers[1]} |",
        "|---|---|",
    ]
    for label, index in zip(config["labels"], config["indices"]):
        original = document.paragraphs[index].text.strip()
        result.append(
            f"| {escape_table_cell(label)} | {escape_table_cell(original)} |"
        )
    return result


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if not SOURCE.exists():
        raise SystemExit(f"Không tìm thấy file nguồn: {SOURCE}")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    ASSET_DIR.mkdir(parents=True, exist_ok=True)

    document = Document(SOURCE)
    output: list[str] = [
        "<!--",
        "Nguồn: Google Docs",
        "Nội dung câu chữ và hình ảnh được giữ nguyên; chỉ bổ sung Markdown để dễ đọc.",
        "-->",
        "",
    ]
    image_count = 0

    for index, paragraph in enumerate(document.paragraphs):
        if index in SECTION_BEFORE:
            output.extend([f"### {SECTION_BEFORE[index]}", ""])

        if index in TABLES:
            output.extend([*table_markdown(document, TABLES[index]), ""])

        text = paragraph.text
        block = "" if index in TABLE_MEMBER_INDICES else paragraph_markdown(index, text)
        if block:
            output.extend([block, ""])

        paragraph_images = images_in_paragraph(document, paragraph)
        for image_name, blob in paragraph_images:
            (ASSET_DIR / image_name).write_bytes(blob)
            image_count += 1
            caption = IMAGE_CAPTIONS.get(index, f"Hình {image_count}")
            output.extend(
                [
                    f"![{caption}](assets/{image_name})",
                    "",
                    f"*{caption}*",
                    "",
                ]
            )

    markdown = "\n".join(output)
    markdown = re.sub(r"\n{3,}", "\n\n", markdown).rstrip() + "\n"
    OUTPUT_FILE.write_text(markdown, encoding="utf-8")

    print(f"Đã tạo: {OUTPUT_FILE}")
    print(f"Số paragraph trong DOCX: {len(document.paragraphs)}")
    print(f"Số ảnh đã chép: {image_count}")
    print(f"Số file ảnh trong assets: {len(list(ASSET_DIR.glob('*')))}")


if __name__ == "__main__":
    main()
