# Known issues

Honest leftovers. Not hidden bugs.

## Cross-site request forgery (CSRF)

Session cookies (`JSESSIONID`, `HttpOnly`, `SameSite=Lax`) plus same-origin nginx. Spring CSRF is **off** in this version so the React app can `POST` JSON with `credentials: "include"` and no extra header.

That is enough against random other sites (Lax blocks most cross-site POSTs). It is **not** a substitute for CSRF tokens if we ever host the API on another origin or add cookie-authed state-changing GET.

Leftover: enable CSRF and send Spring’s `X-XSRF-TOKEN` cookie/header. Do not add a JSON Web Token for this take-home.

## Search suggestions

[SPECS.md](SPECS.md) mentioned a 5-row typeahead. I did not build it. Typing in the header filters the catalog grid (debounce 300 ms, `q` only if length ≥ 2).

## Product photos

The example file has no image column. The storefront picks a photo from the product name (or category) and falls back if that URL fails. Same product code still gets the same picture.
