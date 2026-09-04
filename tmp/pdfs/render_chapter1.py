from pathlib import Path

import pypdfium2 as pdfium
from PIL import Image, ImageDraw


PDF_PATH = Path(r"C:\Users\phudn6\Downloads\System Design Interview by Alex Xu.pdf")
OUTPUT_DIR = Path(r"D:\tech\springboot-learning\tmp\pdfs\chapter1-pages")
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

pdf = pdfium.PdfDocument(PDF_PATH)
thumbnails = []

for page_index in range(4, 33):
    page = pdf[page_index]
    image = page.render(scale=1.1).to_pil().convert("RGB")
    output_path = OUTPUT_DIR / f"page-{page_index + 1:03d}.png"
    image.save(output_path, optimize=True)

    thumb = image.copy()
    thumb.thumbnail((280, 380))
    card = Image.new("RGB", (300, 420), "white")
    card.paste(thumb, ((300 - thumb.width) // 2, 25))
    ImageDraw.Draw(card).text((12, 5), f"PDF page {page_index + 1}", fill="black")
    thumbnails.append(card)

columns = 5
rows = (len(thumbnails) + columns - 1) // columns
sheet = Image.new("RGB", (columns * 300, rows * 420), "#dddddd")
for index, thumbnail in enumerate(thumbnails):
    sheet.paste(thumbnail, ((index % columns) * 300, (index // columns) * 420))
sheet.save(OUTPUT_DIR / "contact-sheet.png", optimize=True)

print(f"Rendered {len(thumbnails)} pages to {OUTPUT_DIR}")
