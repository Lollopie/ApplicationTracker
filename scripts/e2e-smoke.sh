#!/usr/bin/env bash
# End-to-end smoke test that talks to a running stack exactly like the SPA does:
# same-origin API under a prefix, a cookie jar, and the X-XSRF-TOKEN header taken from the cookie.
#
#   scripts/e2e-smoke.sh                                    # docker compose (http://localhost:3000)
#   BASE=https://example.com APP_PREFIX=/tracker API_PREFIX=/tracker/api CHECK_HEADERS=0 scripts/e2e-smoke.sh
#
# It registers a throwaway user (e2e_<timestamp>) with a couple of applications. There is no API to delete users.
BASE=${BASE:-http://localhost:3000}
APP_PREFIX=${APP_PREFIX:-}
API_PREFIX=${API_PREFIX:-$APP_PREFIX/api}
CHECK_HEADERS=${CHECK_HEADERS:-1}
COOKIE_PATH=${APP_PREFIX:-/}

JAR=$(mktemp)
HEADERS=$(mktemp)
trap 'rm -f "$JAR" "$HEADERS"' EXIT
PASS=0; FAIL=0
USER_NAME="e2e_$(date +%s)"

token() { awk '$6=="XSRF-TOKEN"{print $7}' "$JAR"; }
cookie_path() { awk -v name="$1" '$6==name{print $3}' "$JAR"; }
# call METHOD PATH [BODY]: sets STATUS and BODY
call() {
  local method=$1 path=$2 body=$3
  local args=(-s -b "$JAR" -c "$JAR" -D "$HEADERS" -X "$method" -H "Accept: application/json" -w '\n%{http_code}')
  [ "$method" != GET ] && args+=(-H "X-XSRF-TOKEN: $(token)")
  [ -n "$body" ] && args+=(-H "Content-Type: application/json" -d "$body")
  local out; out=$(curl "${args[@]}" "$BASE$API_PREFIX$path")
  STATUS=${out##*$'\n'}; BODY=${out%$'\n'*}
}
csrf() { curl -s -b "$JAR" -c "$JAR" -o /dev/null "$BASE$API_PREFIX/csrf"; }
json() { node -e "try{const d=JSON.parse(process.argv[1]);console.log(eval('d'+process.argv[2]))}catch{console.log('<not json>')}" "$BODY" "$1"; }
check() {
  if [ "$2" == "$3" ]; then PASS=$((PASS+1)); echo "  ok   $1"
  else FAIL=$((FAIL+1)); echo "  FAIL $1 (expected '$3', got '$2')"; fi
}

echo "Target: $BASE  app: ${APP_PREFIX:-/}  api: $API_PREFIX"
echo "SPA"
check "index.html is served" "$(curl -s "$BASE$APP_PREFIX/" | grep -o '<title>Job Tracker</title>')" "<title>Job Tracker</title>"
check "deep link falls back to the SPA" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE$APP_PREFIX/applications/x")" 200
ASSET=$(curl -s "$BASE$APP_PREFIX/" | grep -o "$APP_PREFIX/assets/[^\"]*\.js" | head -1)
check "bundle is served under the prefix" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE$ASSET")" 200
if [ "$CHECK_HEADERS" = 1 ]; then
  check "CSP header on the SPA" "$(curl -sI "$BASE$APP_PREFIX/" | grep -ci '^content-security-policy')" 1
  check "hashed asset is immutable" "$(curl -sI "$BASE$ASSET" | grep -ci 'cache-control: public, max-age=31536000, immutable')" 1
fi

echo "Auth"
call GET /me;                                        check "GET /me without session -> 401" "$STATUS" 401
csrf;                                                check "GET /csrf sets a readable XSRF-TOKEN" "$([ -n "$(token)" ] && echo yes)" yes
check "  scoped to $COOKIE_PATH" "$(cookie_path XSRF-TOKEN)" "$COOKIE_PATH"
call POST /register "{\"username\":\"$USER_NAME\",\"password\":\"short\"}"
check "register with short password -> 400" "$STATUS" 400
check "  field error for password" "$(json '.errors.password.length')" 1
call POST /register "{\"username\":\"$USER_NAME\",\"password\":\"password123\"}"; check "register -> 201" "$STATUS" 201
call POST /register "{\"username\":\"$USER_NAME\",\"password\":\"password123\"}"; check "duplicate register -> 409" "$STATUS" 409
check "  detail" "$(json '.detail')" "Username already exists."
call POST /login "{\"username\":\"$USER_NAME\",\"password\":\"wrong-pass\"}";   check "login with wrong password -> 401" "$STATUS" 401
call POST /login "{\"username\":\"$USER_NAME\",\"password\":\"password123\"}"; check "login -> 200" "$STATUS" 200
# The session cookie is HttpOnly, which curl's jar marks with a #HttpOnly_ prefix (other # lines are comments).
SESSION_COOKIE=$(awk 'NF>=7 && ($1 !~ /^#/ || $1 ~ /^#HttpOnly_/) && $6!="XSRF-TOKEN" {print $6}' "$JAR" | head -1)
check "session cookie ($SESSION_COOKIE) scoped to $COOKIE_PATH" "$(cookie_path "$SESSION_COOKIE")" "$COOKIE_PATH"
# curl 7.x keeps a cookie in its jar when an expired Set-Cookie differs from it only in attributes, while
# browsers (RFC 6265) delete it. So check the header itself, then drop the token the way a browser would.
check "login clears the CSRF cookie at $COOKIE_PATH" \
  "$(tr -d '\r' < "$HEADERS" | grep -i '^set-cookie: XSRF-TOKEN=;' | grep -Eci "path=$COOKIE_PATH(;|$)")" 1
sed -i '/\tXSRF-TOKEN\t/d' "$JAR"
csrf;                                               check "fresh CSRF token after login" "$([ -n "$(token)" ] && echo yes)" yes
call GET /me;                                        check "GET /me -> username" "$(json '.username')" "$USER_NAME"

echo "Applications"
call POST /applications '{"company":" ","position":"Dev","status":"SAVED"}'
check "create with blank company -> 400" "$STATUS" 400
check "  field error for company" "$(json '.errors.company[0]')" "Company is required"
call POST /applications '{"company":"Acme","position":"Backend Engineer","status":"SAVED","notes":"Referral","links":"https://acme.example/jobs/1"}'
check "create -> 201" "$STATUS" 201
ID=$(json '.id')
call POST /applications '{"company":"Globex","position":"Frontend Engineer","status":"APPLIED"}'; check "create second -> 201" "$STATUS" 201
call GET "/applications?sort=applicationId%2Cdesc&page=0&size=20"
check "list has 2 items" "$(json '.content.length')" 2
check "list page metadata" "$(json '.page.totalElements')" 2
check "newest first" "$(json '.content[0].company')" Globex
call GET "/applications?currentStatus=SAVED&company=acm&sort=company%2Casc&page=0&size=20"
check "filter by status + company substring" "$(json '.content.map(a=>a.company).join()')" Acme
call GET "/applications?bogus=1";                     check "unknown parameter -> 400" "$STATUS" 400

echo "Status workflow"
for s in APPLIED INTERVIEW INTERVIEW OFFER; do
  call POST "/applications/$ID/status" "{\"status\":\"$s\"}"; check "-> $s" "$STATUS" 200
done
check "history SAVED..OFFER" "$(json '.statusHistory.map(s=>s.status).join()')" "SAVED,APPLIED,INTERVIEW,INTERVIEW,OFFER"
call POST "/applications/$ID/status" '{"status":"APPLIED"}'; check "illegal OFFER -> APPLIED -> 409" "$STATUS" 409
check "  detail" "$(json '.detail')" "Cannot change from status OFFER to status APPLIED"

echo "Edit and delete"
call PUT "/applications/$ID" '{"company":"Acme Inc","position":"Senior Backend Engineer"}'
check "update -> 200" "$STATUS" 200
check "  company updated, notes cleared" "$(json '.company + "|" + (d.notes ?? "null")')" "Acme Inc|null"
CODE=$(curl -s -b "$JAR" -X DELETE -H "X-XSRF-TOKEN: wrong" -o /dev/null -w '%{http_code}' "$BASE$API_PREFIX/applications/$ID")
check "stale CSRF header -> 403" "$CODE" 403
call DELETE "/applications/$ID";                      check "delete -> 204" "$STATUS" 204
call GET "/applications/$ID";                         check "deleted -> 404" "$STATUS" 404

echo "Logout"
call POST /logout;                                    check "logout -> 200" "$STATUS" 200
call GET /me;                                         check "GET /me after logout -> 401" "$STATUS" 401

echo; echo "passed: $PASS  failed: $FAIL"
[ "$FAIL" -eq 0 ]
