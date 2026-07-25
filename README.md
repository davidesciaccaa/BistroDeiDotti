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
- Pannello admin: `http://localhost:5173/admin` (vedi [Pannello admin](#pannello-admin-gestione-prezzi))

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

## Pannello admin (gestione prezzi)

Il proprietario modifica i prezzi del menù da `http://localhost:5173/admin`. È una pagina separata, non raggiungibile dai link pubblici del sito.

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
| `MENU_OVERRIDES_FILE` | `data/menu-overrides.json` | File JSON con i prezzi modificati. |

Come funziona:

- I prezzi modificati finiscono in `menu-overrides.json`, letto a ogni richiesta del menù: niente database e nessun riavvio dopo un salvataggio.
- Se il file non esiste (o è illeggibile) valgono i prezzi hardcodati in `MenuService.java`. Rimettere un prezzo al valore originale ne rimuove la voce dal file.
- Il login (`POST /api/admin/login`) restituisce un token opaco tenuto **solo in memoria**: si perde a ogni riavvio del backend. Il browser lo tiene in `sessionStorage`, quindi la sessione muore chiudendo il browser.
- Il fallback `fallbackMenuSections` in `App.jsx` resta invariato e serve solo se il backend è irraggiungibile.

Con Docker, passa la password al compose (per esempio con un file `.env` accanto a `compose.yaml`):

```bash
ADMIN_PASSWORD='una-password-lunga' docker compose up --build
```

Il volume `menu-data` conserva `menu-overrides.json` tra un riavvio e l'altro dei container.

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
