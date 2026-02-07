# SunSeeker 🌅

SunSeeker is a collaborative social platform designed for discovering, planning, and sharing sunrise and sunset viewing events. The application provides a centralized hub for nature enthusiasts to connect and experience these moments together.

## Project Overview

The primary goal of SunSeeker is to foster a community around the appreciation of daily solar events. It combines event coordination with social sharing, allowing users to move from the digital planning stage to real-world experiences and back to digital memory-keeping.

## Key Features

* **Sunrise & Sunset Feed:** A dynamic interface where users can discover upcoming viewing events.
* Create and host new viewing sessions.
* Browse and join existing events.
* Interact with event-specific content, including photos and comments from other participants.


* **Personalized Event Management:** A dedicated space for users to track their activities.
* Monitor upcoming events you have committed to attend.
* Manage group memberships and personal event history.


* **SunSeeker Assistant:** A suite of collaborative tools integrated into every event.
* **Real-time Communication:** Group chat functionality for participants to coordinate logistics (e.g., exact meeting spots, weather updates).
* **Shared Media Galleries:** A collaborative photo album where all attendees can upload and view high-quality memories from the event.


* **User Management:** Secure authentication system for creating personal profiles, enabling a customized experience and persistent social connections.

## Target Platform

SunSeeker is designed as a mobile-first experience, optimized for use in the field while chasing the perfect horizon.

## Firebase Setup (Required)

Authentication uses Firebase. To run the app locally, add your Firebase config file and enable Email/Password auth:

1. In the Firebase Console, create a project and register an Android app with the package name `com.example.sunseeker_app`.
2. Download `google-services.json`.
3. Place it at: `app/google-services.json`.
4. In Firebase Console, enable **Authentication -> Sign-in method -> Email/Password**.

## Firebase Storage (Required for Event Images)

Event images are uploaded to Firebase Storage. To enable:

1. In Firebase Console, open **Storage** and create a default bucket.
2. For development, allow authenticated users to read/write to `events/**` and `profiles/**`.

If you need me to wire additional Firebase settings (e.g., Firestore rules or SHA-1/256), share them and I’ll update the project config and docs.
