# RedWhale Bluetooth Chat Application - User Manual

## 1. Introduction

RedWhale is a simple Android application that allows two users to chat with each other over a Bluetooth connection. This document provides an overview of the application's functionality and the technology used to build it.

## 2. Functionality

The application has the following features:

*   **Enable Bluetooth:** Users can switch on their device's Bluetooth from within the application.
*   **Scan for Devices:** Users can scan for other Bluetooth-enabled devices in the vicinity.
*   **Device List:** The application displays a list of paired and available devices.
*   **Connect to a Device:** Users can select a device from the list to establish a Bluetooth connection.
*   **Chat:** Once connected, users can send and receive text messages in a simple chat interface.

## 3. Technology Used

The RedWhale application is built using the following technologies:

*   **Programming Language:** Java
*   **Platform:** Android
*   **Build Tool:** Gradle with Kotlin DSL (`build.gradle.kts`)
*   **Minimum Android Version:** API 24 (Nougat, Android 7.0)

### Android Components

The application utilizes several core Android components:

*   **`BluetoothAdapter`:** This is used for all Bluetooth-related operations, including enabling Bluetooth, scanning for devices, and creating connections.
*   **`Activity`:** The application has two main activities:
    *   `MainActivity`: This is the main screen where users can chat with a connected device.
    *   `DeviceListActivity`: This screen displays a list of paired and available Bluetooth devices for the user to choose from.
*   **`ListView`:** This UI element is used to display the list of chat messages in `MainActivity` and the list of Bluetooth devices in `DeviceListActivity`.
*   **`BroadcastReceiver`:** The application uses a `BroadcastReceiver` to listen for system-wide Bluetooth events, such as when a new device is discovered during a scan.
*   **`Handler`:** A `Handler` is used to pass information from the background threads that manage the Bluetooth connection to the main UI thread. This is essential for updating the chat interface with new messages without blocking the user interface.
