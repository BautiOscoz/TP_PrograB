from pathlib import Path
from PIL import Image, ImageDraw

root = Path(r"C:\Java\TP_PrograB\tmp\pdfs\rendered")
pages = sorted(root.glob("page-*.png"))
out = root.parent / "contact_sheets"
out.mkdir(parents=True, exist_ok=True)

for batch_index in range(0, len(pages), 4):
    batch = pages[batch_index:batch_index + 4]
    thumbs = []
    for page in batch:
        image = Image.open(page).convert("RGB")
        image.thumbnail((595, 842))
        thumbs.append((page, image.copy()))
    sheet = Image.new("RGB", (1210, 1720), "#d9dee3")
    draw = ImageDraw.Draw(sheet)
    for index, (page, image) in enumerate(thumbs):
        x = 5 + (index % 2) * 600
        y = 25 + (index // 2) * 850
        sheet.paste(image, (x, y))
        draw.text((x, 5 + (index // 2) * 850), page.stem, fill="black")
    sheet.save(out / f"contact-{batch_index // 4 + 1}.jpg", quality=90)

print(len(list(out.glob("contact-*.jpg"))))
