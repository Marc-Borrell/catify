# catify
  ![Static Badge](https://img.shields.io/badge/Android-Java-green?style=for-the-badge&logo=android)
![Static Badge](https://img.shields.io/badge/Cryptography-AES%2FRSA-yellow?style=for-the-badge)
![Static Badge](https://img.shields.io/badge/Sockets-TCP-blue?style=for-the-badge)
![Static Badge](https://img.shields.io/badge/FireBase-orange?style=for-the-badge&logo=firebase)


## Description

This project is an Android application developed in **Java** that allows secure file handling using **cryptography and direct socket communication**.

The app enables users to:
- Encrypt audio files using **AES encryption**
- Sign files using **RSA digital signatures**
- Verify file integrity
- Decrypt previously encrypted files
- Send encrypted files directly between devices using **TCP sockets**
- Receive files through a built-in socket server

The main goal of the project is to combine **file security (cryptography)** with **peer-to-peer communication (sockets)** in a mobile environment.

---

## Features

###  Cryptography
- AES encryption (CBC mode with PKCS5Padding)
- RSA key pair generation
- Digital signatures (SHA-256 with RSA)
- File integrity verification using SHA-256 hashes
- Secure key handling (wrapped symmetric keys)

### File Management
- Select audio files from device storage
- Store encrypted files locally
- Store decrypted files separately
- Automatic file organization:
  - `/encrypted`
  - `/decrypted`
  - `/signed`

###  Socket Communication
- Built-in TCP server on port `9090`
- Direct file transfer between devices
- IP-based connection system
- Real-time file reception

###  Key Management
- Automatic RSA key generation
- Symmetric AES key generation and storage
- Public/private key display (with warning for private key exposure)

---

## Set Up

### Requirements

- Android Studio (latest version recommended)
- Android device or emulator (API 24+ recommended)
- Java 8+
- Permissions for file access and network usage

---

### Installation

1. Clone the repository:

```bash
git clone https://github.com/Marc-Borrell/catify.git
```
2. Open the solution (.sln) in Visual Studio.
3. Restore NuGet packages if prompted (MySql.Data is required).
4. Configure your database connection in the Database class.
5. Build the project by selecting Build > Build Solution.
6. Run the application by clicking Start in Visual Studio.

## How it works
1. Encryption Process <br>
User selects an audio file
File is encrypted using AES
AES key is wrapped using RSA public key
Hash of original file is generated
Files saved:
.enc (encrypted data)
.iv (initialization vector)
.key (wrapped AES key)
.hash (integrity check)
2. Decryption Process <br>
User selects encrypted file
App retrieves related .iv, .key, .hash
AES key is unwrapped using RSA private key
File is decrypted
Integrity is verified using SHA-256 hash
3. File Sending (Sockets) <br>
Receiver starts TCP server on port 9090
Sender enters receiver IP
Encrypted file is sent via socket stream
Receiver stores file locally in /encrypted

## Technologies Used
- Java
- Android SDK
- FireBase
- AES Encryption (javax.crypto)
- RSA Encryption
- SHA-256 hashing
- TCP Sockets (ServerSocket / Socket)
- Android UI (Activities, Navigation Drawer)
- File I/O streams

## DEMO
<p>Login</p>

<img width="394" height="836" alt="image" src="https://github.com/user-attachments/assets/3a1720c9-3181-4361-a15c-bd1f2377e539" />

<p>Sign up</p>
<img width="360" height="761" alt="image" src="https://github.com/user-attachments/assets/d2691285-c4b4-41f0-b8d2-95e0d69c4a43" />

<p>Main Screen</p>
<img width="368" height="807" alt="image" src="https://github.com/user-attachments/assets/a88c1f28-3f1c-46ed-9b0f-460111fbe0b1" />

<p>Send file</p>
<img width="376" height="815" alt="image" src="https://github.com/user-attachments/assets/af1f6822-1b9b-43ad-9ee3-dc4b63ce8a6a" />

<p>Songs with verified singature</p>

<img width="373" height="805" alt="image" src="https://github.com/user-attachments/assets/973afeb1-3d97-4536-87f0-95751dcbc875" /> 

<img width="376" height="824" alt="image" src="https://github.com/user-attachments/assets/4bdf5a6a-f4a8-43a0-bfca-5ea62feed696" />
<p>Lateral menu</p>
<img width="383" height="825" alt="image" src="https://github.com/user-attachments/assets/3961e60f-fc6d-4020-8754-f07db5627c17" />


