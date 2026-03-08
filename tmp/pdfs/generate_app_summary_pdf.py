from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    FrameBreak,
    ListFlowable,
    ListItem,
    PageTemplate,
    Paragraph,
    Spacer,
)


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "output" / "pdf" / "covenantiq-app-summary.pdf"


def styles():
    sample = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "Title",
            parent=sample["Title"],
            fontName="Helvetica-Bold",
            fontSize=22,
            leading=24,
            textColor=colors.HexColor("#0f172a"),
            spaceAfter=7,
            alignment=TA_LEFT,
        ),
        "subtitle": ParagraphStyle(
            "Subtitle",
            parent=sample["BodyText"],
            fontName="Helvetica",
            fontSize=9.8,
            leading=12,
            textColor=colors.HexColor("#475569"),
            spaceAfter=10,
        ),
        "section": ParagraphStyle(
            "Section",
            parent=sample["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=11,
            leading=13,
            textColor=colors.HexColor("#0f172a"),
            spaceBefore=5,
            spaceAfter=4,
        ),
        "body": ParagraphStyle(
            "Body",
            parent=sample["BodyText"],
            fontName="Helvetica",
            fontSize=9.2,
            leading=11.1,
            textColor=colors.HexColor("#1f2937"),
            spaceAfter=4,
        ),
        "bullet": ParagraphStyle(
            "Bullet",
            parent=sample["BodyText"],
            fontName="Helvetica",
            fontSize=8.9,
            leading=10.6,
            textColor=colors.HexColor("#1f2937"),
            leftIndent=0,
            firstLineIndent=0,
            spaceAfter=0,
        ),
        "footer": ParagraphStyle(
            "Footer",
            parent=sample["BodyText"],
            fontName="Helvetica-Oblique",
            fontSize=7.8,
            leading=9,
            textColor=colors.HexColor("#64748b"),
            spaceBefore=6,
        ),
    }


def bullet_list(items, style):
    return ListFlowable(
        [ListItem(Paragraph(item, style), leftIndent=0) for item in items],
        bulletType="bullet",
        bulletFontName="Helvetica",
        bulletFontSize=7.5,
        bulletOffsetY=1,
        leftIndent=10,
        spaceBefore=1,
        spaceAfter=5,
    )


def section(title, style_map, bullets=None, paragraphs=None):
    flow = [Paragraph(title, style_map["section"])]
    for text in paragraphs or []:
        flow.append(Paragraph(text, style_map["body"]))
    if bullets:
        flow.append(bullet_list(bullets, style_map["bullet"]))
    return flow


def build_pdf():
    style_map = styles()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    doc = BaseDocTemplate(
        str(OUTPUT),
        pagesize=letter,
        leftMargin=0.5 * inch,
        rightMargin=0.5 * inch,
        topMargin=0.45 * inch,
        bottomMargin=0.42 * inch,
    )

    column_gap = 0.3 * inch
    frame_width = (doc.width - column_gap) / 2
    frame_height = doc.height

    left = Frame(doc.leftMargin, doc.bottomMargin, frame_width, frame_height, id="left", showBoundary=0)
    right = Frame(
        doc.leftMargin + frame_width + column_gap,
        doc.bottomMargin,
        frame_width,
        frame_height,
        id="right",
        showBoundary=0,
    )
    doc.addPageTemplates(PageTemplate(id="two-col", frames=[left, right]))

    story = [
        Paragraph("CovenantIQ", style_map["title"]),
        Paragraph(
            "One-page repo summary generated from code and docs in this workspace only.",
            style_map["subtitle"],
        ),
    ]

    story.extend(
        section(
            "What It Is",
            style_map,
            paragraphs=[
                "CovenantIQ is a commercial loan risk surveillance app with a React single-page frontend and a Spring Boot API/backend.",
                "The repo centers on monitoring borrower financial statements, evaluating covenant compliance, generating alerts, and managing related workflows, rulesets, integrations, and change control.",
            ],
        )
    )
    story.extend(
        section(
            "Who It's For",
            style_map,
            paragraphs=[
                "Primary persona: commercial credit analysts, risk leads, and platform admins. Evidence in repo: role-based access for ANALYST, RISK_LEAD, and ADMIN, plus portfolio, alert, workflow, and user-management screens.",
            ],
        )
    )
    story.extend(
        section(
            "What It Does",
            style_map,
            bullets=[
                "Tracks loans, borrower covenants, financial statements, and per-loan monitoring history.",
                "Calculates covenant results and risk summaries from submitted or bulk-imported statement data.",
                "Creates, reviews, exports, and resolves alerts with role-gated actions and status transitions.",
                "Stores comments, activity logs, statement attachments, and collateral exception records.",
                "Provides policy studio and workflow designer flows for ruleset versioning and alert workflows.",
                "Supports webhook subscriptions, signed event delivery, retries, and delivery history.",
            ],
        )
    )

    story.append(FrameBreak())

    story.extend(
        section(
            "How It Works",
            style_map,
            paragraphs=[
                "<b>Frontend:</b> Vite/React app (frontend/src/App.tsx) routes users to dashboard, loans, alerts, portfolio, reports, workflows, policies, integrations, settings, and admin screens. frontend/src/api/client.ts calls /api/v1, stores JWT sessions in localStorage, and refreshes tokens automatically.",
                "<b>Backend:</b> Spring Boot controllers under src/main/java/com/covenantiq/controller expose loan, alert, auth, user, workflow, ruleset, change-control, report/export, attachment, and integration endpoints. Services encapsulate business logic; Spring Security protects /api/v1/** with JWT auth.",
                "<b>Data flow:</b> when a statement is submitted, FinancialStatementService saves it, triggers covenant evaluation and trend analysis, logs activity, and publishes an outbox event. CovenantEvaluationService computes ratios, persists pass/breach results, and creates alerts as needed. RiskSummaryService reads latest statements/results/alerts for rollups. JPA repositories persist domain records to H2 (jdbc:h2:mem:covenantiqdb).",
                "<b>Integrations:</b> OutboxEventPublisher writes domain events; OutboxDispatcherService polls pending events, signs webhook payloads, retries failures, and stores delivery outcomes. Production queue/broker infrastructure: <b>Not found in repo.</b>",
            ],
        )
    )
    story.extend(
        section(
            "How To Run",
            style_map,
            bullets=[
                "Prereqs from docs/RUNBOOK.md: Java 21+, Maven 3.9+, Node.js 20+ with npm.",
                "Backend: run mvn clean package, then mvn spring-boot:run from the repo root.",
                "Frontend: run cd frontend, npm install, then npm run dev; open http://localhost:5173.",
                "Optional single-container path: docker compose build and docker compose up; open http://localhost:8080.",
                "Seeded demo content is enabled by default (app.seed.enabled: true); demo users and passwords are created in DataInitializer.java.",
            ],
        )
    )
    story.append(Spacer(1, 4))
    story.append(
        Paragraph(
            "Repo signals used: README, RUNBOOK, application.yml, frontend routes/API client, Spring controllers/services, Docker files, and seed-data config.",
            style_map["footer"],
        )
    )

    doc.build(story)


if __name__ == "__main__":
    build_pdf()
    print(OUTPUT)
