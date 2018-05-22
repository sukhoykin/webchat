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

# Protocol specification

Communication protocol include three security and functionality tiers. Each tier has own TLS-encryption and nested in previous:

* Authentication protocol encrypted within [secured](https://en.wikipedia.org/wiki/Transport_Layer_Security) WebSocket [HTTPS](https://en.wikipedia.org/wiki/HTTPS) connection.
* Authorization and delivery protocol encrypted with hybrid client-server cipher suite (ECDHE-ECDSA-AES). 
* Messaging protocol encrypted with hybrid client-client cipher suite (ECDHE-EdDSA-AES). 

## Authentication

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

* Derive key `D` using own private `DHpriv` key and remote public `DHpub` key:

```
D = DHpriv.own x DHpub.remote
```

* Calculate shared secret `K` hashing derived key `D`, server public `DHpub.server` and client public `DHpub.client` keys:

```
K = SHA-256(D || DHpub.server || DHpub.client)
```

* Create separate `AES` cipher engines in `CBC` mode with `PKCS7` padding for `encrypt` end `decrypt` data using shared secret `K` and `TOTP` as initialization vector `IV`.
* All other commands should be sent encrypted as **envelope command** `payload`.

**Send TLS-envelope**

* Create command and encrypt it as `payload` using `encrypt` cipher.
* Calculate signature `Spayload` for `payload` using `DSApriv`:

```
Spayload = DSApriv.Signature(payload)
```

* Send **envelope command**:

```javascript
{
  command: 'envelope',
  payload: '{payload}',
  signature: '{Spayload}'
}
```

**Receive TLS-envelope**

* Verify signature `Spayload` for `payload` using remote `DSApub`:

```
DSApub.Verify(payload, Spayload)
```

* If signature is not valid then close session with code `401`.
* Decrypt `payload` using `decrypt` cipher and process command.

## Authorization

**Client**

* To initiate authorization send **authorize command** with recipient `email` address:

```javascript
{
  command: 'authorize',
  email: '{email}'
}
```

**Server**

* If recipient authenticated on server then send him **authorize command** with originator `email` and keys:

```javascript
{
  command: 'authorize',
  email: '{email}',
  dh: '{DHpub}',
  dsa: '{DSApub}'
}
```

**Client**

* Receive **authorize command** with originator `email` address and keys.
* To accept authorization send **authorize command** with originator `email`.

**Server**

* Send **authorize command** with receiver `email` and keys to originator.
* Add originator and receiver to authorization table.

### Client-Client TLS

**Both clients**

* Derive key `D` using own private `DHpriv` key and remote public `DHpub` key:

```
D = DHpriv.own x DHpub.remote
```

* Calculate shared secret `K` hashing derived key `D`, authorization originator public `DHpub.origin` and authorization recipient public `DHpub.recipient` keys:

```
K = SHA-256(D || DHpub.origin || DHpub.recipient)
```

* Derive initialization vector `IV` using own private `DSApriv` key and remote public `DSApub` key:

```
IV = truncate(DSApriv.own x DSApub.remote)
```

* Create separate `AES` cipher engines in `CBC` mode with `PKCS7` padding for `encrypt` end `decrypt` messages using shared secret `K` and initialization vector `IV`.

## Prohibition

**Client**

* Send **prohibit command** with recipient `email` address:

```javascript
{
  command: 'prohibit',
  email: '{email}'
}
```

**Server**

* Send **prohibit command** to recipient with originator `email`:

```javascript
{
  command: 'prohibit',
  email: '{email}'
}
```

* Remove originator and recipient from authorization table.

## Messaging

**Client**

* Encrypt `message` data with `encrypt` cipher and calculate signature `Smessage`.
* Send **message command** with recipient `email` address, encrypted message and signature:

```javascript
{
  command: 'message',
  email: '{email}',
  message: '{message}',
  signature: '{Smessage}'
}
```

**Server**

* If recipient authenticated and authorized then route him **message command** with originator `email`:

```javascript
{
  command: 'message',
  email: '{email}',
  message: '{message}',
  signature: '{Smessage}'
}
```
