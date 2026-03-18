# Agent Guidelines for frontend

## Folder Overview
This folder contains the Next.js application for the Assine user interface. It is currently ahead of the backend integration in some areas, so treat API assumptions as provisional unless they match implemented backend controllers.

## Files
- `src/app/`: App Router pages and route-level UI
- `src/components/`: shared UI components
- `src/lib/api/`: frontend API client wrappers
- `src/hooks/`: React hooks for auth and payment workflows
- `public/`: static assets

## Constraints
- Keep environment variables aligned with the real backend entrypoints
- Do not assume gateway endpoints exist unless they are implemented in this repo
- Containerization should remain optional until API contracts are aligned
