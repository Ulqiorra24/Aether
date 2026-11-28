# Aether Project & Infrastructure Description

## Overview
Aether is an advanced Android VPN and client application built with **Jetpack Compose** and **Kotlin**, backed by Python micro-services hosted on a Moscow server.

## Architecture & Components

### 1. Android App (`com.example.v2rayng`)
- **UI Framework**: Jetpack Compose, Material 3.
- **Navigation**: Custom Bottom Navigation (Home, Servers, News, Support, Profile, Settings).
- **Features**:
  - **News Screen**: Fetches latest announcements and updates from server, with unread badge counter.
  - **Support Chat**: Real-time polling support chat allowing users to chat directly with administrators via Telegram.
  - **Trial / Promo Claiming**: Allows users to claim trial subscription days directly from the app.

### 2. Moscow Server Backend
- **Domain**: `mikaela-go.ru`
- **Nginx Proxy**: Routes `/api/news`, `/api/support` to port 8001; auth endpoints to port 8000.
- **Services**:
  - **`aether-auth.service` (Port 8000)**: Handles user authentication, subscription status, and trial claim logic (`/api/auth/trial`).
  - **`aether-support.service` (Port 8001)**: Handles News API, Support chat messages, and the Telegram Admin Bot (`9007022544:AAFnmEdwftTqd0KfIzJGrAo0GxU2_CXBvoo`).
  - **Telegram Proxy**: All Telegram Bot API requests route through a local SOCKS5 proxy (`socks5://127.0.0.1:62790`) to bypass regional restrictions.
