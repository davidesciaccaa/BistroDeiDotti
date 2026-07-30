# Il Bistrò dei Dotti

Applicazione full-stack per il sito ristorante de Il Bistrò dei Dotti, composta da:

- `frontend`: React + Vite
- `backend`: Spring Boot 3 + Java 21

La homepage React consuma le API Spring Boot per stato servizio e sezioni del menu. In sviluppo Vite usa un proxy verso il backend; il backend espone comunque CORS configurabile tramite variabili d'ambiente.

## Prerequisiti

- Node.js 20.19+ oppure 22.12+
- Java 21
- Maven 3.9+
- Docker e Docker Compose, opzionali

## Avvio in sviluppo

Terminale 1:

```bash
cd backend
mvn spring-boot:run
```

Terminale 2:

```bash
cd frontend
npm install
npm run dev
```

URL principali:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`
- API stato: `http://localhost:8080/api/status`
- API menu: `http://localhost:8080/api/menu/sections`
- Pannello admin: `http://localhost:5173/admin` (vedi [Pannello admin](#pannello-admin-e-persistenza-del-menu))

## Configurazione CORS

In sviluppo il backend accetta di default:

- `http://localhost:5173`
- `http://127.0.0.1:5173`

Per cambiare gli origin consentiti:

```bash
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://example.com mvn spring-boot:run
```

Nel frontend puoi sovrascrivere la base URL delle API copiando `frontend/.env.example` in `frontend/.env`:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

Senza `.env`, il frontend usa `/api`; in sviluppo Vite fa proxy verso `http://localhost:8080`.

## Pannello admin e persistenza del menu

Il proprietario crea, modifica, sposta ed elimina i piatti da `http://localhost:5173/admin`.
Il prezzo viene inviato alle API come numero; il simbolo `€` è aggiunto soltanto dall'interfaccia.

Il pannello è **disabilitato finché non imposti `ADMIN_PASSWORD`**: senza quella variabile ogni endpoint `/api/admin/**` risponde `503`.

```bash
cd backend
ADMIN_PASSWORD='una-password-lunga' mvn spring-boot:run
```

Variabili disponibili:

| Variabile | Default | Significato |
| --- | --- | --- |
| `ADMIN_PASSWORD` | *(vuota)* | Password del pannello. Vuota = admin disattivato. |
| `ADMIN_SESSION_TTL` | `8h` | Durata del token di sessione. |
| `MENU_DATA_DIRECTORY` | `data` | Directory runtime per menu e backup. |
| `MENU_LEGACY_OVERRIDES_FILE` | `data/menu-overrides.json` | File del vecchio formato, migrato al primo avvio se presente. |

Come funziona:

- `src/main/resources/menu.default.json` è il seed versionato e non viene mai modificato a runtime.
- Al primo avvio il backend crea `data/menu.json`; ai riavvii successivi il file esistente non viene sovrascritto.
- Tutte le modifiche vengono validate e salvate con file temporaneo e sostituzione atomica.
- Il backend crea backup giornalieri in `data/backups/daily` (30 giorni) e mensili in `data/backups/monthly` (12 mesi), usando il fuso `Europe/Rome`.
- Il controllo dei backup avviene all'avvio e ogni giorno alle 03:15 tramite lo scheduler interno di Spring.
- Il login (`POST /api/admin/login`) restituisce un token opaco tenuto **solo in memoria**: si perde a ogni riavvio del backend. Il browser lo tiene in `sessionStorage`, quindi la sessione muore chiudendo il browser.
- Il fallback `fallbackMenuSections` in `App.jsx` resta invariato e serve solo se il backend è irraggiungibile.

Con Docker, passa la password al compose (per esempio con un file `.env` accanto a `compose.yaml`):

```bash
ADMIN_PASSWORD='una-password-lunga' docker compose up --build
```

Il volume `menu-data` conserva `menu.json` e i backup tra un riavvio e l'altro dei container.

## Avvio con Docker

```bash
docker compose up --build
```

Con Docker:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

## Comandi utili

Frontend:

```bash
cd frontend
npm run lint
npm run build
npm run preview
```

Backend:

```bash
cd backend
mvn test
mvn package
```

## Struttura

```text
.
├── backend
│   ├── src/main/java/com/angolodivino
│   │   ├── admin
│   │   ├── config
│   │   ├── menu
│   │   └── status
│   └── src/main/resources
├── frontend
│   ├── src/admin
│   ├── src/api
│   ├── src/assets
│   └── src/components
└── compose.yaml
```
