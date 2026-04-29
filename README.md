# 🌸 LUMINAE
---


**Denial of Existence through High-Fidelity Image Steganography**

Luminae is a professional-grade Java application that secures digital communication by concealing secret data within PNG images. Choose from 4 steganographic algorithms, analyze embeddings in real-time, and maintain a persistent history of operations.

---

## ✨ Project Highlights

| Feature | Benefit |
|---------|----------|
| 🎯 **4 Algorithms** | Sequential LSB, XOR-LSB, Stochastic LSB, Metadata Channel |
| 📊 **Real-time Analytics** | Change ratio, payload density, and visual difference maps |
| 🏗️ **SOLID Architecture** | Single Responsibility, Separation of Concerns, Testability |
| 🎨 **Premium UI** | JavaFX with Girly Dark theme (Teal, Lavender, Peach palette) |
| 🗄️ **Persistent Storage** | JDBC + H2 Database for analytics history |

---

## 🛠 Tech Stack

```
┌─────────────────────────────┐
│   JavaFX UI (Controllers)   │
├─────────────────────────────┤
│ Business Logic (Services)   │
├─────────────────────────────┤
│ Steganography Core Layer    │
├─────────────────────────────┤
│ Database Layer (JDBC/H2)    │
└─────────────────────────────┘
```

- **Language:** Java 17+
- **GUI:** JavaFX with CSS3 Styling
- **Database:** JDBC + H2 (Embeddable, no server needed)
- **Build:** Maven 3.6+

---

## 🧪 The 4 Algorithms

| Algorithm | Method | Security | Capacity | Use Case |
|-----------|--------|----------|----------|----------|
| **Sequential LSB** | Linear bit replacement | Low | High (732 KB in 2MP) | Quick embedding |
| **XOR-LSB** | PBKDF2 encrypted bits | Medium | High | Password-protected |
| **Stochastic LSB** | Random pixel selection | High | High | Resist steganalysis |
| **Metadata Channel** | PNG tEXt chunks | Medium | Medium | Zero pixel change |

### Algorithm Comparison
```
Speed:      Sequential > XOR > Stochastic > Metadata
Security:   Metadata > Stochastic > XOR > Sequential
Detectability: Sequential ❌❌ | XOR ❌ | Stochastic ✓ | Metadata ✓✓
```

---

## 🏗️ System Architecture

### Layered Design Flowchart

```
USER INTERFACE LAYER
        ↓
┌─────────────────────────────────────┐
│  MainController | AnalyticsController│
│  (FXML Bindings, Event Handling)    │
└──────────────┬──────────────────────┘
               ↓
SERVICE LAYER
┌──────────────────────────────────────┐
│ ImageProcessor │ AnalyticsService    │
│ BitManipulator │ SecurityProvider    │
└──────────────┬──────────────────────┘
               ↓
CORE LAYER (Strategy Pattern)
┌──────────────────────────────────────┐
│ StegoAlgorithm (Interface)           │
│  ├─ SequentialLSB                    │
│  ├─ XorLSB                           │
│  ├─ StochasticLSB                    │
│  └─ MetadataChannel                  │
└──────────────┬──────────────────────┘
               ↓
PERSISTENCE LAYER
┌──────────────────────────────────────┐
│ AnalyticsDAO | DatabaseConnection    │
│ (Singleton Pattern, JDBC)            │
└──────────────────────────────────────┘
```

### Module Responsibilities

```
src/main/java/com/hiddeninpixel/
├── core/              → 4 Algorithm implementations
├── service/           → Image processing, bit manipulation, analytics
├── db/                → JDBC connection & data persistence
├── model/             → AnalyticsRecord POJO
├── exception/         → Custom error handling
└── ui/                
    ├── controllers/   → MainController, AnalyticsController
    └── components/   → HistogramChart, DifferenceMapGenerator
```

---

## 🎯 OOPs Design Patterns

| Pattern | Implementation | Purpose |
|---------|---|----------|
| **Strategy** | `StegoAlgorithm` interface + 4 implementations | Runtime algorithm selection without tight coupling |
| **Singleton** | `DatabaseConnection.getInstance()` | Single DB connection instance across app |
| **Factory** | Algorithm instantiation in MainController | Centralized object creation |
| **DAO** | `AnalyticsDAO` | Decouple business logic from database |
| **MVC** | Controllers + FXML Views + Models | Separation of concerns |

---

## ✅ SOLID Principles

| Principle | Implementation |
|-----------|---|
| **S**ingle Responsibility | `SequentialLSB` only embeds; `ImageProcessor` only handles I/O |
| **O**pen/Closed | Add new algorithms by implementing `StegoAlgorithm` interface |
| **L**iskov Substitution | All algorithms substitute interchangeably |
| **I**nterface Segregation | Focused interfaces (embed/extract) |
| **D**ependency Inversion | Depend on `StegoAlgorithm` abstraction, not concrete classes |

---

## 🎨 UI Components

### Main Window
```
┌─────────────────────────────────────────┐
│           MenuBar (File, Edit, Help)    │
├─────────────────────────────────────────┤
│  Original Image  │  Stego Image         │
│  (ImageView)     │  (ImageView)         │
├─────────────────────────────────────────┤
│ Algorithm: [Dropdown]                   │
│ Message: [TextArea] | Key: [Password]  │
│ [Embed] [Extract] [Save]                │
├─────────────────────────────────────────┤
│ Change Ratio: 5% | Payload Density: 0.1│
└─────────────────────────────────────────┘
```

### Analytics Dashboard
- **Metrics Table:** Historical records with change_ratio & payload_density
- **Histogram Chart:** RGB distribution visualization
- **Difference Map:** Visual heat map of modified pixels

### Color Theme
🌌 Deep Teal (#09BBC9) | 💜 Lavender (#C5B5E2) | 🍑 Peach (#FDC89C) | 🎀 Rose Pink (#FEAFC5)

---

## 📊 Database (JDBC + H2)

```sql
CREATE TABLE analytics_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp BIGINT,
    algorithm VARCHAR(50),
    change_ratio DECIMAL(5,2),      -- % of pixels modified
    payload_density DECIMAL(10,4),  -- bits per pixel
    message_length INT,
    image_filename VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Analytics Metrics
| Metric | Calculation |
|--------|-------------|
| **Change Ratio** | (modified_pixels / total) × 100 |
| **Payload Density** | message_bits / total_pixels |
| **Max Capacity** | (pixels - header) × LSB bits |

---

## 🔐 Embedding Process

```
┌─────────────────┐
│ Load PNG Image  │
└────────┬────────┘
         ↓
┌─────────────────────────┐
│ Select Algorithm        │
└────────┬────────────────┘
         ↓
┌─────────────────────────┐
│ Enter Message & Key     │
└────────┬────────────────┘
         ↓
┌─────────────────────────┐
│ Validate Capacity       │
└────────┬────────────────┘
         ↓
┌─────────────────────────┐
│ Embed Data              │
└────────┬────────────────┘
         ↓
┌─────────────────────────┐
│ Calculate Metrics       │
└────────┬────────────────┘
         ↓
┌─────────────────────────┐
│ Save to Database        │
└────────┬────────────────┘
         ↓
┌─────────────────────────┐
│ Display Results         │
└─────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+

### Run
```bash
git clone https://github.com/sanwal-05/Luminae.git
cd Luminae
mvn clean compile
mvn javafx:run
```

### Build JAR
```bash
mvn clean package
```

---

## 📁 Project Structure
```
Hidden-In-Pixel/
├── src/main/
│   ├── java/com/hiddeninpixel/
│   │   ├── core/           (SequentialLSB, XorLSB, StochasticLSB, MetadataChannel)
│   │   ├── service/        (ImageProcessor, AnalyticsService, SecurityProvider)
│   │   ├── db/             (DatabaseConnection, AnalyticsDAO)
│   │   ├── exception/      (StegoException, InvalidKeyException, etc.)
│   │   ├── model/          (AnalyticsRecord)
│   │   └── ui/             (MainController, AnalyticsController, Components)
│   └── resources/
│       ├── views/          (main.fxml, analytics.fxml)
│       └── styles.css      (Girly Dark theme)
├── pom.xml
└── data.txt
```

---

## 📝 License

MIT License

---

*Developed for technical excellence and aesthetic elegance.* ✨
