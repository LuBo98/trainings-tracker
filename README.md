# Training Tracker

Webanwendung zum Verwalten und Tracken von Trainingsuebungen, erstellt mit Java Spring Boot und Thymeleaf.

## Features

- **Benutzerauthentifizierung**: Registrierung mit Username, Passwort und E-Mail-Adresse
- **Passwort-Wiederherstellung**: Reset ueber E-Mail (oder Token-Fallback wenn Mail nicht konfiguriert)
- **Kategorien**: Oberkategorien wie "Oberkoerper", "Unterkoeper", "Cardio" anlegen
- **Uebungen**: In jeder Kategorie individuelle Uebungen definieren
- **Workout-Tracking**: Sets, Wiederholungen und Gewicht pro Uebung eintragen
- **Statistik**: Uebersicht ueber beliebige Zeitaeraume (Workouts, Volumen, Durchschnitte)

## Technologie-Stack

- **Backend**: Java 21, Spring Boot 3.3.5
- **Security**: Spring Security mit BCrypt-Passwortverschlüsselung
- **Template Engine**: Thymeleaf
- **Datenbank**: PostgreSQL (Production) / H2 (Entwicklung)
- **Build**: Maven
- **Container**: Docker + Docker Compose

## Projektstruktur

```
training-tracker/
├── Dockerfile
├── docker-compose.yml          # Mit PostgreSQL
├── docker-compose-h2.yml       # Mit H2 (einfacher)
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/trainingtracker/
    │   │   ├── config/         # Spring Security Konfiguration
    │   │   ├── controller/     # REST/Web Controller
    │   │   ├── entity/         # JPA Entitaeten
    │   │   ├── repository/     # Spring Data Repositories
    │   │   └── service/        # Business-Logik
    │   └── resources/
    │       ├── application.yml
    │       ├── static/css/     # Stylesheet
    │       └── templates/      # Thymeleaf Templates
```

## Schnellstart mit Docker

### Option 1: Mit PostgreSQL (empfohlen)

```bash
docker compose up --build
```

Die Anwendung ist dann unter http://localhost:8080 erreichbar.

### Option 2: Mit H2 In-Memory (einfacher, keine Datenpersistenz)

```bash
docker compose -f docker-compose-h2.yml up --build
```

## Lokale Entwicklung

### Voraussetzungen
- Java 21+
- Maven 3.8+

### Starten

```bash
cd training-tracker
mvn spring-boot:run
```

Die Anwendung startet auf http://localhost:8080.

### Mit PostgreSQL lokal

```bash
# PostgreSQL starten
docker run -d --name postgres -p 5432:5432 \
  -e POSTGRES_DB=trainingdb \
  -e POSTGRES_USER=training \
  -e POSTGRES_PASSWORD=training123 \
  postgres:16-alpine

# Anwendung mit PostgreSQL starten
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --spring.datasource.url=jdbc:postgresql://localhost:5432/trainingdb \
  --spring.datasource.username=training \
  --spring.datasource.password=training123 \
  --spring.datasource.driver-class-name=org.postgresql.Driver"
```

## Umgebungsvariablen

| Variable | Standardwert | Beschreibung |
|---|---|---|
| DB_URL | jdbc:h2:mem:trainingdb | Datenbank-URL |
| DB_USERNAME | sa | Datenbank-Benutzer |
| DB_PASSWORD | | Datenbank-Passwort |
| DB_DRIVER | org.h2.Driver | JDBC-Treiber |
| MAIL_ENABLED | false | E-Mail fuer Passwort-Reset aktivieren |
| MAIL_HOST | localhost | SMTP-Server |
| MAIL_PORT | 587 | SMTP-Port |
| MAIL_USERNAME | | SMTP-Benutzer |
| MAIL_PASSWORD | | SMTP-Passwort |
| BASE_URL | http://localhost:8080 | Basis-URL fuer Reset-Links |

## Verwendung

1. **Registrieren**: Auf "Registrieren" klicken und ein Konto erstellen
2. **Kategorie anlegen**: z.B. "Oberkoerper", "Unterkoeper"
3. **Uebung hinzufuegen**: In jeder Kategorie Uebungen definieren (z.B. "Bankdruecken")
4. **Workout eintragen**: Sets, Wiederholungen und Gewicht eingeben
5. **Statistik ansehen**: Uebersicht ueber Trainingsfortschritt
# Training Tracker
