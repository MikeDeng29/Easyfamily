# easyfamily Architecture (MVP)

## Overview

`easyfamily` uses a frontend-backend separated architecture:

- `easyfamily-android`: end-user app for login, query, and phone management.
- `easyfamily-admin`: operations/admin web for report and quota config.
- `easyfamily-backend`: Spring Boot API service.

## Backend modules

- `auth`: captcha verification, SMS code delivery, login/token.
- `query`: phone binding query, Redis-first cache with in-memory fallback, and Aliyun Market AppCode provider as default.
- `phone`: my phone list and bind/unbind management.
- `report`: DAU, feature usage, and runtime strategy administration.

## Core flow

1. Client verifies captcha ticket.
2. Client requests SMS code.
3. User logs in by phone + SMS code.
4. User queries phone binding status.
5. Backend checks multi-dimensional quotas (user/phone/IP) and cache before third-party query.
6. Backend records query events for reporting/audit.
