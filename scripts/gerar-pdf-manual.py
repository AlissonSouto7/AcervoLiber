"""
Gera o PDF do MANUAL-PROFESSOR.md usando ReportLab.

Por que script proprio (em vez de pandoc/wkhtmltopdf): essas dependencias
externas exigem instalacao manual no servidor da escola/voluntario. ReportLab
e Markdown ambos vem via pip (sem deps de sistema) — qualquer maquina com
Python ja roda.

Como rodar:
    pip install markdown reportlab
    python scripts/gerar-pdf-manual.py

Saida: docs/MANUAL-PROFESSOR.pdf
"""
import re
import sys
from pathlib import Path

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_JUSTIFY
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle,
    ListFlowable, ListItem, KeepTogether
)


ROOT = Path(__file__).resolve().parent.parent
MD_PATH = ROOT / "docs" / "MANUAL-PROFESSOR.md"
PDF_PATH = ROOT / "docs" / "MANUAL-PROFESSOR.pdf"


# Estilos
STYLES = getSampleStyleSheet()
BODY = ParagraphStyle(
    "Body",
    parent=STYLES["BodyText"],
    fontName="Helvetica",
    fontSize=10.5,
    leading=15,
    spaceBefore=4,
    spaceAfter=4,
    alignment=TA_JUSTIFY,
)
H1 = ParagraphStyle(
    "H1",
    parent=STYLES["Heading1"],
    fontName="Helvetica-Bold",
    fontSize=22,
    leading=28,
    spaceBefore=20,
    spaceAfter=12,
    textColor=colors.HexColor("#1f4e79"),
)
H2 = ParagraphStyle(
    "H2",
    parent=STYLES["Heading2"],
    fontName="Helvetica-Bold",
    fontSize=16,
    leading=22,
    spaceBefore=18,
    spaceAfter=10,
    textColor=colors.HexColor("#2e75b6"),
)
H3 = ParagraphStyle(
    "H3",
    parent=STYLES["Heading3"],
    fontName="Helvetica-Bold",
    fontSize=13,
    leading=18,
    spaceBefore=12,
    spaceAfter=6,
    textColor=colors.HexColor("#1f4e79"),
)
SUBTITLE = ParagraphStyle(
    "Subtitle",
    parent=STYLES["Italic"],
    fontName="Helvetica-Oblique",
    fontSize=12,
    alignment=TA_CENTER,
    spaceAfter=20,
    textColor=colors.grey,
)
QUOTE = ParagraphStyle(
    "Quote",
    parent=BODY,
    leftIndent=20,
    rightIndent=20,
    textColor=colors.HexColor("#444444"),
    backColor=colors.HexColor("#f0f7ff"),
    borderColor=colors.HexColor("#2e75b6"),
    borderWidth=0,
    borderPadding=8,
    spaceBefore=8,
    spaceAfter=8,
)


def md_inline_to_para(text):
    """Converte sintaxe inline markdown para HTML do ReportLab."""
    # bold **
    text = re.sub(r"\*\*([^*]+?)\*\*", r"<b>\1</b>", text)
    # italic *
    text = re.sub(r"(?<!\*)\*([^*\n]+?)\*(?!\*)", r"<i>\1</i>", text)
    # inline code `...`
    text = re.sub(
        r"`([^`]+?)`",
        r'<font face="Courier" color="#c7254e" backColor="#f9f2f4">\1</font>',
        text,
    )
    # links [text](url) - so o texto, sem o link real (PDF simples)
    text = re.sub(r"\[([^\]]+?)\]\([^)]+?\)", r"\1", text)
    # escape XML chars do reportlab sem quebrar nossas tags. Estrategia: capturar
    # tags inteiras (incluindo atributos ate o `>` final) como placeholders.
    placeholders = []

    def stash(match):
        placeholders.append(match.group(0))
        return f"\x00{len(placeholders) - 1}\x00"

    # Ordem importa: tags abertura de font (com atributos) > b/i simples > closings
    text = re.sub(r"<font [^>]*>", stash, text)
    text = re.sub(r"</?(b|i|font)>", stash, text)

    # agora escapa o resto
    text = text.replace("&", "&amp;")
    text = text.replace("<", "&lt;").replace(">", "&gt;")

    # restaura as tags
    def restore(match):
        return placeholders[int(match.group(1))]

    text = re.sub(r"\x00(\d+)\x00", restore, text)
    return text


def parse_table(lines, idx):
    """Le uma tabela markdown comecando em lines[idx]. Retorna (rows, novo_idx)."""
    rows = []
    while idx < len(lines) and lines[idx].lstrip().startswith("|"):
        line = lines[idx].strip()
        if re.match(r"^\|[\s\-:|]+\|$", line):
            idx += 1
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        rows.append(cells)
        idx += 1
    return rows, idx


def make_table_flowable(rows):
    if not rows:
        return Spacer(0, 0)
    paragraphs = [
        [Paragraph(md_inline_to_para(c), BODY) for c in row]
        for row in rows
    ]
    n_cols = len(rows[0])
    col_widths = [16 * cm / n_cols] * n_cols
    table = Table(paragraphs, colWidths=col_widths, repeatRows=1)
    table.setStyle(
        TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1f4e79")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
            ("FONTSIZE", (0, 0), (-1, 0), 10),
            ("ALIGN", (0, 0), (-1, 0), "LEFT"),
            ("BOTTOMPADDING", (0, 0), (-1, 0), 6),
            ("TOPPADDING", (0, 0), (-1, 0), 6),
            ("BACKGROUND", (0, 1), (-1, -1), colors.HexColor("#f5faff")),
            ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cfd8e3")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 6),
            ("RIGHTPADDING", (0, 0), (-1, -1), 6),
            ("TOPPADDING", (0, 1), (-1, -1), 4),
            ("BOTTOMPADDING", (0, 1), (-1, -1), 4),
        ])
    )
    return KeepTogether([table, Spacer(0, 6)])


def parse_md(md_text):
    """Converte markdown para lista de flowables ReportLab."""
    story = []
    lines = md_text.splitlines()
    i = 0
    in_list = False
    list_items = []

    def flush_list():
        nonlocal list_items, in_list
        if list_items:
            story.append(
                ListFlowable(
                    [ListItem(Paragraph(md_inline_to_para(item), BODY), leftIndent=12)
                     for item in list_items],
                    bulletType="bullet",
                    leftIndent=18,
                    bulletFontSize=9,
                )
            )
            story.append(Spacer(0, 4))
        list_items = []
        in_list = False

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # tabela
        if stripped.startswith("|") and i + 1 < len(lines) and re.match(r"^\|[\s\-:|]+\|$", lines[i + 1].strip()):
            flush_list()
            rows, i = parse_table(lines, i)
            story.append(make_table_flowable(rows))
            continue

        # headings
        if stripped.startswith("# "):
            flush_list()
            text = md_inline_to_para(stripped[2:])
            story.append(Paragraph(text, H1))
        elif stripped.startswith("## "):
            flush_list()
            text = md_inline_to_para(stripped[3:])
            story.append(Paragraph(text, H2))
        elif stripped.startswith("### "):
            flush_list()
            text = md_inline_to_para(stripped[4:])
            story.append(Paragraph(text, H3))
        elif stripped.startswith("---"):
            flush_list()
            story.append(Spacer(0, 12))
        elif stripped.startswith("> "):
            flush_list()
            text = md_inline_to_para(stripped[2:])
            story.append(Paragraph(text, QUOTE))
        elif re.match(r"^[-*] ", stripped):
            text = md_inline_to_para(re.sub(r"^[-*] ", "", stripped))
            list_items.append(text)
            in_list = True
        elif stripped == "":
            flush_list()
            # paragrafo em branco — pequeno espaco
        else:
            flush_list()
            text = md_inline_to_para(stripped)
            # subtitulo (linha imediatamente apos H1 e em italico)
            story.append(Paragraph(text, BODY))
        i += 1

    flush_list()
    return story


def add_page_number(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 9)
    canvas.setFillColor(colors.grey)
    canvas.drawRightString(
        A4[0] - 1.5 * cm, 1 * cm,
        f"AcervoLiber — Manual do Bibliotecario(a)  |  Pagina {doc.page}",
    )
    canvas.restoreState()


def main():
    if not MD_PATH.exists():
        print(f"Nao encontrei: {MD_PATH}", file=sys.stderr)
        sys.exit(1)

    md = MD_PATH.read_text(encoding="utf-8")
    story = parse_md(md)

    doc = SimpleDocTemplate(
        str(PDF_PATH),
        pagesize=A4,
        leftMargin=2 * cm,
        rightMargin=2 * cm,
        topMargin=2 * cm,
        bottomMargin=2 * cm,
        title="Manual do AcervoLiber",
        author="AcervoLiber",
    )
    doc.build(story, onFirstPage=add_page_number, onLaterPages=add_page_number)
    print(f"PDF gerado: {PDF_PATH}")


if __name__ == "__main__":
    main()
