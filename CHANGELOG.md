# macros Changelog

## 8.5.0

### New Features

* Remove getConstant, sessionid, userprofiledata, formtoken and devmode macros to enhance security. (#54)

* Update compatible Jahia version from 8.0.0.0 to 8.1.6.0 (#56)

* Changed the translated label, author name and page link macros to display their value as plain text.

  A translation inserted with `##resourceBundle(...)##` that contains HTML markup now shows that markup as text. To keep formatting, place the markup in the page content around the macro rather than in the translation.

* Update Jahia compatibility to 8.1.6.1 and specify use of Jahia plugin version 6.8 (#59)

* Update compatible Jahia version from 8.1.6.0 to 8.1.6.2 (#57)

### Bug Fixes

* \##keywords## macro now HTML-escapes `j:keywords` values before rendering (#68)
