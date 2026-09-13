# tupla-kupla frontend

React + TypeScript frontend, built with Vite.

## Requirements

Node 20 or newer.

## Setup

```bash
npm install
```

## Scripts

| Command                | What it does                                    |
| ---------------------- | ----------------------------------------------- |
| `npm run dev`          | Start the dev server with HMR                   |
| `npm run build`        | Type-check (`tsc -b`) and build to `dist/`      |
| `npm run lint`         | Lint with oxlint                                |
| `npm run format`       | Format everything with Prettier                 |
| `npm run format:check` | Report unformatted files without rewriting them |
| `npm run preview`      | Serve the built output from `dist/` locally     |

## Linting and formatting

These are two separate tools doing two separate jobs:

- **oxlint** (`npm run lint`) finds likely bugs and bad patterns — a misused hook,
  an unused variable. It is configured in `.oxlintrc.json`. It does not reformat code.
- **Prettier** (`npm run format`) rewrites whitespace, quotes and line breaks to one
  consistent style. It has no opinion on whether the code is correct.

Prettier is configured in `.prettierrc.json` to match the style the project was
scaffolded with: single quotes and no semicolons. Change it there if you prefer
otherwise — it is two lines, and `npm run format` will restyle the codebase.

Prettier is pinned to an exact version rather than a `^` range, as Prettier itself
recommends: a patch release can change formatting output, which would otherwise show
up as unrelated diff noise for whoever upgrades first.

## Notes

Type-aware lint rules are not enabled. To turn them on, install `oxlint-tsgolint`
and add `"options": { "typeAware": true }` to `.oxlintrc.json`.
