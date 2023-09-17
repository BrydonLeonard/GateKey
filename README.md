<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->

## About The Project

The complex I live in has a keypad at the gate where visitors dial my unit number, which initiates a call to my
cellphone. If I want to let them in, I dial 9 and the gate opens. The system works well enough, but can be a pain when
my wife's at home and needs to open the gate for a delivery, so this repository contains a project that

1. Provisions single-use keycodes for the gate
2. Integrates with Telegram for management of those keys and user accounts
3. Exposes a webhook for telephone calls to a Twilio-managed telephone number
4. Uses DTMF tones to allow visitors to key in their keycodes at the gate
5. Automatically responds with a 9 if the key is valid
6. Notifies all users via telegram whenever the gate is opened

Thereby taking myself out of the gate-opening loop!

### Built With

![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white)
![SQLite](https://img.shields.io/badge/sqlite-%2307405e.svg?style=for-the-badge&logo=sqlite&logoColor=white)
![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)
![Twilio](https://img.shields.io/badge/Twilio-F22F46?style=for-the-badge&logo=Twilio&logoColor=white)

## Getting Started

### Prerequisites

#### SSL cert

The system's Twilio integration operates via a webhook that receives requests over HTTPS, so we need a cert to secure
those connections.
If you purchased your domain through [CloudFlare](https://www.cloudflare.com/products/registrar/), you get certificates
for free (and can
use [my Dynamic DNS script to automatically update your CloudFlare domain record when your IP address changes](https://github.com/BrydonLeonard/CloudFlareDDNS)).

Your cert will come with a `.crt` and `.key` files, but GateKey uses PKCS12 cert bundles, so create one with

```bash
openssl pkcs12 -export -out your_cert_bundle_name -inkey your_key_file.key -in your_cert_file.crt
```

You'll use that cert later when starting up the server.

#### Telegram bot token

You'll need a telegram bot token for the key registration portion of the service to work. You receive one of these when
registering a bot with the BotFather. See the
[BotFather to HelloWorld tutorial](https://core.telegram.org/bots/tutorial) for more info.

#### Twilio account and number

This isn't required to start up the server, but the service is a bit useless without it. Purchase a telephone number
from twilio and set up the voice service to point to `${your_domain}/voice`.

### Installation

#### Packaging and deployment

Run `gradle build` to generate the application tarball in `build/distributions`. Copy it to wherever you want to run it
from
and extract the archive.

#### Starting up

Set these environment variables and run `bin/GateKey`:

- `GATE_KEY_ALLOWED_CALLERS` - A comma-separated list of telephone numbers from which the system will accept calls. The
  number
  used by the gate should be one of them.
- `GATE_KEY_DB_PATH` the path at which the SQLite DB will be saved. This is optional, but not setting it will mean that
  the DB is created _inside_ the docker container and disappears with every deployment.
- `GATE_KEY_TELEGRAM_BOT_TOKEN` - The token that's used to assume the role of the Telegram bot.
- `GATE_KEY_CERT_PATH` - The path of the PKCS12 cert bundle for the server.
- `GATE_KEY_CERT_PASSWORD` - The password for the PKCS12 cert bundle.

## Usage

TODO: Some usage examples here

## License

Distributed under the MIT License. See `LICENSE` for more information.

<!-- ACKNOWLEDGMENTS -->

## Acknowledgments

Use this space to list resources you find helpful and would like to give credit to. I've included a few of my favorites
to kick things off!

* [Best-README-Template](https://github.com/othneildrew/Best-README-Template/blob/master/README.md)
