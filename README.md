# 🌸 Hidden-In-Pixel (HIP)

**Denial of Existence through High-Fidelity Image Steganography**

Hidden-In-Pixel is a professional-grade Java application designed to secure digital communication by concealing secret data within the latent redundancies of `.png` images. Unlike standard encryption, HIP prioritizes "Denial of Existence," ensuring that the very fact of a message's existence remains undetectable to forensic observers.

## ✨ Project Highlights
- **Novelty:** Implements four distinct concealment layers, from standard bit-substitution to randomized stochastic selection.
- **Forensic Validation:** Built-in statistical analysis tools, including Red-channel Histograms and scaled Difference Maps, to prove visual integrity.
- **Architectural Rigor:** Strictly follows **SOLID principles** and **OOPS design patterns** (Strategy, Factory, and Singleton).
- **Fancy UI:** A custom JavaFX dashboard featuring a "Girly Dark" theme—sophisticated, high-contrast, and elegant.

## 🛠 Tech Stack
- **Language:** Java 17+
- **GUI:** JavaFX (CSS3 Styled)
- **Metadata Handling:** Apache Commons Imaging
- **Database:** JDBC (H2/MySQL) for Stego-Analytics
- **Build Tool:** Maven/Gradle

## 🧪 The 4 Algorithms
1.  **Sequential LSB:** Linear bit-by-bit embedding for high-capacity needs.
2.  **XOR-LSB (Secure):** Bitwise XOR operation using a PBKDF2-derived key before embedding.
3.  **Stochastic LSB (Randomized):** Password-seeded PRNG pixel selection to resist pattern-based steganalysis.
4.  **Metadata Channel:** Invisible text-chunk insertion (EXIF/tEXt) that leaves pixel data 100% untouched.

## 🎨 Visual Identity
The interface is designed with a "Fancy & Girly" dark aesthetic, utilizing a specific palette:
- 🌌 **Background:** Deep Teal (#09BBC9) / Dark Charcoal
- 💜 **Accents:** Soft Lavender (#C5B5E2)
- 🍑 **Highlights:** Peach Apricot (#FDC89C)
- 🎀 **Interactive:** Rose Pink (#FEAFC5) & Salmon Coral (#FD8BAD)

## 📁 System Architecture
HIP utilizes a decoupled structure to ensure maintainability:
- `core/`: bit manipulation and steganographic logic.
- `ui/`: JavaFX controllers and CSS styling.
- `db/`: JDBC implementation for tracking `change_ratio` and `payload_density`.
- `exception/`: Custom error handling (e.g., `PayloadOverflowException`, `InvalidStegoKeyException`).

## 🚀 How to Run

Follow these steps to build and launch the application:

### Prerequisites
- **Java 17 or higher** (JDK 17+)
- **Maven 3.6+**

### Steps
1.  **Clone the repository** (or navigate to the project directory).
2.  **Compile the project**:
    ```bash
    mvn clean compile
    ```
3.  **Run the application**:
    ```bash
    mvn javafx:run
    ```

### Optional: Create a Runnable JAR
To package the application into a standalone JAR file:
```bash
mvn clean package
```
*The JAR will be available in the `target/` directory.*

---
*Developed for technical excellence and aesthetic elegance.*
