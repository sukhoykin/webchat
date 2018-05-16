# (DRAFT)

## Development
### Requirements
* JDK
* NPM

```
git clone
npm install
bower install
grunt
```

## Protocol specification

### Overview

Communication protocol include three security and functionality tiers. Each tier has own TLS-encryption and nested in previous:

* Authentication protocol encrypted within [secured](https://en.wikipedia.org/wiki/Transport_Layer_Security) WebSocket [HTTPS](https://en.wikipedia.org/wiki/HTTPS) connection.
* Authorization and delivery protocol encrypted with hybrid client-server cipher suite (ECDHE-ECDSA-AES). 
* Messaging protocol encrypted with hybrid client-client cipher suite (ECDHE-EdDSA-AES). 

### Authentication

**Client**

* Generate ECDH key pair `DHpriv` and `DHpub` with Curve25519 curve for key exchange. 
* Generate ECDSA key pair `DSApriv` and `DSApub` with Curve25519 curve for data signature.
* Initiate secured WebSocket connection (wss) to the server.
* Send **identify command** with `email` address:

```javascript
{
  command: 'identity',
  email: '{email}'
}
```

**Server**

* Generate secure random `R` and associate with client connection.
* Calculate a time-based one-time password `TOTP` that valid for 5 minutes:

```
TOTP = HMAC-MD5(R, now() / 60 / 5)
```

* Send email with `TOTP` to client `email` address.

**Client**

* Obtain `TOTP` from email.
* Calculate signature `Skey` for public keys `DHpub` and `DSApub` using HMAC algorithm and `TOTP` as secret:

```
Skey = HMAC-SHA-256(TOTP, DHpub || DSApub)
```

* Send **authenticate command** with public keys and signature:

```javascript
{
  command: 'authenticate',
  dh: '{DHpub}',
  dsa: '{DSApub}',
  signature: '{Skey}'
}
```

**Server**

* Calculate `TOTP` from `R`.
* Calculate `Skey` for received client public keys.
* Verify signature. If signature is not valid, close session with code `401`.
* If server have `active session` with this `email` then close `active session` with code `101`.
* Generate ECDH key pair `DHpriv` and `DHpub` with Curve25519 curve for key exchange. 
* Generate ECDSA key pair `DSApriv` and `DSApub` with Curve25519 curve for data signature.
* Calculate signature `Skey` for server public keys.
* Send **authenticate command** to client:

```javascript
{
  command: 'authenticate',
  dh: '{DHpub}',
  dsa: '{DSApub}',
  signature: '{Skey}'
}
```

* Create new session for this `email` and associate connection, client/server keys and `TOTP` as cipher initialization vector `IV` with the session.

**Client**

* Calculate `Skey` for received server public keys.
* Verify signature. If signature is not valid, close session with code `401`.

### Client-Server TLS

**Client and server**

* Derive shared secret `D` using own private `DHpriv` key and remote public `DHpub` key:

```
D = DHpriv.own x DHpub.remote
```

* Calculate shared key `K` using shared secret `D`, server public `DHpub.server` and client public `DHpub.client` keys:

```
K = SHA-256(D || DHpub.server || DHpub.client)
```

* Create separate AES cipher engines in CBC-mode for `send` end `receive` data using shared key `K` and `TOTP` as initialization vector `IV`.
* All other commands should be sent encrypted as **data command** payload.

**Send data**

* Create command and encrypt it as `payload`.
* Calculate signature `Spayload` for `payload` using `DSApriv`:

```
Spayload = DSApriv.Signature(payload)
```

* Send **data command**:

```javascript
{
  command: 'data',
  payload: '{payload}',
  signature: '{Spayload}'
}
```

**Receive data**

* Verify signature `Spayload` for `payload` using remote `DSApub`:

```
DSApub.Verify(payload, Spayload)
```

* If signature is not valid then close session with code `401`.
* Decrypt `payload` as command and process it.

### Authorization

**Client**

* Send **authorize command** with recipient `email` address:

```javascript
{
  command: 'authorize',
  email: '{email}'
}
```

**Server**

* Check recipient authenticated on server.

### Client-Client TLS

### Delivery

### Messaging
