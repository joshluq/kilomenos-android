# GitHub Copilot & AI Developer Instructions

You are assisting developers working in an enterprise **Specification-Driven Development (SDD)** ecosystem governed by `AGENTS.md`.

## 1. Golden Rules of Development

1. **Spec-First (No Spec, No Code)**:
   - NEVER generate, propose, or write code without an approved upstream specification (`openspec/changes/` or `templates/prd-template.md`).
   - All code changes must trace directly to explicit functional requirements (`FR-xx`) and acceptance criteria (`AC-xx`).

2. **HITL 2-Stage Gate (Human-In-The-Loop)**:
   - **Stage 1 (Specification & Technical Design)**: PO and Architect align on requirements, failure consequences, and interface contracts.
   - **HALT GATE**: If there are unresolved open questions (`Dudas Abiertas Bloqueantes`), **STOP IMMEDIATELY**. You must prompt the developer/stakeholder for clarification before proceeding.
   - **Stage 2 (Implementation & Verification)**: Only write code after Stage 1 is fully approved.

3. **Zero Ghost Code (Anti-Scope Creep)**:
   - Strictly prohibit adding unrequested endpoints, hidden parameters, undeclared UI components, or phantom behaviors.
   - Implement ONLY what is documented in the approved specification.

4. **Negative Testing for Business Rules (`BR-xx`)**:
   - Every business rule must define an explicit failure consequence (`BR-xx`).
   - Every failure branch must have automated negative test coverage (HTTP 4xx/5xx, localized error UI, fallback state).

## 2. Platform Discipline Profile: ANDROID

### Platform Guardrails: Android (Kotlin / Jetpack Compose)
- **Architecture**: Clean Architecture + MVI. Follow 4-Layer Compose (Screen -> Coordinator -> Content -> Components).
- **Presentation**: Pure Unidirectional Data Flow (UDF). State is immutable StateFlow; actions are sealed interfaces.
- **Testing**: Test StateFlow with Turbine; mock dependencies with MockK; test UI with ComposeTestRule.
- **Boundaries**: Strictly NO direct database/SQL or backend code. Respect Min SDK 24 / Target SDK 35.
- **Design Tokens**: Enforce design tokens (CanvasKit / FoundationKit). Never hardcode hex colors or raw dimensions.
- **Roles & Governance**: Refer to `AGENTS.md` (PO, Solutions Architect, Mobile Architect, Senior Android Dev, Android QA).

## 3. Workflow References

- Role Specifications & Technical Guardrails: `AGENTS.md` (Section 5)
- Living Specifications: `openspec/specs/`
- Active Changes & Proposals: `openspec/changes/`
- Handoff & Validation Schemas: `schemas/`
