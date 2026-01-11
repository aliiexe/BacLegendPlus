# BacLegend - Ultimate Scattergories Game

A modern, multiplayer-capable word game built with JavaFX, featuring real-time network gameplay, AI-powered word validation, and bilingual support (French/English).

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Architecture](#architecture)
4. [Network & Multiplayer System](#network--multiplayer-system)
5. [Word Validation System](#word-validation-system)
6. [Game Mechanics](#game-mechanics)
7. [Language Support](#language-support)
8. [Database System](#database-system)
9. [User Interface](#user-interface)
10. [Installation & Setup](#installation--setup)
11. [Usage Guide](#usage-guide)
12. [Technical Details](#technical-details)

---

## Overview

BacLegend is a Scattergories-style word game where players must come up with words that:
- Start with a randomly selected letter
- Belong to specific categories (Country, City, Animal, etc.)
- Are valid words in the selected language (French or English)

The game supports both **solo mode** and **multiplayer mode** over local networks, with real-time synchronization, score calculation, and player management.

---

## Features

### Core Gameplay
- **Solo Mode**: Play against yourself with customizable time limits
- **Multiplayer Mode**: Host or join games over local network (LAN)
- **Dynamic Categories**: Manage custom categories through the UI
- **Configurable Time Limits**: 30s to 180s per round
- **Score Tracking**: Real-time score calculation with duplicate detection
- **Round System**: Multiple rounds with new letters each time

### Network Features
- **Host/Client Architecture**: One player hosts, others join via game code
- **Unique Game Codes**: Compressed IP:Port codes with random suffixes
- **Automatic IP Detection**: Finds local network IP automatically
- **Player Management**: Real-time player list synchronization
- **Disconnection Handling**: Graceful handling of player disconnections
- **Game State Synchronization**: Letter, time, and language sync across all players

### Validation System
- **Dual Validation**: Database-first, AI fallback
- **Language-Aware**: Validates words in French or English based on game setting
- **Caching**: Validated words saved to database for faster future lookups
- **AI Integration**: OpenRouter API integration for intelligent validation
- **Fallback Mode**: Works without AI if API key is invalid

### Internationalization
- **Bilingual UI**: Full French/English interface translation
- **Category Translation**: Category names translated based on language
- **Language Selection**: Per-game language setting (solo: user choice, multiplayer: host choice)

### User Experience
- **Modern UI**: Dark theme with game-inspired design
- **Animations**: Smooth countdown animations and transitions
- **Notifications**: Toast notifications for player events
- **Responsive Layout**: Adapts to different window sizes
- **Visual Feedback**: Progress bars, score displays, and status indicators

---

## Architecture

### Project Structure

```
src/main/java/com/emsi/baclegend/
├── App.java                          # Main application entry point
├── controller/                       # JavaFX controllers
│   ├── MainController.java          # Main menu
│   ├── GameController.java          # Game screen logic
│   ├── LobbyController.java         # Multiplayer lobby
│   ├── SettingsController.java      # Settings screen
│   └── CategoryController.java      # Category management
├── service/                         # Business logic
│   ├── MoteurJeu.java               # Game engine
│   ├── ServiceReseau.java           # Network service
│   └── ServiceValidation.java       # Word validation
├── dao/                              # Data access objects
│   ├── GestionnaireBaseDeDonnees.java  # DB connection manager
│   ├── CategorieDAO.java            # Category operations
│   ├── MotDAO.java                  # Word operations
│   └── ScoreDAO.java                # Score operations
├── model/                            # Data models
│   ├── Categorie.java               # Category model
│   ├── Mot.java                     # Word model
│   ├── SessionJeu.java              # Game session
│   └── Joueur.java                  # Player model
└── util/                             # Utilities
    ├── CodeUtils.java               # Game code compression
    └── TranslationUtil.java        # Translation system
```

### Key Components

- **App.java**: Global state management (game time, language, network service)
- **MoteurJeu**: Core game logic (rounds, scoring, validation)
- **ServiceReseau**: Network communication (server/client handling)
- **ServiceValidation**: Word validation (database + AI)
- **GameController**: Main game UI and multiplayer coordination

---

## Network & Multiplayer System

### Architecture

BacLegend uses a **host-client model** where one player acts as the server (host) and others connect as clients.

#### Server (Host) Responsibilities
- Manages game state (letter, time, language)
- Broadcasts game events to all clients
- Collects answers from all players
- Calculates scores and detects duplicates
- Handles player disconnections
- Synchronizes player lists

#### Client Responsibilities
- Connects to host via game code
- Sends player name and answers
- Receives game state updates
- Displays synchronized information

### Network Protocol

#### Message Format
All messages are plain text strings sent over TCP sockets, terminated with newlines.

#### Message Types

| Message | Direction | Description |
|---------|-----------|-------------|
| `NAME:<pseudo>` | Client → Server | Client sends their username |
| `LETTER:<char>` | Server → Clients | Server broadcasts the round letter |
| `TIME:<seconds>` | Server → Clients | Server broadcasts game time duration |
| `LANGUAGE:<FR\|EN>` | Server → Clients | Server broadcasts game language |
| `LANGUAGE_NOTIFICATION:<text>` | Server → Clients | Language display notification |
| `START:<char>` | Server → Clients | Start game with letter |
| `PLAYERS:<list>` | Server → Clients | Synchronized player list |
| `ANSWERS:<json>` | Client → Server | Client submits their answers |
| `FINISHED:<pseudo>` | Client → Server | Client finished submitting |
| `RESULTS:<json>` | Server → Clients | Final scores and results |
| `DISCONNECT:<pseudo>` | Client → Server | Client disconnecting gracefully |
| `PLAYER_DISCONNECTED:<pseudo>` | Server → Clients | Notification of player leaving |
| `GAME_ENDED_SOLO` | Server → Clients | Game ended (only 1 player left) |
| `SERVER_STOP` | Server → Clients | Server shutting down |

### IP Address Detection

The system automatically detects the local network IP address for LAN connections.

#### Implementation (`LobbyController.getLocalNetworkIP()`)

1. **Network Interface Enumeration**: Iterates through all network interfaces
2. **Filtering**:
   - Skips loopback interfaces (`127.0.0.1`)
   - Skips inactive interfaces
   - Skips link-local addresses
   - Prefers IPv4 addresses (for LAN compatibility)
3. **Selection**: Returns the first valid non-loopback IPv4 address found
4. **Fallback**: If no network interface is found, falls back to `localhost` (`127.0.0.1`)

**Code Location**: `src/main/java/com/emsi/baclegend/controller/LobbyController.java:349-390`

#### Example Detection Logic
```java
// Iterates through NetworkInterface.getNetworkInterfaces()
// Filters: !isLoopback() && isUp() && !isLinkLocalAddress()
// Returns first IPv4 address found (format: "192.168.1.100")
```

### Game Code System

#### Code Generation (`CodeUtils.compress()`)

Game codes are **compressed IP:Port combinations** with a random suffix for uniqueness.

**Format**:
- **IPv4**: 4 bytes (IP) + 2 bytes (Port) = 6 bytes → Base64 encoded (~8 chars) + 4 random chars = **12 characters total**
- **Fallback**: Full string encoding + 4 random chars

**Compression Algorithm**:
1. Parse IPv4 address into 4 bytes
2. Encode port as 2 bytes (big-endian)
3. Base64 URL-safe encode (no padding)
4. Append 4-character random suffix (A-Z, 2-9, excluding confusing chars: 0, O, I, 1)

**Example**:
- IP: `192.168.1.15`, Port: `9999`
- Encoded: `wKgBexNM` (8 chars) + `ABCD` (4 random) = `wKgBexNMABCD`

#### Code Decompression (`CodeUtils.decompress()`)

1. Remove last 4 characters (random suffix)
2. Base64 decode
3. If 6 bytes: Extract IP (first 4 bytes) and Port (last 2 bytes)
4. Return `"IP:Port"` format

**Code Location**: `src/main/java/com/emsi/baclegend/util/CodeUtils.java`

### Connection Flow

#### Hosting a Game

1. User clicks "Create Server" in lobby
2. System detects local network IP
3. Server starts on port 9999 (or random port if 9999 is taken)
4. IP and port are compressed into a game code
5. Code is displayed to host
6. Host shares code with other players
7. When clients connect:
   - Server accepts connection
   - Client sends `NAME:<pseudo>`
   - Server adds player to list
   - Server broadcasts updated player list

#### Joining a Game

1. User enters game code
2. Code is decompressed to `IP:Port`
3. Client connects to server via TCP socket
4. Client sends `NAME:<pseudo>`
5. Server adds client to player list
6. Server broadcasts player list to all clients
7. Client receives and displays player list

### Port Management

- **Default Port**: `9999` (fixed to keep codes short)
- **Fallback**: If port 9999 is unavailable, uses random port (0 = system-assigned)
- **Port Reuse**: `serverSocket.setReuseAddress(true)` allows quick restart

### Disconnection Handling

#### Client Disconnection

**Graceful Disconnect**:
1. Client sends `DISCONNECT:<pseudo>` message
2. Server receives message and removes player from tracking
3. Server broadcasts `PLAYER_DISCONNECTED:<pseudo>` to remaining clients
4. Server removes player from `allSubmittedAnswers` and `finishedPlayers`
5. Client closes socket

**Ungraceful Disconnect** (network error, crash):
1. Server detects socket closure (`IOException` in `ClientHandler.run()`)
2. Server removes client from `clients` list
3. Server calls `onClientDisconnected()` callback
4. Server removes player from tracking
5. Server broadcasts disconnection notification

#### Host Disconnection

- If host quits, all clients receive `SERVER_STOP` message
- Clients show error and return to main menu
- All connections are closed

#### Two-Player Game Handling

- If only 2 players and one disconnects:
  - Remaining player receives `GAME_ENDED_SOLO` message
  - Game ends automatically
  - Player is notified that multiplayer requires at least 2 players

**Code Location**: `src/main/java/com/emsi/baclegend/controller/GameController.java:setupMultiplayerCallbacks()`

### Thread Safety

- **ConcurrentHashMap**: Used for `allSubmittedAnswers` (thread-safe map)
- **Collections.synchronizedSet**: Used for `finishedPlayers` and `disconnectedPlayers`
- **Platform.runLater()**: All UI updates from network threads are dispatched to JavaFX thread

### Network Service Implementation

**Class**: `ServiceReseau.java`

#### Server Side
- `ServerSocket` listens on specified port
- Each client connection spawns a `ClientHandler` thread
- `ClientHandler` reads messages and forwards to controller via callback
- `broadcast()` sends message to all connected clients
- `broadcastExclude()` sends to all except one client

#### Client Side
- `Socket` connects to server IP:Port
- Separate thread (`clientThread`) reads incoming messages
- Messages forwarded to controller via `MessageCallback`
- `envoyerMessage()` sends messages to server

#### Connection States
- `isConnected`: True when connected (as client) or server running (as host)
- `isServerRunning`: True only when server is active
- `myPseudo`: Current player's username
- `opponentPseudo`: Last received opponent name (legacy, for compatibility)

---

## Word Validation System

### Validation Flow

The validation system uses a **two-tier approach**: database-first, AI fallback.

#### Step 1: Database Lookup

1. Word is normalized (trimmed, lowercased)
2. Database query: `SELECT * FROM mots WHERE contenu = ? AND categorie_id = ?`
3. If found: Return stored `est_valide` boolean
4. If not found: Proceed to AI validation

#### Step 2: AI Validation (if not in database)

1. Check if AI validation is enabled (`aiValidationDisabled` flag)
2. If disabled: Accept words with length >= 2, save to database as valid
3. If enabled:
   - Build JSON request to OpenRouter API
   - Send validation request with category and language context
   - Parse AI response (JSON or text)
   - If valid: Save word to database for future use
   - Return validation result

### AI Integration

#### API Details
- **Provider**: OpenRouter.ai
- **Model**: `nvidia/nemotron-3-nano-30b-a3b:free`
- **Endpoint**: `https://openrouter.ai/api/v1/chat/completions`
- **Method**: POST with JSON body

#### Request Format
```json
{
  "model": "nvidia/nemotron-3-nano-30b-a3b:free",
  "temperature": 0.1,
  "messages": [
    {
      "role": "system",
      "content": "You are a validator for a word game (Scattergories). Return ONLY valid JSON: {\"valid\": boolean}. Check if the word belongs to the category. [Language-specific instructions]"
    },
    {
      "role": "user",
      "content": "Category: [category], Word: [word], Language: [French/English]"
    }
  ]
}
```

#### Language-Specific Instructions
- **French**: "The word must be a valid French word. Validate only if the word exists in French and belongs to the category."
- **English**: "The word must be a valid English word. Validate only if the word exists in English and belongs to the category."

#### Response Parsing

The system handles multiple response formats:

1. **JSON Response**: `{"valid": true}` or `{"valid": false}`
2. **Text with JSON**: Extracts JSON from markdown code blocks (```json ... ```)
3. **Plain Text**: Searches for keywords:
   - `true`, `yes`, `oui` → Valid
   - `false`, `no`, `non` → Invalid

#### API Key Management

- **Environment Variable**: `OPENROUTER_API_KEY` (preferred)
- **Fallback**: Hardcoded key in `apiKey()` method (may be invalid/expired)
- **Error Handling**: If HTTP 401 (Unauthorized) received:
  - `aiValidationDisabled` flag is set to `true`
  - Detailed error message logged with instructions
  - System falls back to basic validation (length >= 2)
  - Words are saved to database as valid for future use

**Code Location**: `src/main/java/com/emsi/baclegend/service/ServiceValidation.java`

### Validation Rules

1. **First Letter Check**: Word must start with the round's letter (case-insensitive)
2. **Category Match**: Word must belong to the specified category
3. **Language Match**: Word must be valid in the selected language (French or English)
4. **Minimum Length**: Words must be at least 2 characters (when AI is disabled)

### Database Caching

- Validated words are saved to `mots` table
- Future lookups skip AI validation (faster)
- Database structure:
  ```sql
  CREATE TABLE mots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    contenu TEXT NOT NULL,
    categorie_id INTEGER,
    est_valide INTEGER DEFAULT 1,
    FOREIGN KEY(categorie_id) REFERENCES categories(id)
  )
  ```

### Validation in Multiplayer

- Each player's words are validated independently
- Host collects all answers and validates them
- Duplicate words (same word by multiple players) are detected
- Scoring:
  - **Unique valid word**: 10 points
  - **Duplicate valid word**: 0 points (no one gets points)
  - **Invalid word**: 0 points

**Code Location**: `src/main/java/com/emsi/baclegend/controller/GameController.java:calculateAndBroadcastResults()`

---

## Game Mechanics

### Round Flow

#### Solo Mode
1. User selects "Solo Mode" from main menu
2. Game generates random letter (A-Z)
3. Countdown animation plays (READY → SET → GO! → Letter)
4. Timer starts (configurable duration)
5. User enters words for each category
6. User clicks "VALIDATE" or timer expires
7. Words are validated
8. Results displayed with score
9. User can start next round or quit

#### Multiplayer Mode

**Host Flow**:
1. Host creates server and shares game code
2. Clients join using code
3. Host selects game language and time duration
4. Host clicks "START GAME"
5. Host generates random letter
6. Host broadcasts letter, time, and language to all clients
7. All players see countdown and timer
8. Players submit answers
9. Host collects all answers
10. Host validates all words and calculates scores
11. Host broadcasts results to all clients
12. All players see results
13. Host can start next round

**Client Flow**:
1. Client enters game code and joins
2. Client receives game settings (time, language)
3. Client receives letter when host starts
4. Client enters words and submits
5. Client receives results from host
6. Client waits for next round

### Letter Generation

- **Random Selection**: `(char) ('A' + random.nextInt(26))`
- **Solo**: Generated locally in `SessionJeu.demarrerPartie()`
- **Multiplayer**: Generated by host, broadcasted to clients
- **Shared Letter**: Stored in `App.sharedLetter` for synchronization

### Timer System

- **Configurable Duration**: 30s, 45s, 60s, 90s, 120s, 180s
- **Progress Bar**: Visual countdown indicator
- **Auto-Submit**: If timer expires, answers are automatically submitted
- **Synchronization**: In multiplayer, all players use the same duration (set by host)

### Scoring System

#### Solo Mode
- **Valid word**: +10 points
- **Invalid word**: 0 points
- **Empty field**: 0 points

#### Multiplayer Mode
- **Unique valid word**: +10 points
- **Duplicate valid word**: 0 points (if 2+ players have the same word, no one scores)
- **Invalid word**: 0 points
- **Empty field**: 0 points

#### Duplicate Detection

The host maintains a map of words by category:
```java
Map<String, Map<String, Integer>> dupeMap
// Key: category name, Value: Map<word, count>
```

For each category:
1. Count occurrences of each word
2. If count > 1: Mark as duplicate (0 points for all)
3. If count == 1: Award 10 points

**Code Location**: `src/main/java/com/emsi/baclegend/controller/GameController.java:calculateAndBroadcastResults()`

### Game State Management

**SessionJeu Model**:
- `lettreCourante`: Current round letter
- `enCours`: Whether round is active
- `tempsLimite`: Round duration in seconds
- `categories`: List of active categories
- `idSession`: Unique session ID (UUID)

**MoteurJeu Engine**:
- Manages game session
- Handles word submission
- Calculates scores
- Validates words via `ServiceValidation`

---

## Language Support

### Language Selection

#### Solo Mode
- User selects language in **Settings** screen
- Language is stored in `App.gameLanguage` ("FR" or "EN")
- Applied to all solo games until changed

#### Multiplayer Mode
- Host selects language in **Lobby** screen before creating server
- Language is broadcasted to all clients via `LANGUAGE:<FR|EN>` message
- All players use the same language for the game session
- Language is displayed in game UI (top-right corner)

### Translation System

**TranslationUtil Class**:
- Centralized translation management
- Static maps for French and English translations
- Category name translations
- Dynamic UI text updates

#### Translation Keys

All UI text uses translation keys:
- `game.title`, `game.subtitle`, `game.solo`, etc.
- `settings.title`, `settings.time`, etc.
- `lobby.title`, `lobby.create`, etc.
- `player.left`, `game.ended.solo`, etc.

#### Category Translations

Categories are translated based on current language:
- **French**: "Pays", "Ville", "Animal", "Plante", "Métier", "Nom fille", "Nom garçon"
- **English**: "Country", "City", "Animal", "Plant", "Profession", "Girl's Name", "Boy's Name"

**Code Location**: `src/main/java/com/emsi/baclegend/util/TranslationUtil.java`

### Language-Aware Validation

- Validation requests to AI include language context
- Database queries consider language (stored with words)
- Category matching uses translated names

---

## Database System

### Database Technology
- **SQLite**: Embedded database (`baclezend.db` file)
- **Location**: Project root directory
- **Connection**: Managed by `GestionnaireBaseDeDonnees`

### Schema

#### Categories Table
```sql
CREATE TABLE categories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nom TEXT NOT NULL UNIQUE,
  est_active INTEGER DEFAULT 1
)
```

**Default Categories**:
- Pays (Country)
- Ville (City)
- Animal
- Plante (Plant)
- Métier (Profession)

#### Words Table
```sql
CREATE TABLE mots (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  contenu TEXT NOT NULL,
  categorie_id INTEGER,
  est_valide INTEGER DEFAULT 1,
  FOREIGN KEY(categorie_id) REFERENCES categories(id)
)
```

**Purpose**: Cache validated words for faster future lookups

#### Scores Table
```sql
CREATE TABLE scores (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  pseudo TEXT NOT NULL,
  score INTEGER NOT NULL,
  date_utc INTEGER NOT NULL
)
```

**Purpose**: Store high scores (if implemented)

### Data Access Objects (DAO)

#### CategorieDAO
- `obtenirToutes()`: Get all active categories
- `ajouter()`: Add new category
- `supprimer()`: Delete category (soft delete via `est_active`)

#### MotDAO
- `trouverParContenu()`: Find word by content and category
- `sauvegarder()`: Save new word with validation result

#### ScoreDAO
- Score persistence (if implemented)

### Database Initialization

- Database is initialized on application startup (`App.start()`)
- Tables are created if they don't exist
- Default categories are inserted if not present

**Code Location**: `src/main/java/com/emsi/baclegend/dao/GestionnaireBaseDeDonnees.java`

---

## User Interface

### Screen Structure

#### Main Menu (`main.fxml`)
- Game title and subtitle
- Username input
- Menu buttons:
  - Solo Mode
  - Multiplayer
  - Manage Categories
  - Settings
  - Quit

#### Lobby Screen (`lobby.fxml`)
- **Create Game Section**:
  - Time duration selector (30s-180s)
  - Language selector (French/English)
  - "Create Server" button
  - Game code display (with copy button)
- **Join Game Section**:
  - Game code input
  - "Join" button
- **Player List**: Dynamic list of connected players
- **Start Game Button**: Visible only to host when 2+ players

#### Game Screen (`game.fxml`)
- **Top HUD Bar**:
  - Current letter display
  - Timer progress bar
  - Score display
  - Language indicator (multiplayer only)
- **Center**: Category input fields (scrollable)
- **Bottom**: Action buttons (Quit, Validate)
- **Overlays**:
  - Results overlay (scores and details)
  - Countdown overlay (blurred background)
  - Notification area (top-right)

#### Settings Screen (`settings.fxml`)
- Time duration slider (30s-180s)
- Language selector (French/English)
- Save and Back buttons

#### Categories Screen (`categories.fxml`)
- List of existing categories
- Add new category input
- Back button

### UI Design

- **Theme**: Dark theme with vibrant accents
- **Colors**: 
  - Background: Deep dark (`#1a1a2e`)
  - Accents: Cyan, neon green, amber
  - Cards: Semi-transparent dark with borders
- **Typography**: Modern sans-serif fonts
- **Animations**: 
  - Countdown animations (scale + fade)
  - Button hover effects
  - Toast notifications
- **Responsive**: Adapts to window resizing

### Styling

**CSS File**: `src/main/resources/com/emsi/baclegend/style.css`

Key style classes:
- `.root`: Main background
- `.game-card`: Card containers
- `.category-card`: Category input rows
- `.button`, `.button-primary`, `.button-success`, `.button-danger`: Button variants
- `.hud-panel`: HUD elements
- `.countdown-ready`, `.countdown-set`, `.countdown-go`, `.countdown-letter`: Countdown styles

---

## Installation & Setup

### Prerequisites
- **Java**: JDK 11 or higher
- **JavaFX**: Included via Maven dependencies
- **Maven**: For dependency management
- **SQLite JDBC**: Included via Maven

### Build & Run

1. **Clone/Download** the project
2. **Navigate** to project directory
3. **Build**:
   ```bash
   mvn clean compile
   ```
4. **Run**:
   ```bash
   mvn javafx:run
   ```

### Configuration

#### AI Validation (Optional)
1. Get API key from [OpenRouter.ai](https://openrouter.ai/keys)
2. Set environment variable:
   ```bash
   export OPENROUTER_API_KEY=your_key_here
   ```
   Or on Windows:
   ```cmd
   set OPENROUTER_API_KEY=your_key_here
   ```
3. If API key is not set or invalid, the game will:
   - Disable AI validation
   - Accept words with length >= 2
   - Save words to database for future use

#### Database
- Database file (`baclezend.db`) is created automatically
- Located in project root directory
- No manual setup required

---

## Usage Guide

### Solo Mode

1. **Set Username**: Enter your name on main menu
2. **Configure Settings** (optional):
   - Go to Settings
   - Adjust time duration
   - Select language (French/English)
   - Save
3. **Start Game**: Click "Solo Mode"
4. **Play Round**:
   - Wait for countdown (READY → SET → GO! → Letter)
   - Enter words starting with the letter for each category
   - Click "VALIDATE" or wait for timer
5. **View Results**: See your score and validated words
6. **Next Round**: Click "Next Round" to play again with a new letter

### Multiplayer Mode

#### As Host

1. **Set Username**: Enter your name on main menu
2. **Go to Lobby**: Click "Multiplayer"
3. **Configure Game**:
   - Select time duration (30s-180s)
   - Select language (French/English)
4. **Create Server**: Click "Create Server"
5. **Share Code**: Copy the game code and share with other players
6. **Wait for Players**: Players will appear in the player list
7. **Start Game**: Click "START GAME" when 2+ players are connected
8. **Play**: Enter words and validate
9. **View Results**: See scores for all players
10. **Next Round**: Start another round or quit

#### As Client

1. **Set Username**: Enter your name on main menu
2. **Go to Lobby**: Click "Multiplayer"
3. **Enter Code**: Get game code from host and enter it
4. **Join**: Click "Join"
5. **Wait**: Wait for host to start the game
6. **Play**: Enter words and validate
7. **View Results**: See scores for all players
8. **Wait for Next Round**: Host controls when next round starts

### Managing Categories

1. **Go to Categories**: Click "Manage Categories" on main menu
2. **View List**: See all existing categories
3. **Add Category**: 
   - Enter category name in text field
   - Click "Add"
4. **Categories are used in all games** (solo and multiplayer)

---

## Technical Details

### Threading Model

- **JavaFX Application Thread**: All UI updates
- **Network Threads**: 
  - Server thread: Accepts client connections
  - Client handler threads: One per connected client
  - Client read thread: Reads messages from server
- **Platform.runLater()**: Used to dispatch network callbacks to JavaFX thread

### Synchronization

- **ConcurrentHashMap**: Thread-safe map for player answers
- **Collections.synchronizedSet**: Thread-safe sets for player tracking
- **Volatile flags**: `isConnected`, `isServerRunning` for state management

### Error Handling

- **Network Errors**: Caught and displayed to user via alerts
- **Validation Errors**: Logged to console, word marked as invalid
- **Database Errors**: Logged, fallback to AI validation
- **AI API Errors**: Handled gracefully, fallback to basic validation

### Performance Considerations

- **Database Caching**: Validated words cached to avoid repeated AI calls
- **Connection Reuse**: Server socket reuse address enabled
- **Efficient Serialization**: JSON for complex data, plain text for simple messages

### Security Notes

- **Local Network Only**: No internet-wide exposure (LAN only)
- **No Authentication**: Username-based identification (no password)
- **Input Validation**: Words validated before scoring
- **SQL Injection**: Protected via prepared statements (DAO layer)

### Known Limitations

1. **LAN Only**: Cannot play over internet (requires port forwarding)
2. **No Reconnection**: If connection drops, must rejoin
3. **Host Dependency**: If host quits, game ends for all
4. **AI API Dependency**: Requires valid API key for best validation
5. **Single Database**: Each player has their own local database (not shared)

### Future Enhancements (Potential)

- Internet multiplayer via relay server
- Persistent high score leaderboard
- Custom category sharing
- Reconnection handling
- Spectator mode
- Tournament mode

---

## Code Examples

### Creating a Game Code
```java
String ip = "192.168.1.15";
int port = 9999;
String code = CodeUtils.compress(ip, port);
// Result: "wKgBexNMABCD" (12 characters)
```

### Decompressing a Game Code
```java
String code = "wKgBexNMABCD";
String decoded = CodeUtils.decompress(code);
// Result: "192.168.1.15:9999"
```

### Validating a Word
```java
ServiceValidation validator = new ServiceValidation();
Categorie cat = new Categorie("Animal", 1);
boolean isValid = validator.validerMot("tigre", cat, "FR");
// Returns true if "tigre" is a valid French word for "Animal" category
```

### Broadcasting a Message (Host)
```java
App.networkService.broadcast("LETTER:A");
// Sends "LETTER:A" to all connected clients
```

### Sending a Message (Client)
```java
App.networkService.envoyerMessage("ANSWERS:{\"Animal\":\"tigre\"}");
// Sends answers to host
```

---

## License

This project is part of an academic project (PFM - Projet de Fin de Module) for EMSI (École Marocaine des Sciences de l'Ingénieur).

---

## Contact & Support

For issues, questions, or contributions, please refer to the project repository or contact the development team.

---

**Last Updated**: January 2026
**Version**: 1.0
**Platform**: JavaFX (Desktop)
