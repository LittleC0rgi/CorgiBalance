# Corgi Balance

CorgiBalance is an open-source desktop application for personal finance management.

The application helps you keep track of income and expenses, analyze spending habits, and understand where your money
goes through clear statistics and reports.

The project focuses on privacy, performance, and simplicity. All financial data is stored locally, allowing users to
manage their money without relying on online services.

## Tech Stack

- Java 25
- JavaFX 25
- Gradle
- FXML
- CSS

## Getting Started

### Requirements

Before running the project, make sure you have installed:

- JDK 25
- Git
- GNU Make (optional, but recommended)

### Clone the repository

```bash
git clone https://github.com/LittleC0rgi/CorgiBalance.git
cd CorgiBalance
```

### Run the application

Using Make:

```bash
make run
```

Or directly with Gradle:

```bash
./gradlew run --no-configuration-cache
```

## Available Commands

| Command      | Description                                         |
|--------------|-----------------------------------------------------|
| `make run`   | Run the application                                 |
| `make build` | Build the project                                   |
| `make clean` | Remove build artifacts                              |
| `make dist`  | Build a native application package using `jpackage` |

## Build Distribution

To create a native package for your operating system:

```bash
make dist
```

The generated application package will be available in the `build/` directory.

