# Orpheus - Android App Demo

> Break-It Early Test Application

# English Version

**App Name:** Orpheus

**Orpheus** is an Android demo application that combines AI emotion analysis with Spotify music streaming. Users input their current state based on five basic emotions (Happy, Sad, Angry, Disgust, Fear) using seek bars. The app then utilizes **Gemini 2.5 Flash** to analyze these emotions and recommend the most suitable music.

### Features

#### **Core Features**

- **Emotion Input**: Accurately describe current mood via seek bars for basic emotions (Happy, Sad, Angry, Disgust, Fear).
- **Smart Music Recommendation**: Combines user input with Gemini analysis to recommend up to 8 matching songs from the Spotify library at once.
- **Spotify Playback Control**: Integrated with Spotify App Remote SDK (Android SDK) to support Play, Pause, Previous, and Next track controls.
- **Favorites**: Save liked songs to a favorites list, supporting date-based filtering.

#### Player Features

- **Player Interface**: Displays album art, track name, artist, and a progress bar.
- **MiniPlayer**: A floating mini-player accessible across different pages for quick control.
- **Playlist Management**: Supports continuous playback of the recommended song list.
- **Progress Tracking**: Real-time display of playback progress and remaining time.

#### User Experience (UX)

- **Spotify OAuth Login**: Secure third-party authorization login.
- **User Profile**: Displays Spotify username and avatar.
- **Favorites Management**: View saved songs filtered by date.
- **Multi-language Support**: Supports Traditional Chinese (Taiwan), Simplified Chinese (Mainland China), English (Default), and Japanese.

### Technical Architecture

This project adopts the **MVVM (Model-View-ViewModel)** architecture, utilizing **Data Binding** and **View Binding**.

```
┌────────────────────────────────────────────────────────────┐
│                      View Layer                            │
│  MainActivity │ UserMainActivity │ PlayerView │ MyListView │
└──────────────────────────┬─────────────────────────────────┘
                           │ Data Binding & LiveData
┌──────────────────────────▼──────────────────────────────┐
│                      ViewModel Layer              		  │
│ LoginViewModel │ MusicViewModel │ PlayerViewModel │ ... │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────┐
│                      Model Layer                    │
│  Repository │ Room Database │ API Services │ POJO   │
└─────────────────────────────────────────────────────┘
```

#### Project Structure

```
app/src/main/java/
├── Model/
│   ├── Api/                    # API Service Interfaces
│   │   ├── ApiClient.java      # Retrofit Client
│   │   ├── GeminiApiService.java
│   │   └── SpotifyApiService.java
│   ├── Cache/
│   │   └── FavoriteCache.java  # LruCache for favorites
│   ├── Dao/
│   │   └── FavoriteDao.java    # Room DAO
│   ├── Database/
│   │   └── AppDatabase.java    # Room Database
│   ├── Entity/
│   │   └── FavoriteEntity.java # Room Entity
│   ├── POJO/                   # Data Models
│   │   ├── MusicItem.java
│   │   ├── EmotionInput.java
│   │   ├── PlaylistData.java
│   │   └── ...
│   ├── Repository/
│   │   ├── MusicRepository.java
│   │   └── FavoriteRepository.java
│   └── Spotify/
│       └── SpotifyPlayerManager.java  # Spotify SDK Manager
├── View/
│   ├── MainActivity.java       # Login Page
│   ├── UserMainActivity.java   # Main Page (Emotion Input + List)
│   ├── PlayerView.java         # Player Page
│   ├── MyListView.java         # Favorites Page
│   ├── UserSettingView.java    # User Settings Page
│   └── adapter/
│       └── MusicListAdapter.java
├── ViewModel/
│   ├── LoginViewModel.java
│   ├── MusicViewModel.java
│   ├── PlayerViewModel.java
│   ├── MiniPlayerViewModel.java
│   ├── MyListViewModel.java
│   └── UserMainViewModel.java
└── Util/
    ├── TokenManager.java       # Token Management
    ├── BindingAdapters.java    # Data Binding Adapters
    └── SliderBindingAdapters.java # Two-way binding for Slider data
```

### Tech Stack

| Category          | Technology                                                   |
| ----------------- | ------------------------------------------------------------ |
| **Language**      | Java 21                                                      |
| **Architecture**  | MVVM + Repository Pattern                                    |
| **UI**            | Material Design 3, Data Binding, ViewBinding                 |
| **Async**         | LiveData, CompletableFuture                                  |
| **Network**       | Retrofit2, OkHttp3                                           |
| **Local Storage** | Room Database, SharedPreferences                             |
| **Caching**       | LruCache                                                     |
| **Image Loading** | Glide                                                        |
| **Auth**          | AppAuth (OAuth 2.0)                                          |
| **Dependencies**  | Spotify App Remote SDK (Android SDK), Google Gemini API, Spotify Web API |

**APIs Used**

- **Spotify Web API**: User authentication & authorization (OAuth 2.0), Song search, User profile retrieval.
- **Spotify App Remote SDK (Android SDK)**: Music playback control (Play, Pause, Skip), Playback state monitoring, Progress tracking.
- **Google Gemini API**: Emotion analysis, Emotion-based music recommendation generation.

### Environment Setup

#### Prerequisites

- Android Studio Ladybug or newer
- JDK 21
- Android SDK 36
- Spotify App (Installed on the testing device)
- Spotify Developer Account
- Google AI Studio Account

#### Setup Steps

1. **Configure API Keys** Create or edit `local.properties` in the project root directory:

   ```
   # Spotify API
   SPOTIFY_CLIENT_ID=your_spotify_client_id
   SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
   
   # Gemini API
   GEMINI_API_KEY=your_gemini_api_key
   ```

2. **Spotify Developer Console Setup**

   - Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
   - Create an app
   - Set the Redirect URI
   - Obtain Client ID and Client Secret

3. **Download Spotify SDK**

   - Download from [Spotify Android SDK](https://github.com/spotify/android-sdk/releases)
   - Place `spotify-app-remote-release-x.x.x.aar` into the `app/libs/` directory

### Application Flow

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Login Page │────▶│    Main Page     │────▶│   Player Page   │
│ (Spotify    │     │ (Emotion Input + │     │ (Full Controls) │
│  OAuth)     │     │  Rec List)       │     │                 │
└─────────────┘     └──────────────────┘     └─────────────────┘
                            │
                            ▼
                    ┌──────────────────┐
                    │  Favorites Page  │
                    │ (Date Filtering) │
                    └──────────────────┘
```

### Key Functions

| Function          | Description                                            |
| ----------------- | ------------------------------------------------------ |
| **Login**         | Secure login via Spotify OAuth.                        |
| **Emotion Input** | 5-dimensional slider to adjust emotion values (0-100). |
| **Direct Input**  | Text description of current feelings.                  |
| **Rec List**      | Displays 8 songs recommended by AI.                    |
| **Player**        | Full playback control and progress display.            |
| **MiniPlayer**    | Quick controls via a mini player.                      |
| **Favorites**     | Save favorite songs and view by date.                  |

### Notes

#### **Spotify Premium Restrictions**

- **Spotify Free Users**: Cannot play specific songs on demand; cannot use seek/rewind functions (functionality depends on Spotify's free tier limitations).

#### **Network Requirements**

- Requires a stable internet connection.
- Both Gemini API and Spotify API require network access.

### **Reflections & Future Improvements**

If starting over, I would consider:

- **Spotify Web API + Web Player**: Since Spotify is shifting focus to Web, using the Web Player SDK might be more robust.
- **Backend Architecture**: Implementing a Spring Boot backend. The mobile app should remain lightweight and not handle complex data logic locally.

<a name="japanese-version"></a>

# 日本語版

**アプリ名：** Orpheus (オルフェウス)

**Orpheus**は、AIによる感情分析とSpotifyの音楽ストリーミングを組み合わせたAndroidデモアプリです。 ユーザーはシークバーを使って、現在の5つの基本感情（喜び、悲しみ、怒り、嫌悪、恐れ）の数値を入力します。**Gemini 2.5 Flash**がその感情を分析し、最適な音楽を推奨します。

### 機能と特徴

#### **コア機能**

- **感情数値入力**：基本感情（Happy、Sad、Angry、Disgust、Fear）のシークバーを通じて、現在の気分を正確に描写します。
- **スマート音楽推奨**：Geminiによる感情分析を統合し、Spotifyのライブラリから最もマッチする楽曲を一度に最大8曲推奨します。
- **Spotify再生コントロール**：Spotify App Remote SDK（Spotify Android SDK）を統合し、再生、一時停止、前の曲、次の曲への操作をサポートします。
- **お気に入り機能**：気に入った曲をお気に入りに追加し、日付フィルタリングによる閲覧が可能です。

#### プレーヤー機能

- **プレーヤー画面**：アルバムアート、曲名、アーティスト名、プログレスバーを表示します。
- **ミニプレーヤー (MiniPlayer)**：他の画面にいても操作可能なミニプレーヤーを表示し、素早いコントロールを実現します。
- **プレイリスト管理**：推奨されたリスト全体の連続再生をサポートします。
- **進捗追跡**：再生の進捗状況と残り時間を表示します。

#### ユーザー体験 (UX)

- **Spotify OAuthログイン**：安全なサードパーティ認証ログイン。
- **ユーザー情報表示**：Spotifyのユーザー名とアイコンを表示します。
- **お気に入り管理**：日付によるフィルタリング機能をサポート。
- **多言語サポート**：繁体字中国語（台湾）、簡体字中国語（中国大陸）、英語（デフォルト）、日本語に対応しています。

### 技術アーキテクチャ

本プロジェクトは **MVVM (Model-View-ViewModel)** アーキテクチャを採用し、**Data Binding** と **View Binding** を使用して構築されています。

```
┌────────────────────────────────────────────────────────────┐
│                      View Layer                            │
│  MainActivity │ UserMainActivity │ PlayerView │ MyListView │
└──────────────────────────┬─────────────────────────────────┘
                           │ Data Binding & LiveData
┌──────────────────────────▼──────────────────────────────┐
│                      ViewModel Layer              		  │
│ LoginViewModel │ MusicViewModel │ PlayerViewModel │ ... │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────┐
│                      Model Layer                    │
│  Repository │ Room Database │ API Services │ POJO   │
└─────────────────────────────────────────────────────┘
```

#### プロジェクト構成

```
app/src/main/java/
├── Model/
│   ├── Api/                    # APIサービスインターフェース
│   │   ├── ApiClient.java      # Retrofitクライアント
│   │   ├── GeminiApiService.java
│   │   └── SpotifyApiService.java
│   ├── Cache/
│   │   └── FavoriteCache.java  # LruCache (お気に入りキャッシュ)
│   ├── Dao/
│   │   └── FavoriteDao.java    # Room DAO
│   ├── Database/
│   │   └── AppDatabase.java    # Room データベース
│   ├── Entity/
│   │   └── FavoriteEntity.java # Room Entity
│   ├── POJO/                   # データモデル
│   │   ├── MusicItem.java
│   │   ├── EmotionInput.java
│   │   ├── PlaylistData.java
│   │   └── ...
│   ├── Repository/
│   │   ├── MusicRepository.java
│   │   └── FavoriteRepository.java
│   └── Spotify/
│       └── SpotifyPlayerManager.java  # Spotify SDK マネージャー
├── View/
│   ├── MainActivity.java       # ログイン画面
│   ├── UserMainActivity.java   # メイン画面（感情入力 + 推奨リスト）
│   ├── PlayerView.java         # プレーヤー画面
│   ├── MyListView.java         # お気に入りリスト画面
│   ├── UserSettingView.java    # ユーザー設定画面
│   └── adapter/
│       └── MusicListAdapter.java
├── ViewModel/
│   ├── LoginViewModel.java
│   ├── MusicViewModel.java
│   ├── PlayerViewModel.java
│   ├── MiniPlayerViewModel.java
│   ├── MyListViewModel.java
│   └── UserMainViewModel.java
└── Util/
    ├── TokenManager.java       # トークン管理
    ├── BindingAdapters.java    # Data Binding アダプター
    └── SliderBindingAdapters.java # Sliderデータの双方向バインディング処理
```

### 技術スタック

| カテゴリ           | 技術                                                         |
| ------------------ | ------------------------------------------------------------ |
| **言語**           | Java 21                                                      |
| **アーキテクチャ** | MVVM + Repository Pattern                                    |
| **UI**             | Material Design 3, Data Binding, ViewBinding                 |
| **非同期処理**     | LiveData, CompletableFuture                                  |
| **ネットワーク**   | Retrofit2, OkHttp3                                           |
| **ローカル保存**   | Room Database, SharedPreferences                             |
| **キャッシュ**     | LruCache                                                     |
| **画像読み込み**   | Glide                                                        |
| **認証**           | AppAuth (OAuth 2.0)                                          |
| **外部依存**       | Spotify App Remote SDK (Android SDK), Google Gemini API, Spotify Web API |

**使用しているAPI**

- **Spotify Web API**: ユーザー認証と認可 (OAuth 2.0)、楽曲検索、ユーザー情報取得。
- **Spotify App Remote SDK (Android SDK)**: 音楽再生コントロール（再生、一時停止、スキップ）、再生状態の監視、進捗追跡。
- **Google Gemini API**: 感情分析、感情に基づく音楽推奨の生成。

### 環境設定

#### 前提条件

- Android Studio Ladybug またはそれ以降のバージョン
- JDK 21
- Android SDK 36
- Spotifyアプリ（テスト端末にインストール済みであること）
- Spotify Developer アカウント
- Google AI Studio アカウント

#### 設定手順

1. **APIキーの設定** プロジェクトのルートディレクトリに `local.properties` を作成または編集します：

   ```
   # Spotify API
   SPOTIFY_CLIENT_ID=your_spotify_client_id
   SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
   
   # Gemini API
   GEMINI_API_KEY=your_gemini_api_key
   ```

2. **Spotify Developer Console の設定**

   - [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) にアクセス
   - アプリケーションを作成
   - Redirect URI を設定
   - Client ID と Client Secret を取得

3. **Spotify SDK のダウンロード**

   - [Spotify Android SDK](https://github.com/spotify/android-sdk/releases) からダウンロード
   - `spotify-app-remote-release-x.x.x.aar` を `app/libs/` ディレクトリに配置

### アプリケーションフロー

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  ログイン画面 │────▶│    メイン画面     │────▶│  プレーヤー画面  │
│ (Spotify    │     │ (感情入力 +       │     │ (完全な操作系)   │
│  OAuth)     │     │  推奨リスト)      │     │                 │
└─────────────┘     └──────────────────┘     └─────────────────┘
                            │
                            ▼
                    ┌──────────────────┐
                    │ お気に入りリスト   │
                    │  (日付フィルタ)    │
                    └──────────────────┘
```

### 主な機能説明

| 機能           | 説明                                       |
| -------------- | ------------------------------------------ |
| **ログイン**   | Spotify OAuthを通じた安全なログイン。      |
| **感情入力**   | 5次元のスライダーで感情数値(0-100)を調整。 |
| **直接入力**   | 現在の気分をテキストで記述。               |
| **推奨リスト** | AIが推奨した8曲を表示。                    |
| **プレーヤー** | 完全な再生コントロールと進捗表示。         |
| **MiniPlayer** | クイックコントロール用のミニプレーヤー。   |
| **お気に入り** | 気に入った曲を保存し、日付ごとに閲覧可能。 |

### 注意事項

#### **Spotify Premium の制限**

- **Spotify Free ユーザー**：特定の楽曲をオンデマンドで再生できない、シークバーによる早送り/巻き戻しができない等の制限があります（Spotifyの仕様に準じます）。

#### **ネットワーク要件**

- 安定したインターネット接続が必要です。
- Gemini API および Spotify API の利用にはネットワークアクセスが必須です。

### **改善案 / 今後の展望**

もしプロジェクトを最初から作り直すなら、以下を検討します：

- **Spotify Web API + Web Player**：SpotifyがWebへの注力を強めているため、Android SDKよりもWeb Playerの使用を検討します。
- **バックエンド構築**：Spring Bootなどでバックエンドを構築し、モバイルアプリ側を軽量化します。複雑なデータロジックはアプリ内で処理させない設計にします。
