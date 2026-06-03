"""
EHRAssist V2 - Full Stack Documentation PDF Generator
Generates a professional, color-coded full-stack documentation PDF
covering Backend (Spring Boot + HAPI FHIR), Frontend (React 19 + Vite),
AI Integration (Azure OpenAI), and Deployment topology.
"""

from datetime import datetime
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm, mm
from reportlab.platypus import (
    BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, KeepTogether, Image, HRFlowable, ListFlowable, ListItem,
)
from reportlab.pdfgen import canvas
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont


# =============================================================================
# FONT REGISTRATION  (Windows TTFs — full Unicode coverage incl. box drawing)
# =============================================================================
def _register_fonts():
    fonts_dir = r"C:\Windows\Fonts"
    pdfmetrics.registerFont(TTFont("UIBody",     f"{fonts_dir}\\arial.ttf"))
    pdfmetrics.registerFont(TTFont("UIBody-B",   f"{fonts_dir}\\arialbd.ttf"))
    pdfmetrics.registerFont(TTFont("UIBody-I",   f"{fonts_dir}\\ariali.ttf"))
    pdfmetrics.registerFont(TTFont("UIBody-BI",  f"{fonts_dir}\\arialbi.ttf"))
    pdfmetrics.registerFontFamily(
        "UIBody",
        normal="UIBody", bold="UIBody-B",
        italic="UIBody-I", boldItalic="UIBody-BI",
    )
    pdfmetrics.registerFont(TTFont("UICode",     f"{fonts_dir}\\consola.ttf"))
    pdfmetrics.registerFont(TTFont("UICode-B",   f"{fonts_dir}\\consolab.ttf"))
    pdfmetrics.registerFontFamily(
        "UICode",
        normal="UICode", bold="UICode-B",
        italic="UICode", boldItalic="UICode-B",
    )

_register_fonts()

FONT_BODY = "UIBody"
FONT_BOLD = "UIBody-B"
FONT_CODE = "UICode"


# =============================================================================
# COLOR PALETTE  (clinical / healthcare professional theme)
# =============================================================================
PRIMARY      = colors.HexColor("#0B3D5C")   # deep navy / clinical
ACCENT       = colors.HexColor("#0E8FB8")   # teal blue
ACCENT_LIGHT = colors.HexColor("#E6F3F8")   # very light teal
SUCCESS      = colors.HexColor("#1F8A4C")   # medical green
WARNING      = colors.HexColor("#C8741B")   # amber
DANGER       = colors.HexColor("#B23B3B")   # soft red
TEXT_DARK    = colors.HexColor("#1B2733")
TEXT_MUTED   = colors.HexColor("#5C6873")
BG_SOFT      = colors.HexColor("#F4F7FA")
ROW_ALT      = colors.HexColor("#F8FAFC")
BORDER       = colors.HexColor("#D4DCE3")
CODE_BG      = colors.HexColor("#0F1B27")
CODE_FG      = colors.HexColor("#D6E4F0")

PAGE_W, PAGE_H = A4
MARGIN_L = 1.8 * cm
MARGIN_R = 1.8 * cm
MARGIN_T = 2.2 * cm
MARGIN_B = 2.0 * cm


# =============================================================================
# PARAGRAPH STYLES
# =============================================================================
ss = getSampleStyleSheet()

style_h1 = ParagraphStyle(
    "H1", parent=ss["Heading1"], fontName=FONT_BOLD, fontSize=22,
    textColor=PRIMARY, spaceBefore=8, spaceAfter=12, leading=26,
    keepWithNext=1,
)
style_h2 = ParagraphStyle(
    "H2", parent=ss["Heading2"], fontName=FONT_BOLD, fontSize=15,
    textColor=PRIMARY, spaceBefore=14, spaceAfter=6, leading=18,
    keepWithNext=1,
)
style_h3 = ParagraphStyle(
    "H3", parent=ss["Heading3"], fontName=FONT_BOLD, fontSize=12,
    textColor=ACCENT, spaceBefore=10, spaceAfter=4, leading=15,
    keepWithNext=1,
)
style_h4 = ParagraphStyle(
    "H4", parent=ss["Heading4"], fontName=FONT_BOLD, fontSize=10.5,
    textColor=TEXT_DARK, spaceBefore=6, spaceAfter=2, leading=13,
    keepWithNext=1,
)
style_body = ParagraphStyle(
    "Body", parent=ss["BodyText"], fontName=FONT_BODY, fontSize=9.5,
    textColor=TEXT_DARK, leading=14, spaceAfter=6, alignment=TA_JUSTIFY,
)
style_body_left = ParagraphStyle(
    "BodyL", parent=style_body, alignment=TA_LEFT,
)
style_small = ParagraphStyle(
    "Small", parent=style_body, fontSize=8.5, textColor=TEXT_MUTED, leading=12,
)
style_bullet = ParagraphStyle(
    "Bullet", parent=style_body, leftIndent=14, bulletIndent=2,
    fontSize=9.5, leading=13, spaceAfter=2, alignment=TA_LEFT,
)
style_code = ParagraphStyle(
    "Code", parent=ss["Code"], fontName=FONT_CODE, fontSize=8.5,
    textColor=CODE_FG, backColor=CODE_BG, leading=11,
    leftIndent=8, rightIndent=8, spaceBefore=4, spaceAfter=4,
    borderPadding=6,
)
style_table_cell = ParagraphStyle(
    "TblCell", parent=ss["BodyText"], fontName=FONT_BODY, fontSize=8.5,
    textColor=TEXT_DARK, leading=11, alignment=TA_LEFT,
)
style_table_cell_bold = ParagraphStyle(
    "TblCellBold", parent=style_table_cell, fontName=FONT_BOLD,
    textColor=PRIMARY,
)
style_table_header = ParagraphStyle(
    "TblHdr", parent=ss["BodyText"], fontName=FONT_BOLD, fontSize=9,
    textColor=colors.white, leading=11, alignment=TA_LEFT,
)
style_toc_l1 = ParagraphStyle(
    "TOC1", fontName=FONT_BOLD, fontSize=11, textColor=PRIMARY,
    leading=18, spaceAfter=2,
)
style_toc_l2 = ParagraphStyle(
    "TOC2", fontName=FONT_BODY, fontSize=9.5, textColor=TEXT_DARK,
    leftIndent=18, leading=14, spaceAfter=1,
)
style_cover_title = ParagraphStyle(
    "CoverTitle", fontName=FONT_BOLD, fontSize=34,
    textColor=colors.white, alignment=TA_CENTER, leading=38,
)
style_cover_sub = ParagraphStyle(
    "CoverSub", fontName=FONT_BODY, fontSize=14,
    textColor=colors.HexColor("#CFE3EE"), alignment=TA_CENTER, leading=20,
)
style_cover_tag = ParagraphStyle(
    "CoverTag", fontName="UIBody-I", fontSize=11,
    textColor=colors.HexColor("#9CC1D2"), alignment=TA_CENTER, leading=16,
)


# =============================================================================
# PAGE TEMPLATES  (Cover, Content)
# =============================================================================
def draw_cover_background(canv, doc):
    """Clean cover background: solid navy with thin top/bottom accent strips."""
    canv.saveState()

    canv.setFillColor(PRIMARY)
    canv.rect(0, 0, PAGE_W, PAGE_H, stroke=0, fill=1)

    canv.setFillColor(ACCENT)
    canv.rect(0, PAGE_H - 6 * mm, PAGE_W, 6 * mm, stroke=0, fill=1)
    canv.rect(0, 0, PAGE_W, 6 * mm, stroke=0, fill=1)

    canv.setFillColor(colors.HexColor("#7FB7CC"))
    canv.setFont(FONT_BODY, 8.5)
    canv.drawString(MARGIN_L, 11 * mm,
                    "Internal Documentation  |  R Systems  |  EHRAssist V2 Platform")
    canv.drawRightString(PAGE_W - MARGIN_R, 11 * mm,
                         "Confidential")

    canv.restoreState()


def draw_content_chrome(canv, doc):
    """Header + footer chrome for every content page."""
    canv.saveState()

    canv.setFillColor(PRIMARY)
    canv.rect(0, PAGE_H - 14 * mm, PAGE_W, 14 * mm, stroke=0, fill=1)
    canv.setFillColor(ACCENT)
    canv.rect(0, PAGE_H - 16 * mm, PAGE_W, 2 * mm, stroke=0, fill=1)

    canv.setFillColor(colors.white)
    canv.setFont(FONT_BOLD, 10)
    canv.drawString(MARGIN_L, PAGE_H - 9 * mm, "EHRAssist V2")
    canv.setFont(FONT_BODY, 9)
    canv.setFillColor(colors.HexColor("#CFE3EE"))
    canv.drawString(MARGIN_L + 26 * mm, PAGE_H - 9 * mm,
                    "Full Stack Project Documentation")
    canv.setFont("UIBody-I", 8.5)
    canv.drawRightString(PAGE_W - MARGIN_R, PAGE_H - 9 * mm,
                         "Spring Boot  •  React 19  •  Azure OpenAI  •  HAPI FHIR R4")

    canv.setStrokeColor(BORDER)
    canv.setLineWidth(0.4)
    canv.line(MARGIN_L, 14 * mm, PAGE_W - MARGIN_R, 14 * mm)
    canv.setFont(FONT_BODY, 8)
    canv.setFillColor(TEXT_MUTED)
    canv.drawString(MARGIN_L, 9 * mm,
                    "EHRAssist V2 — Full Stack Documentation")
    canv.drawRightString(PAGE_W - MARGIN_R, 9 * mm,
                         f"Page {doc.page}")
    canv.restoreState()


# =============================================================================
# REUSABLE BUILDERS
# =============================================================================
def p(text, style=style_body):
    return Paragraph(text, style)


def section_banner(number, title, color=PRIMARY):
    """A colored section banner (number chip + title)."""
    chip = Paragraph(
        f'<font color="white"><b>{number}</b></font>',
        ParagraphStyle("chip", fontName=FONT_BOLD, fontSize=14,
                       textColor=colors.white, alignment=TA_CENTER, leading=18),
    )
    title_para = Paragraph(
        f'<font color="#0B3D5C"><b>{title}</b></font>',
        ParagraphStyle("banTitle", fontName=FONT_BOLD, fontSize=16,
                       textColor=PRIMARY, leading=20),
    )
    tbl = Table(
        [[chip, title_para]],
        colWidths=[12 * mm, None],
        rowHeights=[12 * mm],
    )
    tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, 0), color),
        ("BACKGROUND", (1, 0), (1, 0), ACCENT_LIGHT),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (0, 0), (0, 0), "CENTER"),
        ("LEFTPADDING", (1, 0), (1, 0), 12),
        ("BOX", (0, 0), (-1, -1), 0, colors.white),
    ]))
    return tbl


def data_table(header_row, body_rows, col_widths=None, header_color=PRIMARY,
               first_col_bold=True, zebra=True):
    """Standard data table with header + zebra rows + soft borders."""
    h = [Paragraph(str(c), style_table_header) for c in header_row]
    data = [h]
    for row in body_rows:
        new = []
        for i, c in enumerate(row):
            s = style_table_cell_bold if (first_col_bold and i == 0) else style_table_cell
            new.append(Paragraph(str(c), s))
        data.append(new)

    t = Table(data, colWidths=col_widths, repeatRows=1)
    ts = [
        ("BACKGROUND", (0, 0), (-1, 0), header_color),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LINEBELOW", (0, 0), (-1, 0), 0.6, header_color),
        ("LINEBELOW", (0, 1), (-1, -1), 0.25, BORDER),
        ("BOX", (0, 0), (-1, -1), 0.4, BORDER),
    ]
    if zebra:
        for i in range(1, len(data)):
            if i % 2 == 0:
                ts.append(("BACKGROUND", (0, i), (-1, i), ROW_ALT))
    t.setStyle(TableStyle(ts))
    return t


def callout(title, body, color=ACCENT, icon="i"):
    """Highlighted callout / info box (icon is a single text glyph)."""
    bar = Table(
        [[Paragraph(f'<font color="white" size=14><b>{icon}</b></font>',
                    ParagraphStyle("ico", fontName=FONT_BOLD,
                                   fontSize=14, alignment=TA_CENTER,
                                   textColor=colors.white)),
          Paragraph(f'<b>{title}</b><br/><font size=9>{body}</font>',
                    ParagraphStyle("co", fontName=FONT_BODY, fontSize=10,
                                   textColor=TEXT_DARK, leading=13))]],
        colWidths=[10 * mm, None],
    )
    bar.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, 0), color),
        ("BACKGROUND", (1, 0), (1, 0), ACCENT_LIGHT),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (0, 0), (0, 0), "CENTER"),
        ("LEFTPADDING", (1, 0), (1, 0), 10),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (1, 0), (1, 0), 10),
    ]))
    return bar


def bullets(items):
    """ListFlowable of bulletized paragraphs (explicit Unicode bullet)."""
    return ListFlowable(
        [ListItem(Paragraph(it, style_bullet),
                  bulletColor=ACCENT, leftIndent=14)
         for it in items],
        bulletType="bullet",
        bulletFontName=FONT_BOLD,
        bulletFontSize=9,
        bulletColor=ACCENT,
        start="\u25CF",   # ● BLACK CIRCLE — supported by Arial
        leftIndent=14,
    )


def code_block(text):
    """Dark themed code/text block."""
    safe = (text.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;"))
    safe = safe.replace("\n", "<br/>").replace("  ", "&nbsp;&nbsp;")
    inner = Paragraph(f'<font face="{FONT_CODE}" size=8.5 color="#D6E4F0">{safe}</font>',
                      ParagraphStyle("cb", fontName=FONT_CODE, fontSize=8.5,
                                     leading=11, textColor=CODE_FG))
    t = Table([[inner]], colWidths=[PAGE_W - MARGIN_L - MARGIN_R])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), CODE_BG),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
        ("RIGHTPADDING", (0, 0), (-1, -1), 10),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
        ("BOX", (0, 0), (-1, -1), 0.4, PRIMARY),
    ]))
    return t


def stat_cards(cards):
    """Row of metric/stat cards: cards = [(label, value, color), ...]"""
    row = []
    for label, value, color in cards:
        hexcolor = "#" + color.hexval()[2:]
        cell = Paragraph(
            f'<para align="center"><font size=18 color="{hexcolor}">'
            f'<b>{value}</b></font><br/>'
            f'<font size=8 color="#5C6873">{label}</font></para>',
            ParagraphStyle("stat", fontName=FONT_BODY, alignment=TA_CENTER,
                           leading=22, textColor=TEXT_DARK))
        row.append(cell)
    inner_w = PAGE_W - MARGIN_L - MARGIN_R
    col_w = inner_w / len(cards)
    t = Table([row], colWidths=[col_w] * len(cards), rowHeights=[22 * mm])
    style_rows = [
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("BOX", (0, 0), (-1, -1), 0.4, BORDER),
    ]
    for i in range(len(cards)):
        style_rows.append(("BACKGROUND", (i, 0), (i, 0), BG_SOFT))
        style_rows.append(("LINEBEFORE", (i, 0), (i, 0), 4, cards[i][2]))
    t.setStyle(TableStyle(style_rows))
    return t


def divider():
    return HRFlowable(width="100%", color=BORDER, thickness=0.4,
                      spaceBefore=6, spaceAfter=6)


# =============================================================================
# CONTENT BUILDERS PER SECTION
# =============================================================================
def build_cover():
    elems = []

    brand_mark = Table(
        [[Paragraph(
            '<font color="white"><b>EHR</b></font>',
            ParagraphStyle("bm", fontName=FONT_BOLD, fontSize=18,
                           textColor=colors.white, alignment=TA_CENTER,
                           leading=22))]],
        colWidths=[22 * mm], rowHeights=[14 * mm],
    )
    brand_mark.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), ACCENT),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (0, 0), (-1, -1), "CENTER"),
        ("BOX", (0, 0), (-1, -1), 0, ACCENT),
    ]))
    brand_mark.hAlign = "CENTER"

    elems.append(Spacer(1, 35 * mm))
    elems.append(brand_mark)
    elems.append(Spacer(1, 8 * mm))

    elems.append(Paragraph(
        '<font color="#9CC1D2" size=10>R&nbsp;SYSTEMS&nbsp;&nbsp;|&nbsp;&nbsp;'
        'HEALTHCARE&nbsp;PLATFORM</font>',
        ParagraphStyle("brandtag", fontName=FONT_BOLD, fontSize=10,
                       textColor=colors.HexColor("#9CC1D2"),
                       alignment=TA_CENTER, leading=14)))

    elems.append(Spacer(1, 18 * mm))

    elems.append(Paragraph("EHRAssist V2", style_cover_title))
    elems.append(Spacer(1, 6 * mm))
    elems.append(Paragraph("Full Stack Project Documentation", style_cover_sub))
    elems.append(Spacer(1, 4 * mm))

    divider_tbl = Table(
        [[""]], colWidths=[40 * mm], rowHeights=[1.2],
    )
    divider_tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), ACCENT),
        ("LINEABOVE", (0, 0), (-1, -1), 0, ACCENT),
    ]))
    divider_tbl.hAlign = "CENTER"
    elems.append(divider_tbl)

    elems.append(Spacer(1, 6 * mm))
    elems.append(Paragraph(
        "Spring Boot &nbsp;·&nbsp; React 19 &nbsp;·&nbsp; HAPI FHIR R4 "
        "&nbsp;·&nbsp; Azure OpenAI &nbsp;·&nbsp; SQL Server",
        style_cover_tag))

    elems.append(Spacer(1, 22 * mm))

    label_style = ParagraphStyle(
        "lbl", fontName=FONT_BOLD, fontSize=8.5,
        textColor=colors.HexColor("#7FB7CC"), alignment=TA_LEFT, leading=11)
    value_style = ParagraphStyle(
        "val", fontName=FONT_BODY, fontSize=10.5,
        textColor=colors.white, alignment=TA_LEFT, leading=14)

    info_rows = [
        [Paragraph("BACKEND", label_style),
         Paragraph("Java 21  ·  Spring Boot 3.4.4  ·  HAPI FHIR 8.6.0  ·  HTTPS Port 3001", value_style)],
        [Paragraph("FRONTEND", label_style),
         Paragraph("React 19.2  ·  Vite 8  ·  Chart.js 4.5  ·  Vercel Edge", value_style)],
        [Paragraph("AI&nbsp;ENGINE", label_style),
         Paragraph('Azure OpenAI <font name="UIBody-I">gpt-4.1-mini</font>', value_style)],
        [Paragraph("DEPLOYMENT", label_style),
         Paragraph("Vercel (frontend)  ·  Tomcat @ R Systems (backend, port 3001)", value_style)],
    ]
    info_panel = Table(
        info_rows,
        colWidths=[32 * mm, 130 * mm],
    )
    info_panel.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 14),
        ("RIGHTPADDING", (0, 0), (-1, -1), 14),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#0E5A7F")),
        ("LINEBEFORE", (0, 0), (0, -1), 3, ACCENT),
        ("LINEBELOW", (0, 0), (-1, -2), 0.4, colors.HexColor("#1A6A8F")),
    ]))
    info_panel.hAlign = "CENTER"
    elems.append(info_panel)

    elems.append(Spacer(1, 22 * mm))

    date_tbl = Table(
        [[Paragraph(
            f'<font color="#9CC1D2" size=9><b>DOCUMENT GENERATED</b></font><br/>'
            f'<font color="white" size=11>'
            f'{datetime.now().strftime("%B %d, %Y &nbsp;·&nbsp; %I:%M %p")}'
            f'</font>',
            ParagraphStyle("dt", fontName=FONT_BODY, alignment=TA_CENTER,
                           leading=14, textColor=colors.white))]],
        colWidths=[70 * mm], rowHeights=[16 * mm],
    )
    date_tbl.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (0, 0), (-1, -1), "CENTER"),
        ("LINEABOVE", (0, 0), (-1, -1), 0.6, colors.HexColor("#3F87A6")),
        ("LINEBELOW", (0, 0), (-1, -1), 0.6, colors.HexColor("#3F87A6")),
    ]))
    date_tbl.hAlign = "CENTER"
    elems.append(date_tbl)

    elems.append(PageBreak())
    return elems


def build_toc():
    elems = []
    elems.append(Paragraph("Table of Contents", style_h1))
    elems.append(HRFlowable(width="40%", color=ACCENT, thickness=2,
                            spaceAfter=10))

    toc_entries = [
        ("1", "Project Overview & Architecture", [
            "Executive Summary",
            "Full Stack Architecture",
            "Architecture Patterns",
        ]),
        ("2", "Technology Stack", [
            "Backend Stack",
            "Frontend Stack",
            "Infrastructure & Tooling",
        ]),
        ("3", "Frontend Application", [
            "React 19 + Vite Build System",
            "Routing & Component Architecture",
            "Data Visualization (Chart.js)",
            "Styling Approach",
        ]),
        ("4", "AI Integration (Azure OpenAI)", [
            "Architecture & Edge Function Proxy",
            "AI Use Cases",
            "AI Recommendation Persistence",
            "Semantic Deduplication & High-Risk Detection",
        ]),
        ("5", "Authentication & Security", [
            "JWT-Based Authentication Flow",
            "Role-Based Access Control",
            "AES-256-GCM Encryption",
            "Session Management",
        ]),
        ("6", "Data Standards & Clinical Codes", [
            "LOINC, ICD-10, CPT, RxNorm, SNOMED CT",
            "Code Master Tables",
        ]),
        ("7", "Backend Package Structure", []),
        ("8", "Entity–Relationship Diagram", []),
        ("9", "Database Schema — All Tables", []),
        ("10", "API Registry — All Endpoints", []),
        ("11", "Service Layer Details", []),
        ("12", "Coding Conventions", []),
        ("13", "Deployment Topology", [
            "Frontend Deployment (Vercel)",
            "Backend Deployment (Tomcat at R Systems)",
            "Database & Network",
        ]),
        ("14", "Feature Log", []),
    ]

    for num, title, subs in toc_entries:
        elems.append(Paragraph(f"{num}. &nbsp;{title}", style_toc_l1))
        for s in subs:
            elems.append(Paragraph(f"›&nbsp; {s}", style_toc_l2))
        elems.append(Spacer(1, 2))

    elems.append(PageBreak())
    return elems


def build_section_1():
    e = []
    e.append(section_banner("1", "Project Overview & Architecture"))
    e.append(Spacer(1, 6))

    e.append(p(
        "<b>EHRAssist V2</b> is an end-to-end healthcare platform that brings together "
        "a HAPI-FHIR R4 backend, a modern React 19 single-page application, and an "
        "Azure OpenAI powered intelligence layer. The system serves four user personas "
        "— <b>Patient</b>, <b>Provider</b>, <b>Care Manager</b>, and <b>Admin</b> — "
        "across clinical data management, risk prediction, care-gap evaluation, "
        "AI-assisted recommendations, and outreach workflows. The platform is 100% "
        "compliant with HL7 FHIR R4 and exchanges all clinical data as "
        "<i>application/fhir+json</i>."))

    e.append(Spacer(1, 4))
    e.append(stat_cards([
        ("FHIR Resources", "16", ACCENT),
        ("REST Endpoints", "54+", SUCCESS),
        ("JPA Entities", "41", PRIMARY),
        ("Service Modules", "26", WARNING),
        ("User Roles", "4", DANGER),
    ]))

    e.append(Spacer(1, 10))
    e.append(Paragraph("Full Stack Architecture", style_h2))
    e.append(p(
        "The platform follows a three-tier architecture with a dedicated AI proxy edge "
        "layer. Clients never call Azure OpenAI directly — every prompt is brokered "
        "through a Vercel Edge Function which injects credentials and enforces rate "
        "limits."))

    e.append(Spacer(1, 4))
    e.append(code_block(
        "┌──────────────────────────────────────────────────────────────────────────┐\n"
        "│                       CLIENT  (Browser  /  Mobile)                       │\n"
        "│            React 19 SPA  ·  Vite 8  ·  Chart.js  ·  React Router 7       │\n"
        "└──────────────────────────────────────────────────────────────────────────┘\n"
        "          │  HTTPS + JWT Bearer                  │  /api/chat (HTTPS)\n"
        "          ▼                                      ▼\n"
        "┌──────────────────────────────┐     ┌──────────────────────────────────┐\n"
        "│   Spring Boot REST API       │     │   Vercel Edge Function           │\n"
        "│   (HAPI FHIR R4, port 3001)  │     │   AI Proxy  →  Azure OpenAI       │\n"
        "│   JWT · @PreAuthorize · AES  │     │   Model: gpt-4.1-mini             │\n"
        "└──────────────┬───────────────┘     └──────────────────┬───────────────┘\n"
        "               │ JDBC                                   │ HTTPS\n"
        "               ▼                                        ▼\n"
        "┌──────────────────────────────┐     ┌──────────────────────────────────┐\n"
        "│   SQL Server: EHRAssistData  │     │   Azure OpenAI Service           │\n"
        "│   41 tables · code masters   │     │   Recommendations · Summaries     │\n"
        "└──────────────────────────────┘     └──────────────────────────────────┘"))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Backend Architecture Pattern", style_h3))
    e.append(p(
        "The Spring Boot service follows a clean layered architecture:"))
    e.append(code_block(
        "Controller  →  Service (Interface + Impl)  →  Repository  →  SQL Server\n"
        "                       │\n"
        "                       ▼\n"
        "             Mapper Layer (Entity ↔ FHIR Resource)\n"
        "                       │\n"
        "                       ▼\n"
        "       FHIR JSON Response  (via FhirResponseHelper / BundleBuilder)"))

    e.append(Spacer(1, 6))
    e.append(Paragraph("Key Architectural Decisions", style_h3))
    e.append(bullets([
        "All clinical resources follow FHIR R4 standard with FHIR JSON responses (<i>application/fhir+json</i>).",
        "Lists are returned as FHIR <b>Bundle (searchset)</b> with pagination via <b>BundleBuilder</b>.",
        "Errors are returned as FHIR <b>OperationOutcome</b> through the <b>GlobalExceptionHandler</b>.",
        "<b>BaseEntity</b> (@MappedSuperclass) provides id (UUID), version, createdAt, updatedAt.",
        "Code master tables centralize LOINC, ICD-10, CPT, and medication reference data.",
        "<code>verified_at</code> column gates AI recommendation visibility — no separate status column.",
        "AI Instructions use parent/child entities (ordered bullets); AI Actions use a single flat entity.",
        "Interface projections are used for lightweight native read-only queries.",
        "N+1 prevention via <b>JOIN FETCH</b> or native <b>INNER JOIN + projection</b> for heavy list APIs.",
    ]))

    e.append(PageBreak())
    return e


def build_section_2():
    e = []
    e.append(section_banner("2", "Technology Stack"))
    e.append(Spacer(1, 6))

    e.append(Paragraph("Backend Stack", style_h2))
    backend_rows = [
        ["Framework",            "Spring Boot 3.4.4"],
        ["Language",             "Java 21"],
        ["Build Tool",           "Maven"],
        ["FHIR Library",         "HAPI FHIR 8.6.0 (R4)"],
        ["Database",             "Microsoft SQL Server (EHRAssistData)"],
        ["ORM",                  "Spring Data JPA / Hibernate"],
        ["Authentication",       "JWT (HS256) with 8-hour expiry"],
        ["Security",             "Spring Security with @PreAuthorize"],
        ["API Docs",             "SpringDoc OpenAPI (Swagger)"],
        ["Encryption",           "AES-256-GCM (toggle-able)"],
        ["Email",                "Gmail SMTP"],
        ["SSL",                  "HTTPS with custom keystore / truststore (PKCS12)"],
        ["Server Port",          "3001 (HTTPS)"],
    ]
    e.append(data_table(["Component", "Technology / Version"], backend_rows,
                        col_widths=[55 * mm, None]))

    e.append(Spacer(1, 12))
    e.append(Paragraph("Frontend Stack", style_h2))
    frontend_rows = [
        ["Framework",            "React 19.2.5"],
        ["Build Tool",           "Vite 8.0.10"],
        ["Routing",              "React Router DOM 7.14.2 (BrowserRouter, Routes, Route)"],
        ["Charts & Visuals",     "Chart.js 4.5.1 + react-chartjs-2 5.3.1 (Bar, Line, Pie, Doughnut)"],
        ["Styling",              "Pure CSS — modularized per component (no CSS framework)"],
        ["State / Session",      "localStorage with <code>p360_</code> prefix · 8-hour timeout"],
        ["HTTP",                 "Fetch API with Authorization: Bearer &lt;JWT&gt;"],
        ["Encryption (Client)",  "AES-256-GCM decrypt (optional, toggled by <code>VITE_DECRYPT_KEY</code>)"],
        ["Hosting",              "Vercel (static + Edge Functions)"],
        ["Source",               "GitHub — rishabh-r/patient360"],
    ]
    e.append(data_table(["Component", "Technology / Version"], frontend_rows,
                        col_widths=[55 * mm, None], header_color=ACCENT))

    e.append(Spacer(1, 12))
    e.append(Paragraph("AI &amp; Intelligence Layer", style_h2))
    ai_rows = [
        ["AI Provider",          "Azure OpenAI"],
        ["Model",                "<b>gpt-4.1-mini</b>"],
        ["Proxy",                "Vercel Edge Function — <code>/api/chat</code>"],
        ["Auth (AI calls)",      "Server-side API key injected by Edge Function (never exposed to client)"],
        ["Use Cases",            "Health status, summaries, daily tasks, follow-up instructions, recommended actions, dedup, risk flags"],
    ]
    e.append(data_table(["Component", "Technology / Version"], ai_rows,
                        col_widths=[55 * mm, None], header_color=SUCCESS))

    e.append(Spacer(1, 12))
    e.append(Paragraph("Infrastructure &amp; Deployment", style_h2))
    infra_rows = [
        ["Frontend Hosting",     "Vercel (frontend + AI Edge Function)"],
        ["Backend Hosting",      "Apache Tomcat on local R Systems machine (port 3001)"],
        ["Backend Access",       "R Systems VPN + Remote Desktop Connection (IP 10.131.58.59)"],
        ["Database Host",        "SQL Server at 10.131.58.62:1433 (internal network)"],
        ["Source Control",       "Git / GitHub"],
        ["CI / Build",           "Maven (backend) · Vite (frontend)"],
    ]
    e.append(data_table(["Component", "Detail"], infra_rows,
                        col_widths=[55 * mm, None], header_color=WARNING))

    e.append(PageBreak())
    return e


def build_section_3_frontend():
    e = []
    e.append(section_banner("3", "Frontend Application", color=ACCENT))
    e.append(Spacer(1, 6))

    e.append(p(
        "The frontend is a single-page <b>React 19</b> application built and bundled "
        "with <b>Vite 8</b>. It is deployed to Vercel as static assets together with a "
        "single Edge Function that brokers AI requests. The UI is organized by user "
        "persona — Patient, Provider, Care Manager, Admin — each with role-specific "
        "dashboards, clinical timelines, charts, and AI-driven panels."))

    e.append(Spacer(1, 6))
    e.append(Paragraph("Build &amp; Tooling", style_h3))
    e.append(bullets([
        "<b>Vite 8.0.10</b> — fast dev server with HMR, optimized production builds, environment variable handling via <code>import.meta.env</code>.",
        "<b>React 19.2.5</b> — concurrent rendering, Server Components-ready APIs, automatic batching, improved Suspense.",
        "<b>react-dom 19.2.5</b> — DOM renderer paired with React 19.",
        "<b>Module-based imports</b> — ES modules everywhere; zero CommonJS in app code.",
    ]))

    e.append(Paragraph("Routing", style_h3))
    e.append(p(
        "Client-side routing uses <b>react-router-dom 7.14.2</b> with "
        "<code>BrowserRouter</code>, <code>Routes</code>, and <code>Route</code>. "
        "Routes are guarded by a role-based wrapper that reads the cached JWT payload "
        "from <code>localStorage</code> (prefix <code>p360_</code>) and redirects to "
        "<code>/login</code> when the 8-hour session window expires."))

    e.append(Paragraph("Data Visualization", style_h3))
    e.append(p(
        "All charts are rendered with <b>Chart.js 4.5.1</b> via the "
        "<b>react-chartjs-2 5.3.1</b> wrapper. The chart families in use are:"))
    e.append(bullets([
        "<b>Bar</b> charts — encounters by month, care-gap counts per organization.",
        "<b>Line</b> charts — vitals trends (BP, glucose, weight) over time.",
        "<b>Pie</b> / <b>Doughnut</b> charts — risk stratification, condition mix, compliance ratios.",
    ]))

    e.append(Paragraph("Styling", style_h3))
    e.append(p(
        "<b>Pure CSS</b> only — no Tailwind, no styled-components, no UI framework. "
        "Each component owns a co-located <code>.css</code> file, keeping concerns "
        "modular and the bundle small."))

    e.append(Paragraph("Network &amp; Security on the Client", style_h3))
    e.append(bullets([
        "Every request to the Spring Boot API is sent over <b>HTTPS</b> with an "
        "<code>Authorization: Bearer &lt;JWT&gt;</code> header.",
        "If <code>VITE_DECRYPT_KEY</code> is configured, responses are transparently "
        "decrypted with <b>AES-256-GCM</b> before being handed to React state.",
        "AI requests go to the local edge route <code>/api/chat</code> — the Azure "
        "OpenAI key is never present in the browser bundle.",
    ]))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Key NPM Dependencies", style_h3))
    e.append(data_table(
        ["Package", "Version", "Purpose"],
        [
            ["react", "19.2.5", "Core UI library"],
            ["react-dom", "19.2.5", "DOM renderer for React"],
            ["react-router-dom", "7.14.2", "Client-side routing"],
            ["chart.js", "4.5.1", "Charting engine"],
            ["react-chartjs-2", "5.3.1", "React bindings for Chart.js"],
            ["vite", "8.0.10", "Build tool / dev server"],
        ],
        col_widths=[45 * mm, 25 * mm, None], header_color=ACCENT,
    ))

    e.append(PageBreak())
    return e


def build_section_4_ai():
    e = []
    e.append(section_banner("4", "AI Integration (Azure OpenAI)", color=SUCCESS))
    e.append(Spacer(1, 6))

    e.append(p(
        "EHRAssist V2 integrates <b>Azure OpenAI</b> using the <b>gpt-4.1-mini</b> "
        "model. All AI traffic is proxied through a single <b>Vercel Edge Function</b> "
        "(<code>/api/chat</code>) so that secrets stay server-side and the browser only "
        "ever talks to a same-origin endpoint."))

    e.append(Spacer(1, 4))
    e.append(callout(
        "Why an Edge Function proxy?",
        "Keeping the Azure OpenAI key on the edge prevents key leakage, lets us inject "
        "system prompts consistently, allows lightweight throttling, and gives the "
        "frontend a clean same-origin URL — no CORS, no token plumbing.",
        color=SUCCESS, icon="+",
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Request Flow", style_h3))
    e.append(code_block(
        "React component (e.g. HealthSummaryCard)\n"
        "        │  POST /api/chat   { messages, context }\n"
        "        ▼\n"
        "Vercel Edge Function (/api/chat)\n"
        "        │  injects AZURE_OPENAI_API_KEY + endpoint\n"
        "        │  POST {endpoint}/openai/deployments/gpt-4.1-mini/chat/completions\n"
        "        ▼\n"
        "Azure OpenAI Service  →  streamed / batched response\n"
        "        ▼\n"
        "Edge function returns JSON to the browser\n"
        "        ▼\n"
        "Component renders AI output; selected outputs are persisted by\n"
        "the Spring Boot backend (AiRecommendationInstructions, AiRecommendedAction,\n"
        "P360 AI Recommended Actions tables)."))

    e.append(Spacer(1, 8))
    e.append(Paragraph("AI Use Cases", style_h2))
    e.append(data_table(
        ["#", "Feature", "What the AI Produces"],
        [
            ["1", "Health Status Assessment", "Stratifies a patient as stable / monitor / high-risk from vitals, labs and conditions."],
            ["2", "Health Summary", "Plain-language paragraph summarizing the chart for a provider or patient."],
            ["3", "Daily Tasks", "Personalized daily action list (medications, walks, vitals checks)."],
            ["4", "Appointment Summary", "Concise pre-visit briefing for the practitioner."],
            ["5", "Follow-up Instructions", "Discharge / visit follow-up steps in patient-friendly language."],
            ["6", "Recommended Actions", "Care actions with priority + rationale → stored in <code>patient_360_ai_recommended_actions</code>."],
            ["7", "Semantic Deduplication", "Detects duplicate clinical statements / notes that vary only in wording."],
            ["8", "High-Risk Patient Detection", "Flags patients matching deterioration patterns for the risk feed."],
            ["9", "HEDIS-style Logic", "Original HEDIS prompt was replaced with deterministic custom logic for auditability."],
        ],
        col_widths=[8 * mm, 50 * mm, None], header_color=SUCCESS,
    ))

    e.append(Spacer(1, 10))
    e.append(Paragraph("Persistence of AI Output (Backend)", style_h2))
    e.append(p(
        "Output that needs to be reviewed, audited or shown later is persisted by the "
        "Spring Boot backend. The <code>verified_at</code> column gates visibility — "
        "AI content only surfaces to clinicians after it has been verified."))
    e.append(data_table(
        ["Table", "Shape", "Purpose"],
        [
            ["ai_recommendation_instructions",
             "Parent + child (1:N payload)",
             "Ordered bullet instructions (e.g. discharge steps). Mapped to FHIR Communication."],
            ["ai_recommendation_instructions_payload",
             "Child rows",
             "Sequenced text payload lines tied to the parent recommendation."],
            ["ai_recommended_action",
             "Flat",
             "Single AI action with title, description, priority, urgency. FHIR Communication."],
            ["patient_360_ai_recommended_actions",
             "Flat (P360 portal)",
             "Task-queue items with status, priority, rationale, due date."],
            ["care_coordination_notes",
             "Notes",
             "Care manager notes linked to a P360 AI action via <code>recommendation_note_id</code>."],
            ["mark_reviewed",
             "Flag",
             "Marks a patient as reviewed by a care manager after AI insights triage."],
        ],
        col_widths=[55 * mm, 40 * mm, None], header_color=SUCCESS,
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Security &amp; Compliance Posture", style_h3))
    e.append(bullets([
        "Azure OpenAI API key resides only in the Edge Function environment — never in client code.",
        "Patient identifiers in prompts are limited to internal UUIDs; no PHI fields are sent unnecessarily.",
        "Every persisted AI artifact carries <code>patient_id</code> + <code>practitioner_id</code> for auditing.",
        "AI responses that drive clinical actions are gated by <code>verified_at</code> before being shown.",
        "All AI traffic is logged with FHIR audit entries when it leads to a backend write.",
    ]))

    e.append(PageBreak())
    return e


def build_section_5_auth():
    e = []
    e.append(section_banner("5", "Authentication &amp; Security"))
    e.append(Spacer(1, 6))

    e.append(Paragraph("JWT Authentication Flow", style_h2))
    e.append(code_block(
        "POST /api/v1/users/login   (email + password)\n"
        "        │\n"
        "        ▼\n"
        "Validate credentials\n"
        "        │\n"
        "        ▼\n"
        "Generate JWT token  (userId, email, role, refId)   — HS256, 8h expiry\n"
        "        │\n"
        "        ▼\n"
        "Return  { token, userId, email, role, refId }\n"
        "\n"
        "Subsequent requests:\n"
        "  Authorization: Bearer <JWT token>\n"
        "        │\n"
        "        ▼\n"
        "JwtAuthFilter validates token  →  SecurityContext\n"
        "        │\n"
        "        ▼\n"
        "@PreAuthorize checks role on each endpoint"))

    e.append(Spacer(1, 10))
    e.append(Paragraph("User Roles", style_h2))
    e.append(data_table(
        ["Role", "Access Level", "Reference in user_account"],
        [
            ["ADMIN",        "Full system access",                   "None"],
            ["CARE_MANAGER", "Care coordination, org patients",      "practitioner_ref_id → practitioner"],
            ["PROVIDER",     "Clinical data, patient management",    "practitioner_ref_id → practitioner"],
            ["PATIENT",      "Own data access",                      "patient_ref_id → patient"],
        ],
        col_widths=[35 * mm, 65 * mm, None],
    ))
    e.append(p(
        "<i>Note: PROVIDER and CARE_MANAGER both link to the practitioner table via "
        "<code>practitioner_ref_id</code>. The role determines access level and which "
        "endpoints are visible.</i>", style_small))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Transport &amp; Payload Security", style_h2))
    e.append(bullets([
        "<b>HTTPS</b> on port 3001 with custom PKCS12 keystore + truststore.",
        "<b>AES-256-GCM</b> response encryption is toggleable via <code>app.encryption.enabled</code>; "
        "matched by the frontend <code>VITE_DECRYPT_KEY</code> flag.",
        "<b>CORS</b> allow-list configured in <code>application.properties</code> for known Vercel "
        "deployments and local dev origins.",
        "<b>Session lifetime</b> — JWT and localStorage entries both expire after 8 hours.",
    ]))

    e.append(Spacer(1, 6))
    e.append(callout(
        "Encryption Toggle",
        "Set <code>app.encryption.enabled=true</code> on the backend and provide a matching "
        "<code>VITE_DECRYPT_KEY</code> on the frontend to enable AES-256-GCM payload encryption. "
        "Disable both for Postman / dev testing.",
        color=ACCENT, icon="i"))

    e.append(PageBreak())
    return e


def build_section_6_standards():
    e = []
    e.append(section_banner("6", "Data Standards &amp; Clinical Codes"))
    e.append(Spacer(1, 6))

    e.append(p(
        "The platform aligns to standard healthcare terminologies. Reference data is "
        "stored in dedicated <i>code master</i> tables and is joined to clinical entities "
        "by foreign key, keeping the operational tables compact and the standards "
        "centrally maintained."))

    e.append(Spacer(1, 6))
    e.append(data_table(
        ["Standard", "Domain", "Approx. Codes Loaded", "Backing Table"],
        [
            ["LOINC",      "Observations / lab orders", "82+ observation codes",  "observation_code_master"],
            ["ICD-9 / ICD-10", "Diagnoses / conditions", "100+ condition codes",  "condition_code_master"],
            ["CPT",        "Procedures",                "30+ procedure codes",    "procedure_code_master"],
            ["RxNorm",     "Medications",               "45+ drug codes",         "medication_code_master"],
            ["SNOMED CT",  "Allergies (codes)",         "Used inline on allergy_intolerance", "allergy_intolerance"],
        ],
        col_widths=[28 * mm, 42 * mm, 38 * mm, None],
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("How the Codes Are Used", style_h3))
    e.append(bullets([
        "<b>FHIR Coding objects</b> are constructed by mappers using <code>system</code>, "
        "<code>code</code>, and <code>display</code> drawn from the master tables.",
        "<b>Reference ranges</b> (low / high) live on <code>observation_code_master</code> "
        "to support out-of-range flags and vitals display.",
        "<b>Formulary IDs</b> on RxNorm rows enable formulary search via the "
        "<code>formulary-drug-cd</code> filter on <code>MedicationRequest</code>.",
        "<b>CPT category ranges</b> on <code>procedure_code_master</code> drive section "
        "headings in procedure search.",
    ]))

    e.append(PageBreak())
    return e


def build_section_7_packages():
    e = []
    e.append(section_banner("7", "Backend Package Structure"))
    e.append(Spacer(1, 6))
    e.append(p("The backend source tree is organized strictly by layer:"))
    e.append(code_block(
        "src/main/java/ehrAssist/\n"
        "├── EhrAssistV2Application.java        (Main entry point)\n"
        "├── config/\n"
        "│   ├── FhirConfig.java                (FHIR context bean)\n"
        "│   └── OpenApiConfig.java             (Swagger configuration)\n"
        "├── controller/                        (27 REST controllers)\n"
        "├── service/                           (26 service interfaces)\n"
        "│   └── impl/                          (26 service implementations)\n"
        "├── repository/                        (32 JPA repositories)\n"
        "│   └── master/                        (4 code master repositories)\n"
        "├── entity/                            (41 JPA entities)\n"
        "│   └── master/                        (4 code master entities)\n"
        "├── mapper/                            (20 Entity ↔ FHIR mappers)\n"
        "├── dto/\n"
        "│   ├── request/                       (10 request DTOs)\n"
        "│   ├── response/                      (8 response DTOs)\n"
        "│   └── projection/                    (3 interface projections)\n"
        "├── security/\n"
        "│   └── jwt/                           (JwtUtil, JwtAuthFilter)\n"
        "├── exception/                         (GlobalExceptionHandler)\n"
        "├── interceptor/\n"
        "│   ├── audit/                         (FHIR Audit logging)\n"
        "│   └── encryption/                    (AES-256-GCM)\n"
        "└── util/                              (BundleBuilder, FhirResponseHelper)"))
    e.append(PageBreak())
    return e


def build_section_8_er():
    e = []
    e.append(section_banner("8", "Entity–Relationship Diagram"))
    e.append(Spacer(1, 6))
    e.append(p(
        "Entities extending <b>BaseEntity</b> inherit: <code>id</code> (UUID PK), "
        "<code>version</code>, <code>created_at</code>, <code>updated_at</code>."))

    e.append(Paragraph("Core Entities &amp; Relationships", style_h3))
    e.append(code_block(
        "┌──────────────────┐\n"
        "│   ORGANIZATION   │\n"
        "│   (BaseEntity)   │\n"
        "│──────────────────│\n"
        "│ name             │\n"
        "│ type_code        │\n"
        "│ phone            │\n"
        "│ address_city     │\n"
        "│ address_state    │\n"
        "│ active           │\n"
        "└────────┬─────────┘\n"
        "         │\n"
        "  ┌──────┴─────────┬─────────────────────┐\n"
        "  │ 1            1 │                   1 │\n"
        "  ▼                ▼                     ▼\n"
        "┌──────────────┐ ┌──────────────────┐ ┌─────────────────────────────┐\n"
        "│ PRACTITIONER │ │ EPISODE_OF_CARE  │ │ CARE_MANAGER_ORG_MAPPER     │\n"
        "│ (BaseEntity) │ │ (BaseEntity)     │ │─────────────────────────────│\n"
        "│ family_name  │ │ status           │ │ care_manager_id  (FK)       │\n"
        "│ given_name   │ │ type_code        │ │ organization_id  (FK)       │\n"
        "│ gender, npi  │ │ period_start/end │ │ person_id (FK → patient)    │\n"
        "│ specialty    │ │ care_manager_id  │ └─────────────────────────────┘\n"
        "│ phone, email │ │ patient_id       │\n"
        "│ org_id (FK)  │ │ managing_org (FK)│\n"
        "└──────┬───────┘ └────────┬─────────┘\n"
        "       │                  │\n"
        "       │                  ├── EOC_DIAGNOSIS         (episode_id, condition_id, role, rank)\n"
        "       │                  ├── EOC_ENCOUNTER         (composite PK)\n"
        "       │                  └── EOC_STATUS_HISTORY    (status, period)\n"
        "       ▼\n"
        "┌──────────────────────────┐\n"
        "│         PATIENT          │\n"
        "│      (BaseEntity)        │\n"
        "│──────────────────────────│\n"
        "│ active, gender           │\n"
        "│ birth_date, disease      │\n"
        "│ deceased_flag/date       │\n"
        "│ marital_status, language │\n"
        "│ primary_practitioner FK  │  →  Practitioner\n"
        "│ managing_organization FK │  →  Organization\n"
        "└────┬────┬────┬────┬──────┘\n"
        "     │    │    │    │\n"
        "     ▼    ▼    ▼    ▼\n"
        "  NAME  ADDRESS  TELECOM  IDENTIFIER"))

    e.append(PageBreak())

    e.append(Paragraph("Clinical Entities (all hold patient_id FK)", style_h3))
    e.append(code_block(
        "┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐\n"
        "│   OBSERVATION   │  │    CONDITION    │  │     ENCOUNTER        │\n"
        "│  (BaseEntity)   │  │  (BaseEntity)   │  │   (BaseEntity)       │\n"
        "│─────────────────│  │─────────────────│  │──────────────────────│\n"
        "│ patient_id   FK │  │ patient_id   FK │  │ patient_id        FK │\n"
        "│ encounter_id FK │  │ encounter_id FK │  │ practitioner_id   FK │\n"
        "│ performer_id FK │  │ recorder_id  FK │  │ status, class        │\n"
        "│ obs_code_id  FK │  │ condition_code  │  │ type, period         │\n"
        "│ status, value   │  │ clinical_status │  │ admission/discharge  │\n"
        "│ value_unit      │  │ verification    │  │ reason, diagnosis    │\n"
        "│ effective_date  │  │ severity, onset │  │ insurance            │\n"
        "└────────┬────────┘  └────────┬────────┘  └──────────────────────┘\n"
        "         ▼                    ▼\n"
        "┌─────────────────┐  ┌──────────────────┐\n"
        "│OBS_CODE_MASTER  │  │COND_CODE_MASTER  │\n"
        "│ item_id UNIQUE  │  │ code_value       │\n"
        "│ loinc_code      │  │ icd10_code       │\n"
        "│ category        │  │ category         │\n"
        "│ reference_ranges│  └──────────────────┘\n"
        "└─────────────────┘\n"
        "\n"
        "┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐\n"
        "│  MEDICATION_REQUEST │  │      PROCEDURE      │  │ ALLERGY_INTOLERANCE │\n"
        "│    (BaseEntity)     │  │    (BaseEntity)     │  │    (BaseEntity)     │\n"
        "│─────────────────────│  │─────────────────────│  │─────────────────────│\n"
        "│ patient_id      FK  │  │ patient_id      FK  │  │ patient_id      FK  │\n"
        "│ encounter_id    FK  │  │ encounter_id    FK  │  │ clinical_status     │\n"
        "│ requester_id    FK  │  │ performer_id    FK  │  │ verification        │\n"
        "│ medication_code FK  │  │ procedure_code  FK  │  │ type, category      │\n"
        "│ status, intent      │  │ cpt_code, status    │  │ criticality, code   │\n"
        "│ priority, dosage    │  │ body_site, outcome  │  │ onset_date          │\n"
        "│ authored_on         │  │ performed_period    │  └─────────────────────┘\n"
        "└─────────────────────┘  └─────────────────────┘"))

    e.append(Spacer(1, 8))
    e.append(Paragraph("AI &amp; Care Management Entities", style_h3))
    e.append(code_block(
        "┌─────────────────────────────┐    ┌─────────────────────────────┐\n"
        "│ AI_RECOMMENDATION_INSTR     │    │  AI_RECOMMENDED_ACTION       │\n"
        "│─────────────────────────────│    │──────────────────────────────│\n"
        "│ patient_id, practitioner_id │    │ patient_id, practitioner_id  │\n"
        "│ category                    │    │ title, description           │\n"
        "│ verified_at                 │    │ priority, urgency_note       │\n"
        "│   └─▶ INSTR_PAYLOAD (1:N)   │    │ verified_at, verified_by     │\n"
        "│      sequence, content      │    └──────────────────────────────┘\n"
        "└─────────────────────────────┘\n"
        "\n"
        "┌─────────────────────────────┐    ┌─────────────────────────────┐\n"
        "│ P360_AI_RECOMMENDED_ACTIONS │    │  CARE_COORDINATION_NOTES    │\n"
        "│─────────────────────────────│    │─────────────────────────────│\n"
        "│ patient_id                  │    │ patient_id                  │\n"
        "│ status, priority            │    │ coordinator_email/name/role │\n"
        "│ action, description         │    │ care_notes, status          │\n"
        "│ ai_rationale, due_date      │◀───│ recommendation_note_id (FK) │\n"
        "└─────────────────────────────┘    │ is_active                   │\n"
        "                                   └─────────────────────────────┘"))

    e.append(PageBreak())

    e.append(Paragraph("Relationship Summary", style_h2))
    rel_rows = [
        ["Patient",            "Practitioner",          "ManyToOne",   "primary_practitioner_id"],
        ["Patient",            "Organization",          "ManyToOne",   "managing_organization_id"],
        ["Patient",            "PatientName",           "OneToMany",   "patient_id (cascade ALL)"],
        ["Patient",            "PatientAddress",        "OneToMany",   "patient_id (cascade ALL)"],
        ["Patient",            "PatientTelecom",        "OneToMany",   "patient_id (cascade ALL)"],
        ["Patient",            "PatientIdentifier",     "OneToMany",   "patient_id (cascade ALL)"],
        ["Practitioner",       "Organization",          "ManyToOne",   "organization_id"],
        ["Observation",        "Patient / Encounter / Practitioner", "ManyToOne", "patient_id / encounter_id / performer_id"],
        ["Observation",        "ObsCodeMaster",         "ManyToOne",   "observation_code_id → item_id"],
        ["Condition",          "CondCodeMaster",        "ManyToOne",   "condition_code_id"],
        ["Encounter",          "Patient / Practitioner","ManyToOne",   "patient_id / practitioner_id"],
        ["MedicationRequest",  "MedCodeMaster",         "ManyToOne",   "medication_code_id"],
        ["Procedure",          "ProcCodeMaster",        "ManyToOne",   "procedure_code_id"],
        ["DiagnosticReport",   "Observation",           "ManyToMany",  "diagnostic_report_observation"],
        ["FamilyMemberHistory","FMH_Condition",         "OneToMany",   "family_member_history_id"],
        ["EpisodeOfCare",      "EOC_Diagnosis",         "OneToMany",   "episode_id"],
        ["EpisodeOfCare",      "EOC_Encounter",         "ManyToMany",  "composite PK (episode, encounter)"],
        ["EpisodeOfCare",      "EOC_StatusHist",        "OneToMany",   "episode_id"],
        ["AI_Rec_Instr",       "AI_Instr_Payload",      "OneToMany",   "recommendation_id"],
        ["CareCoordNote",      "P360_AI_Actions",       "ManyToOne",   "recommendation_note_id"],
        ["P360RiskScore",      "Patient / Practitioner / Organization", "ManyToOne", "patient_id / practitioner_id / organization_id"],
        ["Vitals",             "ObsCodeMaster",         "ManyToOne",   "observation_code_id → item_id"],
        ["UserAccount",        "Patient",               "FK (UUID)",   "patient_ref_id"],
        ["UserAccount",        "Practitioner",          "FK (UUID)",   "practitioner_ref_id"],
    ]
    e.append(data_table(
        ["From Entity", "To Entity", "Type", "Join Column / FK"],
        rel_rows,
        col_widths=[40 * mm, 50 * mm, 22 * mm, None],
    ))

    e.append(PageBreak())
    return e


def build_section_9_schema():
    e = []
    e.append(section_banner("9", "Database Schema — All Tables"))
    e.append(Spacer(1, 6))
    e.append(p(
        "Database: <b>SQL Server</b> (EHRAssistData), <i>dbo</i> schema. "
        "<code>BaseEntity</code> provides <code>id</code> (UUID PK), "
        "<code>version</code>, <code>created_at</code>, <code>updated_at</code>."))

    rows = [
        ["patient", "BaseEntity", "active, gender, birth_date, deceased_flag/date, marital_status, language"],
        ["patient_name", "Standalone", "patient_id (FK), family, given, prefix, suffix, use"],
        ["patient_address", "Standalone", "patient_id (FK), use, type, line, city, state, postal_code"],
        ["patient_telecom", "Standalone", "patient_id (FK), system, value, use, rank"],
        ["patient_identifier", "Standalone", "patient_id (FK), system, value, type_code"],
        ["practitioner", "BaseEntity", "family_name, given_name, gender, npi (UNIQUE), specialty_code"],
        ["organization", "BaseEntity", "name, type_code/display, phone, address_city/state, active"],
        ["observation", "BaseEntity", "patient_id, encounter_id, performer_id, observation_code_id (FK)"],
        ["condition", "BaseEntity", "patient_id, encounter_id, recorder_id, condition_code_id (FK)"],
        ["encounter", "BaseEntity", "patient_id, practitioner_id, status, encounter_class, period"],
        ["medication_request", "BaseEntity", "patient_id, encounter_id, requester_id, medication_code_id"],
        ["procedure", "BaseEntity", "patient_id, encounter_id, performer_id, procedure_code_id"],
        ["allergy_intolerance", "BaseEntity", "patient_id, clinical_status, verification, type, code"],
        ["appointment", "BaseEntity", "patient_id, practitioner_id, status, service_type, start/end"],
        ["diagnostic_report", "BaseEntity", "patient_id, encounter_id, performer_id, status, conclusion"],
        ["document_reference", "BaseEntity", "patient_id, status, type_system/code/display, content_type"],
        ["family_member_history", "BaseEntity", "patient_id, status, name, relationship code/display"],
        ["family_member_history_condition", "Standalone", "family_member_history_id (FK), code_system/value/display"],
        ["immunization", "BaseEntity", "patient_id, vaccine code/display, status, occurrence date"],
        ["service_request", "BaseEntity", "patient_id, encounter_id, requester_id, status, code"],
        ["episode_of_care", "BaseEntity", "patient_id, managing_organization_id, care_manager_id, status"],
        ["episode_of_care_diagnosis", "Standalone", "episode_id, condition_id, role, rank"],
        ["episode_of_care_encounter", "Composite PK", "episode_id (FK), encounter_id (FK)"],
        ["episode_of_care_status_history", "Standalone", "episode_id, status, period_start/end"],
        ["observation_code_master", "Identity PK", "item_id UNIQUE, code_system, loinc_code, category, ranges"],
        ["condition_code_master", "Identity PK", "code_system, code_value, icd10_code, display, category"],
        ["medication_code_master", "Identity PK", "code_system, formulary_drug_cd, display, form_code"],
        ["procedure_code_master", "Identity PK", "category, section_header/display, min_code, max_code"],
        ["ai_recommendation_instructions", "Standalone", "patient_id, practitioner_id, category, verified_at + payload"],
        ["ai_recommendation_instructions_payload", "Standalone", "recommendation_id (FK), sequence, content_string"],
        ["ai_recommended_action", "Standalone", "patient_id, practitioner_id, title, description, priority"],
        ["patient_360_ai_recommended_actions", "Standalone", "patient_id, status, priority, action, ai_rationale"],
        ["care_coordination_notes", "Standalone", "patient_id, coordinator email/name/role, care_notes, status"],
        ["care_manager_organization_mapper", "Standalone", "care_manager_id (FK), organization_id (FK)"],
        ["care_goals", "Standalone", "patient_id, status, kind, display, description, scheduled_start"],
        ["lifestyle_goal", "Standalone", "patient_id, steps, water_intake_glasses, exercise_minutes"],
        ["mark_reviewed", "Standalone", "patient_id, is_reviewed, created_date"],
        ["p360_risk_score", "Standalone", "patient_id, practitioner_id, care_manager_id, organization_id"],
        ["user_account", "Standalone", "email UNIQUE, password_hash, role, is_active, failed_logins, ref_ids"],
        ["fhir_audit_logs", "Standalone", "recorded, action, outcome, http_method, request_path, response_status"],
        ["vitals", "Standalone", "patient_id, encounter_id, observation_code_id (FK), value, unit"],
        ["risk_insights_cashing", "Standalone", "patient_id, observation/condition dates, report_html"],
    ]
    e.append(data_table(
        ["Table Name", "Type", "Key Columns"],
        rows,
        col_widths=[58 * mm, 25 * mm, None],
    ))
    e.append(PageBreak())
    return e


def build_section_10_api():
    e = []
    e.append(section_banner("10", "API Registry — All Endpoints"))
    e.append(Spacer(1, 6))

    e.append(Paragraph("User Management — /api/v1/users", style_h3))
    e.append(data_table(
        ["Method", "Path", "Roles", "Description"],
        [
            ["POST",   "/login",     "Public", "Login → returns JWT + refId"],
            ["POST",   "/",          "ADMIN",  "Create user account"],
            ["GET",    "/",          "ADMIN",  "List non-admin users"],
            ["GET",    "/{id}",      "ADMIN",  "Get user by ID"],
            ["PUT",    "/{id}",      "ADMIN",  "Update user"],
            ["DELETE", "/{id}",      "ADMIN",  "Deactivate user (soft delete)"],
        ],
        col_widths=[16 * mm, 30 * mm, 22 * mm, None],
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Patient — /baseR4/Patient", style_h3))
    e.append(data_table(
        ["Method", "Path", "Roles", "Params / Description"],
        [
            ["GET",  "/find",                                 "ALL",         "id (single patient lookup)"],
            ["GET",  "/",                                     "ADM,CM,PROV", "_id, family, given, gender, birthdate, email"],
            ["POST", "/",                                     "—",           "FHIR JSON body"],
            ["PUT",  "/{id}",                                 "—",           "FHIR JSON body"],
            ["DELETE","/{id}",                                "—",           "Delete patient"],
            ["GET",  "/ai-recommendation-instructions",       "ALL",         "patientId (paginated)"],
            ["GET",  "/ai-recommended-actions",               "ALL",         "patientId (paginated)"],
            ["GET",  "/{patientId}/clinical-data",            "ALL",         "Aggregated clinical data (paginated)"],
            ["GET",  "/{patientId}/prediction-data",          "ADM,CM,PROV", "Structured data for ML API"],
        ],
        col_widths=[16 * mm, 60 * mm, 22 * mm, None],
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Practitioner — /baseR4/Practitioner", style_h3))
    e.append(data_table(
        ["Method", "Path", "Roles", "Params / Description"],
        [
            ["GET",   "/{id}",                              "—",           "Get by ID"],
            ["GET",   "/",                                  "—",           "_id, name, specialty"],
            ["GET",   "/dropdown",                          "—",           "Dropdown list"],
            ["GET",   "/fetch-patients-by-practitioner",    "ADM,PROV",    "id (practitioner ID)"],
            ["POST",  "/",                                  "—",           "FHIR JSON body"],
            ["PUT",   "/{id}",                              "—",           "FHIR JSON body"],
            ["DELETE","/{id}",                              "—",           "Delete practitioner"],
            ["POST",  "/ai-recommendation-instructions",    "ADM,CM,PROV", "Create AI instruction"],
            ["POST",  "/ai-recommended-action",             "ADM,CM,PROV", "Create AI action"],
        ],
        col_widths=[16 * mm, 60 * mm, 22 * mm, None],
    ))

    e.append(PageBreak())

    e.append(Paragraph("Organization — /baseR4/Organization", style_h3))
    e.append(data_table(
        ["Method", "Path", "Roles", "Params / Description"],
        [
            ["GET", "/by-care-manager", "ADM,CM", "_id (paginated) — orgs for care manager"],
            ["GET", "/patients",        "ADM,CM", "orgId (paginated) — patients in organization"],
        ],
        col_widths=[16 * mm, 45 * mm, 22 * mm, None],
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Observation — /baseR4/Observation", style_h3))
    e.append(data_table(
        ["Method", "Path", "Roles", "Params / Description"],
        [
            ["GET",   "/{id}",          "ALL",           "Get by ID"],
            ["GET",   "/search",        "ALL",           "_id, patient, code, category, value-quantity, date"],
            ["GET",   "/vitals/search", "ALL",           "patient, code (vital signs)"],
            ["GET",   "/$risk-feed",    "ADM,CM,PROV",   "practitionerId, asOfDate (6-month CTE)"],
            ["POST",  "/",              "—",             "FHIR JSON body"],
            ["PUT",   "/{id}",          "—",             "FHIR JSON body"],
            ["DELETE","/{id}",          "—",             "Delete observation"],
        ],
        col_widths=[16 * mm, 40 * mm, 22 * mm, None],
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Standard FHIR CRUD Resources", style_h3))
    e.append(p(
        "All following resources support: <code>GET /{id}</code> (ALL roles), "
        "<code>GET</code> search (ALL roles), <code>POST</code>, <code>PUT</code>, "
        "<code>DELETE</code>. Common search filters on all: <code>_id</code>, "
        "<code>patient</code>."))
    e.append(data_table(
        ["Resource", "Base Path", "Extra Search Params"],
        [
            ["Condition",            "/baseR4/Condition",            "code"],
            ["Encounter",            "/baseR4/Encounter",            "status, class, date"],
            ["MedicationRequest",    "/baseR4/MedicationRequest",    "status, formulary-drug-cd"],
            ["Procedure",            "/baseR4/Procedure",            "code (CPT integer)"],
            ["AllergyIntolerance",   "/baseR4/AllergyIntolerance",   "—"],
            ["Appointment",          "/baseR4/Appointment",          "status"],
            ["DiagnosticReport",     "/baseR4/DiagnosticReport",     "—"],
            ["DocumentReference",    "/baseR4/DocumentReference",    "type.coding"],
            ["FamilyMemberHistory",  "/baseR4/FamilyMemberHistory",  "relationship"],
            ["Immunization",         "/baseR4/Immunization",         "—"],
            ["ServiceRequest",       "/baseR4/ServiceRequest",       "—"],
            ["EpisodeOfCare (R/O)",  "/baseR4/EpisodeOfCare",        "_id, patient, status, type"],
        ],
        col_widths=[45 * mm, 55 * mm, None],
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Other Endpoints", style_h3))
    e.append(data_table(
        ["Method", "Path", "Roles", "Description"],
        [
            ["GET",   "/baseR4/Measure/$care-gaps",                "ADM,CM,PROV", "Care gap evaluation (MeasureReport)"],
            ["GET",   "/baseR4/care-plan/tasks",                   "ALL",         "Care goals by patientId"],
            ["GET",   "/baseR4/care-plan/lifestyle-goals",         "ALL",         "Daily / weekly lifestyle goals"],
            ["GET",   "/baseR4/CareCoordinationNote/search",       "ADM,CM,PROV", "Search coordination notes"],
            ["POST",  "/baseR4/CareCoordinationNote",              "—",           "Create coordination note"],
            ["PATCH", "/baseR4/CareCoordinationNote",              "—",           "Deactivate notes"],
            ["POST",  "/baseR4/portal/create-recommendations",     "—",           "Bulk AI recommended actions"],
            ["GET",   "/baseR4/portal/task-queue",                 "ADM,CM,PROV", "Task queue for P360"],
            ["PATCH", "/baseR4/portal/update-task",                "—",           "Update task status"],
            ["POST",  "/baseR4/portal/create-review",              "—",           "Mark patient reviewed"],
            ["GET",   "/baseR4/portal/get-review",                 "ADM,CM,PROV", "Get review status"],
            ["GET",   "/api/v1/predict/risk-insights",             "ADM,CM,PROV", "Risk prediction HTML report"],
            ["GET",   "/baseR4/metadata",                          "Public",      "FHIR CapabilityStatement"],
            ["POST",  "/baseR4/out-reach/email-portal",            "—",           "Send outreach email"],
        ],
        col_widths=[16 * mm, 70 * mm, 22 * mm, None],
    ))
    e.append(PageBreak())
    return e


def build_section_11_services():
    e = []
    e.append(section_banner("11", "Service Layer Details"))
    e.append(Spacer(1, 6))
    e.append(p(
        "All services follow the <b>Interface + Implementation</b> pattern. "
        "Implementations are annotated with <code>@Transactional</code> "
        "(<code>readOnly=true</code> on read operations)."))

    e.append(Spacer(1, 6))
    rows = [
        ["UserService",                       "Login, CRUD user accounts, password hashing, JWT generation", "184"],
        ["PatientService",                    "FHIR Patient CRUD, search with specs, clinical data aggregation", "210"],
        ["PractitionerService",               "FHIR Practitioner CRUD, dropdown, patient-by-practitioner", "221"],
        ["OrganizationService",               "Organizations by care manager, patients by organization", "~100"],
        ["CareManagerService",                "Care coordination notes CRUD, deactivation", "~120"],
        ["ObservationService",                "FHIR Observation CRUD, vitals search, $risk-feed (CTE)", "487"],
        ["ConditionService",                  "FHIR Condition CRUD, code master lookup", "186"],
        ["EncounterService",                  "FHIR Encounter CRUD, date search, org encounter count", "366"],
        ["MedicationRequestService",          "FHIR MedicationRequest CRUD, formulary search", "202"],
        ["ProcedureService",                  "FHIR Procedure CRUD, CPT code search", "183"],
        ["AllergyIntoleranceService",         "FHIR AllergyIntolerance CRUD", "~120"],
        ["AppointmentService",                "FHIR Appointment CRUD, status filter", "~120"],
        ["DiagnosticReportService",           "FHIR DiagnosticReport CRUD with M2M observations", "~140"],
        ["DocumentReferenceService",          "FHIR DocumentReference CRUD", "~120"],
        ["FamilyMemberHistoryService",        "FHIR FamilyMemberHistory CRUD with child conditions", "~150"],
        ["ImmunizationService",               "FHIR Immunization CRUD", "~120"],
        ["ServiceRequestService",             "FHIR ServiceRequest CRUD", "~110"],
        ["EpisodeOfCareService",              "Read-only Episode of Care with JOIN FETCH", "~100"],
        ["CareGapService",                    "Diabetes / Hypertension / CKD / Kidney Cancer gap evaluation", "842"],
        ["DataForPredictionService",          "Structured data map for external ML API", "447"],
        ["LifestyleGoalService",              "Daily goals + weekly rollup via native SUM", "~100"],
        ["MyCarePlanTaskService",             "Care goals as FHIR CarePlan tasks", "~80"],
        ["AiRecommendationInstructionsService","Create / retrieve AI instructions (parent + child)", "~100"],
        ["AiRecommendedActionService",        "Create / retrieve AI recommended actions", "~80"],
        ["AiActionsService",                  "P360 portal: bulk create, task queue, update status", "~150"],
        ["RiskInsightsService",               "Cache-aware risk prediction via external API", "~80"],
        ["MarkReviewedService",               "Mark patient as reviewed", "~50"],
        ["PatientOutReachService",            "Send outreach email via Gmail SMTP", "~60"],
    ]
    e.append(data_table(
        ["Service", "Responsibility", "Size (lines)"],
        rows,
        col_widths=[60 * mm, None, 22 * mm],
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Notable Service Patterns", style_h3))
    e.append(bullets([
        "<b>CareGapService (842 lines)</b> — evaluates Diabetes, Hypertension, CKD, and Kidney Cancer gaps using clinical rules; returns FHIR <i>MeasureReport</i>.",
        "<b>DataForPredictionService (447 lines)</b> — builds <code>Map&lt;String, Object&gt;</code> for consumption by external ML prediction APIs.",
        "<b>ObservationService.$risk-feed</b> — native CTE query with a 6-month date window for the practitioner risk dashboard.",
        "All clinical services use <b>Mapper</b> classes to convert JPA entities to FHIR R4 resources and back.",
        "<code>BundleBuilder.searchSetWithPagination()</code> creates FHIR <i>Bundle</i> (type: <i>searchset</i>) with proper pagination links.",
    ]))
    e.append(PageBreak())
    return e


def build_section_12_conv():
    e = []
    e.append(section_banner("12", "Coding Conventions"))
    e.append(Spacer(1, 6))

    e.append(Paragraph("Mandatory Rules", style_h3))
    e.append(bullets([
        "<code>ObjectUtils.isEmpty()</code> / <code>CollectionUtils.isEmpty()</code> for all null/empty checks — never raw <code>!= null</code>.",
        "Java 8+ streams for collection transforms — never for-loops. <code>AtomicInteger</code> for sequencing.",
        "<code>Page&lt;T&gt;</code> + <code>Pageable</code> on ALL list APIs. Default page size: 10.",
        "Group endpoints in existing controllers — no extra controller classes.",
        "<code>@PreAuthorize</code> on all endpoints with correct roles.",
        "Interface + Impl for services. <code>@Transactional</code> on class, <code>readOnly=true</code> on reads.",
        "Discuss approach before implementing new features.",
    ]))

    e.append(Paragraph("Entity Conventions", style_h3))
    e.append(bullets([
        "Clinical entities extend <b>BaseEntity</b> (@MappedSuperclass) with <code>@SuperBuilder</code>, <code>@EqualsAndHashCode(callSuper=true)</code>.",
        "Lightweight entities: standalone <code>@Id</code> <code>@GeneratedValue(UUID)</code> with <code>@Builder</code>.",
        "<code>@ManyToOne(LAZY)</code> + <code>@ToString.Exclude</code> + <code>@EqualsAndHashCode.Exclude</code> for all associations.",
        "<code>@OneToMany(mappedBy, cascade=ALL, orphanRemoval=true)</code> for child collections.",
    ]))

    e.append(Paragraph("Repository Conventions", style_h3))
    e.append(bullets([
        "<code>JpaRepository&lt;Entity, UUID&gt;</code> + <code>JpaSpecificationExecutor</code> where needed.",
        "Derived methods for simple queries, <code>@Query</code> for complex joins.",
        "Interface projections for native read-only queries.",
        "Always <code>countQuery</code> for native paginated queries.",
    ]))

    e.append(Paragraph("FHIR Response Conventions", style_h3))
    e.append(bullets([
        "Single resource: <code>FhirResponseHelper.toResponse(Resource)</code>",
        "Lists: <code>BundleBuilder.searchSetWithPagination()</code> then <code>FhirResponseHelper</code>",
        "Custom fields: FHIR <b>Extension</b>",
        "Errors: <b>OperationOutcome</b> (404 / 400 / 403 / 401 / 500)",
    ]))

    e.append(Paragraph("Naming Conventions", style_h3))
    e.append(bullets([
        "Entity: <code>{Resource}Entity</code> | Repository: <code>{Resource}Repository</code>",
        "Service: <code>{Resource}Service</code> / <code>{Resource}ServiceImpl</code>",
        "Mapper: <code>{Resource}Mapper</code>",
        "Request DTO: <code>{Action}{Resource}Request</code> | Response DTO: <code>{Resource}Response</code>",
        "Projection: <code>{Purpose}Projection</code>",
    ]))

    e.append(PageBreak())
    return e


def build_section_13_deploy():
    e = []
    e.append(section_banner("13", "Deployment Topology", color=WARNING))
    e.append(Spacer(1, 6))

    e.append(p(
        "EHRAssist V2 is deployed as two cooperating tiers: a cloud-hosted frontend "
        "on Vercel, and a self-hosted backend running on Apache Tomcat inside the "
        "R Systems network."))

    e.append(Spacer(1, 4))
    e.append(code_block(
        "                 Internet\n"
        "                    │\n"
        "                    ▼\n"
        "        ┌──────────────────────────┐\n"
        "        │     Vercel (CDN)         │   ←  React 19 + Vite static bundle\n"
        "        │   patient360-*.vercel    │       + Edge Function /api/chat\n"
        "        └──────────┬───────────────┘\n"
        "                   │ HTTPS  (Bearer JWT)\n"
        "                   ▼\n"
        "        ┌──────────────────────────────┐\n"
        "        │   R Systems Office VPN       │\n"
        "        │   (developer access only)    │\n"
        "        └──────────┬───────────────────┘\n"
        "                   │ RDP (10.131.58.59)\n"
        "                   ▼\n"
        "        ┌──────────────────────────────┐\n"
        "        │  Local Machine @ R Systems   │\n"
        "        │  Apache Tomcat — port 3001   │   ←  Spring Boot WAR\n"
        "        │  HTTPS (PKCS12 keystore)     │\n"
        "        └──────────┬───────────────────┘\n"
        "                   │ JDBC\n"
        "                   ▼\n"
        "        ┌──────────────────────────────┐\n"
        "        │  SQL Server  10.131.58.62    │\n"
        "        │  Database: EHRAssistData     │\n"
        "        └──────────────────────────────┘"))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Frontend Deployment — Vercel", style_h2))
    e.append(bullets([
        "Hosted on <b>Vercel</b> — static assets served from the global edge CDN.",
        "Builds run with <code>vite build</code>; output is published from the GitHub repository <code>rishabh-r/patient360</code>.",
        "A single <b>Vercel Edge Function</b> at <code>/api/chat</code> proxies all Azure OpenAI traffic.",
        "Environment variables (e.g. <code>VITE_DECRYPT_KEY</code>, AI keys) are configured in the Vercel project settings — never committed to git.",
    ]))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Backend Deployment — Tomcat at R Systems", style_h2))
    e.append(p(
        "For deployment of the Tomcat server we are currently using a <b>local machine "
        "kept inside the R Systems premises</b>, running on <b>port 3001</b> at "
        "<b>IP 10.131.58.59</b>. Developers connect to this machine via the "
        "<b>R Systems VPN</b> and then a <b>Remote Desktop Connection</b> (RDP) to "
        "manage builds, restarts, and log inspection. Once VPN is connected, the API "
        "is reachable from inside the corporate network at "
        "<code>https://10.131.58.59:3001/baseR4/…</code>"))

    e.append(Spacer(1, 4))
    e.append(data_table(
        ["Attribute", "Value"],
        [
            ["Server",          "Apache Tomcat (Spring Boot embedded / WAR deployment)"],
            ["Host Machine",    "Local desktop kept in R Systems office"],
            ["IP Address",      "10.131.58.59"],
            ["Port",            "3001 (HTTPS)"],
            ["Access Method",   "R Systems VPN → Windows Remote Desktop Connection"],
            ["TLS",             "PKCS12 keystore (<code>keystore.p12</code>) configured in application.properties"],
            ["Restart Workflow","RDP into machine → stop Tomcat → drop new WAR → start Tomcat → verify <code>/baseR4/metadata</code>"],
        ],
        col_widths=[42 * mm, None], header_color=WARNING,
    ))

    e.append(Spacer(1, 8))
    e.append(Paragraph("Database &amp; Network", style_h2))
    e.append(bullets([
        "<b>SQL Server</b> instance: <code>10.131.58.62:1433</code>, database <code>EHRAssistData</code> (encrypted JDBC connection with <code>trustServerCertificate=true</code>).",
        "<b>CORS</b> allow-list in <code>application.properties</code> includes all production Vercel deployments plus localhost dev origins.",
        "<b>Gmail SMTP</b> is used for outreach email — credentials are kept in application properties on the host (rotate before sharing builds).",
    ]))

    e.append(Spacer(1, 6))
    e.append(callout(
        "Operational Note",
        "Because the backend lives on a single in-office desktop, restarting the host or losing the VPN tunnel takes the API offline. A migration plan to a managed VM / container host is recommended before broader rollout.",
        color=DANGER, icon="!",
    ))

    e.append(PageBreak())
    return e


def build_section_14_features():
    e = []
    e.append(section_banner("14", "Feature Log"))
    e.append(Spacer(1, 6))

    e.append(Paragraph("Core Features", style_h3))
    e.append(bullets([
        "FHIR CRUD for 16 resources: Patient, Practitioner, Observation, Condition, Encounter, MedicationRequest, Procedure, AllergyIntolerance, Appointment, DiagnosticReport, DocumentReference, FamilyMemberHistory, Immunization, ServiceRequest, EpisodeOfCare (read-only), Organization.",
        "JWT authentication with 4 roles (ADMIN, CARE_MANAGER, PROVIDER, PATIENT).",
        "<code>@PreAuthorize</code> on 54+ endpoints.",
        "<b>GlobalExceptionHandler</b> returning FHIR <i>OperationOutcome</i>.",
    ]))

    e.append(Paragraph("AI Features", style_h3))
    e.append(bullets([
        "<b>Instructions</b>: parent/child entities (<code>ai_recommendation_instructions</code> + payload). Create via PractitionerController, Get via PatientController. <code>verified_at</code> gates visibility. Mapped to FHIR <i>Communication</i>.",
        "<b>Actions</b>: flat entity (<code>ai_recommended_action</code>). Same create/get pattern. Extensions for title/priority/urgency. FHIR <i>Communication</i>.",
        "<b>Portal AI 360</b>: <code>patient_360_ai_recommended_actions</code> table, task queue, care coordination notes, mark-reviewed workflow.",
        "<b>Azure OpenAI (gpt-4.1-mini)</b> drives summaries, daily tasks, follow-up instructions, recommended actions, semantic dedup, and high-risk flagging — all routed via the Vercel Edge Function <code>/api/chat</code>.",
    ]))

    e.append(Paragraph("Clinical Features", style_h3))
    e.append(bullets([
        "<b>Risk Feed</b>: <code>$risk-feed</code> with 6-month date window using a native CTE query.",
        "<b>Care Gaps</b>: Diabetes / Hypertension / CKD evaluation returning FHIR <i>MeasureReport</i>.",
        "<b>Vitals</b>: separate vitals table + search API.",
        "<b>Clinical Data Aggregation</b>: <code>/Patient/{id}/clinical-data</code> endpoint.",
        "<b>Prediction Data</b>: structured map for external ML API.",
        "<b>Lifestyle Goals</b>: daily steps / water / exercise with weekly rollups as FHIR <i>CarePlan</i>.",
    ]))

    e.append(Paragraph("Care Management Features", style_h3))
    e.append(bullets([
        "<code>care_manager_organization_mapper</code> (many-to-many) with native JOIN + projection.",
        "Organization patients API with total count.",
        "Care coordination notes with recommendation linkage.",
        "P360 Risk Score tracking across patient / practitioner / organization.",
    ]))

    e.append(Paragraph("Fixes Applied", style_h3))
    e.append(bullets([
        "Observation N+1 (JoinColumn <code>item_id</code> fix).",
        "Lifestyle Goal column name (<code>scheduled_start</code>).",
        "Patient GET changed to <code>/find?id=</code> (request param).",
        "Exception handler 500 changed to proper 403 / 401.",
        "SQL Server filtered unique indexes for NULL handling.",
    ]))

    e.append(Spacer(1, 4))
    e.append(HRFlowable(width="40%", color=ACCENT, thickness=1, hAlign="CENTER"))
    e.append(Paragraph(
        "<i>End of EHRAssist V2 Full Stack Documentation.</i>",
        ParagraphStyle("end", fontName="UIBody-I", fontSize=9,
                       textColor=TEXT_MUTED, alignment=TA_CENTER,
                       spaceBefore=3)))
    return e


# =============================================================================
# DOCUMENT BUILD
# =============================================================================
def build_document(output_path):
    doc = BaseDocTemplate(
        output_path,
        pagesize=A4,
        leftMargin=MARGIN_L, rightMargin=MARGIN_R,
        topMargin=MARGIN_T, bottomMargin=MARGIN_B,
        title="EHRAssist V2 — Full Stack Documentation",
        author="R Systems",
        subject="Full Stack project documentation for EHRAssist V2",
    )

    cover_frame = Frame(0, 0, PAGE_W, PAGE_H, id="cover",
                        leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    cover_tpl = PageTemplate(id="Cover", frames=[cover_frame],
                             onPage=draw_cover_background)

    content_frame = Frame(
        MARGIN_L, MARGIN_B,
        PAGE_W - MARGIN_L - MARGIN_R,
        PAGE_H - MARGIN_T - MARGIN_B,
        id="content",
    )
    content_tpl = PageTemplate(id="Content", frames=[content_frame],
                               onPage=draw_content_chrome)

    doc.addPageTemplates([cover_tpl, content_tpl])

    story = []
    story.extend(build_cover())
    story.append({"_": "next_template"})  # placeholder, replaced below

    # Build the story for content pages
    content_story = []
    content_story.extend(build_toc())
    content_story.extend(build_section_1())
    content_story.extend(build_section_2())
    content_story.extend(build_section_3_frontend())
    content_story.extend(build_section_4_ai())
    content_story.extend(build_section_5_auth())
    content_story.extend(build_section_6_standards())
    content_story.extend(build_section_7_packages())
    content_story.extend(build_section_8_er())
    content_story.extend(build_section_9_schema())
    content_story.extend(build_section_10_api())
    content_story.extend(build_section_11_services())
    content_story.extend(build_section_12_conv())
    content_story.extend(build_section_13_deploy())
    content_story.extend(build_section_14_features())

    # Replace placeholder with NextPageTemplate flowable
    from reportlab.platypus import NextPageTemplate
    story = (
        build_cover()[:-1]                  # cover minus its PageBreak
        + [NextPageTemplate("Content"), PageBreak()]
        + content_story
    )

    doc.build(story)


if __name__ == "__main__":
    out = r"C:\Users\Harshit\Downloads\EHRAssist_FullStack_Documentation.pdf"
    build_document(out)
    print(f"Wrote: {out}")
