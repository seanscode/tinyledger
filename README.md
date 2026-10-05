# Tiny Ledger

REST API for managing ledgers and recording money movements.

The API supports:

* Creating a ledger with a currency
* Depositing funds
* Withdrawing funds
* Retrieving the current balance
* Retrieving transaction history

I also added these that are outside the scope as they were useful when I was playing around with it locally
* Filtering transactions by transaction type
* Paginating transaction history

## Design decisions

### Money representation

Amounts are represented as integer minor units. For example, £10.50 is represented as `1050` for currency like Yen Y1000 `1000`.

The ledger stores an ISO 4217 currency code alongside the amounts so that clients can determine how the value should be displayed.
The creation of the ledger validates the code is 3 letters but does not validate its a real valid iso code.

### Overdrafts

The ledger now returns a 442 for any transaction that would take it overdrawan

### Transactions

Each transaction has a UUID, creation timestamp, transaction type, amount and the resulting balance after the transaction.

Including `balanceAfter` means clients can display transaction history without needing to make a separate balance request for each transaction.

Transactions are returned in creation order.

### Transaction history

Transaction history supports filtering by transaction type and limiting the number of results returned. This is not really production ready and has few limits on what people can ask for but for the in memory solution this feels ok.
### Amount limits

Transaction amounts are capped at 18 digits of the minor units. This means we can transfer around most of the worlds gdp but also cant have the service killed by an unbounded input

## Implementation

The API contract was defined using OpenAPI first. The OpenAPI specification is then used by Vert.x to build and validate the HTTP routing.

The application uses Vert.x's event-loop model. For this small in-memory implementation, this keeps request processing simple and avoids the need for explicit locking around the ledger state when it is accessed from the same event-loop context.
Its not intended to support running multiple verticles and doing so would need more changes. The openapi validator was used for requests , I did not enable it for responses as it adds a lot , relative to the side of the project, code and given t
the limited scope did not make things as clear. The Error on validations was simplified down to just a general validation exception I orginally planned it to do each type but it looks like the validator still in 5.2.0 does not give a clear
indicator of the field etc. Though with more time I suspect I could find a way for that.

## How to run locally

The application runs on port `8888`.

Start the API using the Maven wrapper:

```bash
./mvnw clean compile exec:java
```

A demo client is also provided:

```bash
./mvnw -Pdemo-client compile exec:java
```

The demo client creates a ledger, performs two deposits and a withdrawal, and retrieves the resulting transaction history.

The OpenAPI specification is available in `ledger.yaml` and can be opened in tools such as IntelliJ IDEA or Postman to explore and execute the API.

## Other info

The base project was generated using [start.vertx.io](https://start.vertx.io), which provided the initial Maven configuration and empty main verticle.
IntelijPro trial I installed has some AI auto complete on, I used this when it came to boilerplate only.
