---
# Allowed version bumps: patch, minor, major
macros: minor
---

Changed the translated label, author name and page link macros to display their value as plain text.

A translation inserted with `##resourceBundle(...)##` that contains HTML markup now shows that markup as text. To keep formatting, place the markup in the page content around the macro rather than in the translation.
