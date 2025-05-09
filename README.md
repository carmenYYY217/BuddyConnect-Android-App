# BuddyConnect

**Project Overview**  
BuddyConnect is a native Android social connectivity application developed in Java. It is designed to help users find and connect with nearby friends and new people in real time. The app integrates personal scheduling, location sharing, and social interactions, providing an all-in-one platform for communication and planning. In Phase I, the focus will be on detailing the concept, outlining core Android components, and establishing feature priorities.

---

**Key Features**

1. **User Accounts**  
   - **User Registration and Login:**  
     Users can sign up or log in using email/display name and password credentials.  
   - **Profile Management:**  
     Allows storage and updating of user profiles, including display name and profile picture.  
   - **Social Media Integrations [Advanced]:**  
     Facilitates quicker sign-ups or logins through popular social media platforms (e.g., Facebook, X, Google).

2. **Friend Management**  
   - **Add/Remove Friends:**  
     Users can search for others by display name or email and send friend requests.  
   - **Friend List:**  
     Displays friends using a RecyclerView component.

3. **Settings and Privacy**  
   - **Theme:**  
     Provides an option for users to switch between themes (e.g., light/dark mode).  
   - **Status:**  
     Users can choose whether to display their last seen status or remain hidden. Additionally, they can set their current status (e.g., “Inactive”, “Busy”, “Offline”).

4. **Recording**  
   - **Timetable Input and Comparison:**  
     Enables users to input their weekly schedule, allowing the system to identify overlapping free slots among friends.  
   - **Simple Post Sharing [Advanced]:**  
     Users can upload short posts or photos accompanied by text.

5. **Chatbot**  
   - **Advanced Chatbot Integration (API) [Advanced]:**  
     Integrates an external AI (e.g., ChatGPT, xAI) to assist with specific topics.

6. **Planning**  
   - **Place Bookmarks:**  
     Users can bookmark locations (by name/address) for future reference.  
   - **Comments and Rating:**  
     Allows users to comment on and rate various places.

7. **Location Sharing**  
   - **Real-Time Location Sharing:**  
     Displays approximate locations on a map when both the user and a friend are online.  
   - **Connecting with New Friends [Advanced]:**  
     Shows nearby strangers with an "online" status. Users can opt to hide their current location from non-friends.

8. **Other Advanced Features**  
   - **Group Chats/Chat Rooms:**  
     Enables multi-user chats with shareable invitation links or QR codes.  
   - **Search Engine:**  
     Provides a feature to search for specific photos or messages using keywords or tags.  
   - **Automatic Recommendations:**  
     Suggests restaurants, shopping malls, etc., based on the user’s current location within the planning module.

---

**High-Level Design and Architecture**

- **Client Application (Java):**  
  - **User Interface (UI):**  
    Developed in Android Studio with a modular design encompassing a Login Page, Main Menu, and dedicated feature sections (Settings, Add Friends, Connecting, Recording, Planning, Chatbot).  
  - **Data Handling:**  
    Utilizes Firebase as the backend server to store user profiles, friend lists, schedules, and uploaded content.

- **Backend Services:**  
  - **Authentication and User Management:**  
    Manages user sign-up, login, and session management through Firebase Authentication.  
  - **Database and Storage:**  
    Uses Firebase Firestore for storing user data, friend relationships, timetables, messages, and media content (including restaurant and place details).  
  - **Real-Time Connectivity:**  
    Implements real-time location sharing and chat functionalities via WebSockets or Firebase Realtime Database (optional) to ensure up-to-date data synchronization.

- **Security and Privacy:**  
  - **Data Encryption:**  
    Sensitive data is transmitted with end-to-end encryption.  
  - **Multi-Factor Authentication (MFA):**  
    Adds an extra layer of security during login.  
  - **Privacy Controls:**  
    Allows users to manage whether to share their location, last online status, and content visibility.

- **System Flow:**  
  1. **Login:**  
     Users are prompted to log in unless an active session is detected.  
  2. **Main Page:**  
     Upon successful login, users see a menu displaying functions such as Settings, Add Friends, Connect, Record, Timetable Planning, and Chatbot.  
  3. **Feature Selection:**  
     Depending on user choice:  
     - **Add Friends:** Manage friend lists through user search and friend requests.  
     - **Settings:** Update profile, privacy, or theme preferences.  
     - **Connect:** View online and nearby friends, initiate group chats, and share location.  
     - **Record:** Upload personal messages/photos and check free time slot matches with friends.  
     - **Plan:** Browse and add places to visit; provide comments, ratings, and receive recommendations.  
     - **Chatbot:** Engage with AI assistance on specific topics.  
  4. **Database Interaction:**  
     All changes are synchronized with Firebase in real time.

---

**Project Plan and Timeline**

- **Timeline by Phase:**  
  1. **Phase 1 (1 Week):**  
     UI Design; implementation of Login, Authentication, Friend List, Settings & Privacy; Database creation.  
  2. **Phase 2 (2 Weeks):**  
     Implementation of Timetable and Sharing features; Chatbot Integration.  
  3. **Phase 3 (2 Weeks):**  
     Development of the Planning Module.  
  4. **Phase 4 (1 Week, Optional):**  
     Implementation of Real-Time Location Sharing, dependent on project progress.  

  **Total Duration:** 6 weeks

- **Division of Labor:**  
  Responsibilities are divided among team members based on feature sets, with each member overseeing critical components such as backend integration, friend management, UI/UX design, scheduling/recording, and planning features.

- **Feature Difficulty Ranking:**  
  - **Easy:** User Accounts  
  - **Moderate:** Friend Management, Settings/Privacy, Recording, Chatbot Integration, Planning, Database creation  
  - **High:** Real-Time Location Sharing and Other Advanced Features

---

**Expected Outcomes**

- **User Engagement:**  
  Develop a platform that encourages persistent interaction among friends through text messaging, real-time location sharing, and scheduling coordination.  
- **Privacy and Security:**  
  Deliver a secure Android application that safeguards user data with robust privacy controls and encryption.

---

**Conclusion**  
BuddyConnect aims to unify social networking, location sharing, schedule coordination, and planning into a cohesive Android application. By balancing simplicity with advanced features, the app offers a user-friendly environment suitable for a diverse audience. Future updates may enhance the recommendation engine, improve the user interface, and integrate additional real-time analytical tools to further enrich the social experience.