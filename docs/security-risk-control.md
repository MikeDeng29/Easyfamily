# Security and Risk Control (MVP)

## Authentication controls

- Captcha must be verified before SMS delivery.
- SMS rate limits on IP + phone dimensions.
- Login failures are counted and can trigger temporary cool-down.

## Query controls

- Daily quota per user is configurable via admin endpoint.
- Phone query requests are audited with requester identity and timestamp.
- Sensitive fields should be masked in logs and response payloads when possible.

## Data and compliance

- Store only required personal data (minimum principle).
- Keep third-party query responses in cache with strict TTL.
- Prepare privacy policy and user consent for phone/binding queries.

## Operational controls

- Alert on error spikes and third-party timeout rates.
- Keep immutable audit logs for key admin actions.
