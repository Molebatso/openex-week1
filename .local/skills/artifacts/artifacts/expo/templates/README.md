# Expo scaffold templates

`bootstrap.js expo` copies `../files/`. `bootstrap.js expo --template=expo-sdk57`
layers `sdk57/files/` **over** that base rather than replacing it, so a template
holds only what it changes.

Layering is keyed by the path a file produces in the generated artifact, not by
its name in the layer, so `sdk57/files/app.json` would override a base
`app.json.template`. Keep the `.template` suffix on any file that needs
`__REPLIT_ARTIFACT_*` token interpolation — the suffix is a property of the file,
not of the layer.

## Removing a base file

Overlay semantics have no way to express "the base ships this, I must not."
Deleting a file from a template directory means "inherit the base copy", which is
the point of the overlay — so removal needs its own statement. State it in
`<template>/template.json`:

```json
{
  "remove": ["babel.config.js"]
}
```

Paths are generated-artifact paths (post-`.template`), `/`-separated, relative to
the artifact root. Files only; to drop a directory, list its files. A template
that only removes needs no `files/` directory — git cannot carry an empty one.

This is an explicit manifest rather than a `foo.remove` sentinel file on purpose:

- A sentinel is ambiguous against the `.template` suffix (`app.json.remove` or
  `app.json.template.remove`?) and can collide with a file legitimately named
  `*.remove`.
- One manifest is the whole base→template delta a reviewer has to read, next to
  the directory it applies to.
- Removal is rare and load-bearing. Making it a named entry keeps it from
  happening by accident, and keeps the accident that overlays invite — deleting a
  file and expecting it to vanish from the output — from being silent.

`bootstrap.js` fails rather than continuing when the manifest and the base
disagree: an unknown key, a `remove` entry the base does not produce, or a path
that is both removed and provided by the template. A stale entry is a scaffold
bug, and the alternative is an artifact that quietly regains a file someone
deliberately dropped.

## Keeping templates thin

A template file byte-identical to the file it overrides is dead weight — it is
inherited content restated, and it silently stops tracking the base. The
`expo template layering` tests in
`pkg/agent-skills/src/__tests__/artifactBootstrap.test.ts` fail on one, so add
files here only when they actually differ.
