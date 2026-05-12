# HTTP Requests

Ready-to-run HTTP requests for the API. Compatible with:

- **IntelliJ IDEA / WebStorm HTTP Client** (built in)
- **VS Code REST Client** extension (`humao.rest-client`)

## Layout

| File                 | Purpose                                                  |
|----------------------|----------------------------------------------------------|
| `http-client.env.json` | Environment variables (`host`, `productId`)            |
| `products.http`      | Happy-path CRUD against `/api/v1/products`               |
| `validation.http`    | Bad-request / not-found cases (expect 400 / 404)         |
| `actuator.http`      | Health, info, and metrics endpoints                      |

## Usage

### IntelliJ
Open any `.http` file and click the green ▶ next to a request. Pick the `local` environment from the dropdown to resolve `{{host}}` and `{{productId}}`.

### VS Code (REST Client)
Open the file and click **Send Request** above each block. To resolve variables, add this to your `settings.json`:

```json
"rest-client.environmentVariables": {
  "local": {
    "host": "http://localhost:8080",
    "productId": "1"
  }
}
```

## Demonstrating the cache

In `products.http`, fire the two consecutive `GET /api/v1/products/{{productId}}` requests after a `POST`:

1. First `GET` — logs `Fetching product from database...` (Postgres hit, populates Redis).
2. Second `GET` — silent in the logs (served from Redis).

A `PUT` refreshes the cached entry (`@CachePut`); a `DELETE` evicts it (`@CacheEvict`).
