# FinalBRD Gap Report

## Scope
- Source of truth reviewed: `docs/FinalBRD.md`
- Comparison targets: codebase (`src/`, `frontend/`), `docs/`, and `.kiro/`
- Source fallback used: user-provided FinalBRD content in chat (same structure/sections as file)

## Comparison Result
Most FinalBRD high-level requirements are already represented either:
- directly in code (implemented or in progress), or
- in planning/design docs under `docs/` and `.kiro/`.

One requirement area from FinalBRD is not represented outside FinalBRD itself.

## Must Be Implemented (from FinalBRD-only gaps)
1. **System Context Diagram artifact**
   - FinalBRD references a dedicated system context diagram section (`Section 6`) but no actual context diagram exists in:
     - codebase (`src/`, `frontend/`)
     - other files in `docs/`
     - files in `.kiro/`
   - Must implement as a documented artifact (recommended: Mermaid diagram in `docs/` and/or `.kiro` design docs) showing:
     - Financial Analyst
     - Risk Lead
     - Administrator
     - CovenantIQ System
     - Financial Statement Inputs
     - Risk Reports/Alerts Outputs

## Notes
1. Features such as JWT/RBAC, alert lifecycle, portfolio metrics, CSV export, bulk import, attachments, activity logging, and health endpoint are already covered in `docs/.kiro` even if not fully completed in runtime code yet.
