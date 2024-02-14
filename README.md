pega-proof-of-concept-frontend
================================

Frontend microservice proof of concept for future PEGA work, submits a string to pega proof of concept backend.

## Service dependencies

```sm2 --start PEGA_POC```

## Public API

| Path                                                            | Description                                                |
|-----------------------------------------------------------------|------------------------------------------------------------|
| [GET /pega-proof-of-concept-frontend/start](#get-start)         | Starts the PEGA proof of concept string submission journey |
| [GET /pega-proof-of-concept-frontend/callback/p](#get-callback) | Callback endpoint for pega proof of concept                |               

## GET /start
Journey start for the pega proof of concept string submission.

## GET /callback
Callback endpoint for pega proof of concept

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").