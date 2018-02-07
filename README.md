# wikitolearn-pwa-gateway

## Synopsis
WikiToLearn PWA Gateway is an API gateway for the WikiToLearn Progressive Web App.
Its aim is to provide a unique entrypoint for the PWA rerounting requests to mid-tier services.
WikiToLearn PWA Gateway performs also authorization checks before to forward requests.

## Development
We use Docker to speed-up development and setup the environment without any dependency issues.

### Minimum requirements
- Docker Engine 17.09.0+

### How to run
It is advisable to run using the `docker-compose.yml` file provided.
First, you have replace your Docker Host IP into the `.env` file.
Second, you need to run the following services on which WikiToLearn PWA Gateway strongly depends on:
- Keycloak

## Versioning
We use [SemVer](http://semver.org/) for versioning.

## License
This project is licensed under the AGPLv3+. See the [LICENSE.md](LICENSE.md) file for details.
