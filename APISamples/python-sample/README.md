# Poynt API - Python Sample
A simple Python sample demonstrating the usage of Poynt Cloud APIs.

## Installing

```
$ sudo pip install PyJWT
$ sudo pip install cryptography
$ sudo pip install requests
$ sudo pip install rsa
```

## Usage

```
./src/PoyntAPI.py
```

Note that you might need to give executable permissions to the file or you
can just run:

```
python src/PoyntAPI.py
```

NOTE: Please update the following parameters in src/PoyntAPI.py to match with yours from Poynt Developer Portal:

```
BUSINESS_ID = "<your-business-id>"
APPLICATION_ID = "<your-application-id>"
### Please make sure you update the following files with your
### own public/private keys for your Application
PRIVATE_KEY_FILE = 'keys/poynt_test_key'
PUBLIC_KEY_FILE = 'keys/poynt_test_key.pub'
```
