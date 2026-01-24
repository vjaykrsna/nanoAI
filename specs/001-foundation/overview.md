# nanoAI — UI/UX Blueprint

**Last Updated:** 2026-01-25  
**Status:** Implemented in `feature/uiux` and `shared/ui`  
**Scope:** App shell, navigation, and core UX patterns.

---

## Design Priorities

- **Usable:** fast, predictable flows.
- **Consistent:** shared layouts and components.
- **Offline-first:** graceful when disconnected.
- **Trustworthy:** clear messaging, no clutter.

---

## App Shell

- **Home-first:** launch into a hub with quick access to key modes.
- **Left sidebar:** Home, Chat, Library, Settings, and related entries.
- **Right sidebar:** contextual panels (model selector, details, progress) when needed.
- **Command palette:** keyboard-first access to navigation and actions.

---

## Global Feedback

- Connectivity banner for online/offline.
- Lightweight toasts for background events.
- Progress center for downloads and long-running jobs.
- Undo or confirm for impactful actions.

---

## Core Screens (Implemented)

- **Home:** mode cards and recent activity.
- **Chat:** threaded messages, composer, model/persona controls.
- **Model Library:** installed/available models with metadata and controls.
- **Settings:** grouped sections (General, Appearance, Privacy, Models, Backup, Advanced).

---

## Components & Patterns

- **Primitives:** buttons, inputs, cards, lists, toasts, progress.
- **Composed:** composer bar, chat bubbles, model cards, sidebar sections.
- **Rules:**

  - Accessible by default.
  - Use design tokens from shared theme.
  - Support compact/comfortable density.

---

## Flows

- Action → feedback → retry.
- Online → offline → sync.
- Create → edit → review.

---

Tone: concise, direct, and transparent. The implemented UI in `feature/uiux` and `shared/ui` should reflect these principles without overcomplication.
