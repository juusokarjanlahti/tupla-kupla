# tupla-kupla frontend

React + TypeScript frontend, built with Vite.

## Requirements

Node 20 or newer.

## Setup

```bash
npm install
```

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Start the dev server with HMR |
| `npm run build` | Type-check (`tsc -b`) and build to `dist/` |
| `npm run lint` | Run oxlint |
| `npm run preview` | Serve the built output from `dist/` locally |

## Notes

Type-aware lint rules are not enabled. To turn them on, install `oxlint-tsgolint`
and add `"options": { "typeAware": true }` to `.oxlintrc.json`.
