<!-- Improved compatibility of back to top link: See: https://github.com/othneildrew/Best-README-Template/pull/73 -->
<a id="readme-top"></a>
<!--
*** Thanks for checking out the Best-README-Template. If you have a suggestion
*** that would make this better, please fork the repo and create a pull request
*** or simply open an issue with the tag "enhancement".
*** Don't forget to give the project a star!
*** Thanks again! Now go create something AMAZING! :D
-->



<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![project_license][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]



<!-- PROJECT TITLE -->
<br />
<div align="center">

<h3 align="center">Transaction System</h3>

  <p align="center">
    <br />
    <a href="https://github.com/oscar-gom/transaction-system"><strong>View Documentation »</strong></a>
    <br />
    <br />
    <a href="https://github.com/oscar-gom/transaction-system/issues/new?labels=bug&template=bug-report---.md">Report Bug</a>
    &middot;
    <a href="https://github.com/oscar-gom/transaction-system/issues/new?labels=enhancement&template=feature-request---.md">Request Feature</a>
  </p>
</div>



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
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project

A secure banking transaction processing and management system built with JWT authentication, comprehensive data validation, and guaranteed ACID operations. The system includes the following features:

- Secure JWT authentication
- Transaction and account management
- Automatic welcome bonus for new accounts
- Data persistence with PostgreSQL
- Automated database migrations with Flyway
- Unit and integration tests
- Automatic API documentation with Swagger/OpenAPI

<p align="right">(<a href="#readme-top">back to top</a>)</p>



### Built With

* [![Java][Java.com]][Java-url]
* [![Spring Boot][SpringBoot.io]][SpringBoot-url]
* [![PostgreSQL][PostgreSQL.org]][PostgreSQL-url]
* [![Maven][Maven.com]][Maven-url]
* [![JWT][JWT.io]][JWT-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- GETTING STARTED -->
## Getting Started

Follow these steps to set up the project locally.

### Prerequisites

You need to have the following installed:

* Java 25 or higher
  ```sh
  java --version
  ```
* Maven
  ```sh
  mvn --version
  ```
* Docker and Docker Compose (for the database)
  ```sh
  docker --version
  docker-compose --version
  ```

### Installation

1. **Clone the repository**
   ```sh
   git clone https://github.com/oscar-gom/transaction-system.git
   cd transaction-system
   ```

2. **Configure environment variables**
    
   > For a complete list of all available environment variables, refer to the `.env.example` file included in the project root.
   

3. **Start the database with Docker Compose**
   ```sh
   docker-compose up -d
   ```

4. **Compile the project**
   ```sh
   mvn clean compile
   ```

5. **Run database migrations**
   ```sh
   mvn flyway:migrate
   ```

6. **Run the application**
   ```sh
   mvn spring-boot:run
   ```

   The application will be available at `http://localhost:8080`

   Swagger documentation will be available at `http://localhost:8080/swagger-ui.html`

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- USAGE EXAMPLES -->
## Usage

### Running Tests

```sh
# Run all tests
mvn test

# Run tests for a specific class
mvn test -Dtest=AccountServiceTest

# Generate test coverage report
mvn test jacoco:report
```

### Main Endpoints

For a complete list of all available endpoints and their documentation, please visit the OpenAPI/Swagger documentation:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/v3/api-docs.yaml`

The API documentation is accessible **without authentication** and provides interactive testing capabilities.

Example authentication request:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "password"}'
```

### Project Structure

```
src/main/java/com/oscargsedas/transactionsystem/
├── controller/      # REST Controllers
├── dto/            # Data Transfer Objects
├── entity/         # JPA Entities
├── exception/      # Custom Exceptions
├── repository/     # Data Access Layer
├── security/       # JWT and Security
├── service/        # Business Logic
└── util/           # Utilities
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- ROADMAP -->
## Roadmap

- [x] Basic transaction system
- [x] JWT authentication
- [x] Data validation
- [X] Make the repository public
- [ ] CLI interface for direct system interaction
- [ ] Rate Limiting to prevent spam attacks
- [ ] Compensatory transactions for managing refunds or failures
- [ ] Administrator system
- [ ] Contact directory/address book
- [ ] Robust server-side security validations for registration

See [open issues](https://github.com/oscar-gom/transaction-system/issues) for a complete list of proposed features.

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- LICENSE -->
## License

See `LICENSE.txt` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- CONTACT -->
## Contact

- Email: oscargsedas@gmail.com
- LinkedIn: [LinkedIn](https://www.linkedin.com/in/oscar-gomez-sedas/)
- GitHub: [GitHub](https://github.com/oscar-gom)

Project Link: [https://github.com/oscar-gom/transaction-system](https://github.com/oscar-gom/transaction-system)

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/oscar-gom/transaction-system.svg?style=for-the-badge
[contributors-url]: https://github.com/oscar-gom/transaction-system/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/oscar-gom/transaction-system.svg?style=for-the-badge
[forks-url]: https://github.com/oscar-gom/transaction-system/network/members
[stars-shield]: https://img.shields.io/github/stars/oscar-gom/transaction-system.svg?style=for-the-badge
[stars-url]: https://github.com/oscar-gom/transaction-system/stargazers
[issues-shield]: https://img.shields.io/github/issues/oscar-gom/transaction-system.svg?style=for-the-badge
[issues-url]: https://github.com/oscar-gom/transaction-system/issues
[license-shield]: https://img.shields.io/github/license/oscar-gom/transaction-system.svg?style=for-the-badge
[license-url]: https://github.com/oscar-gom/transaction-system/blob/main/LICENSE
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://www.linkedin.com/in/oscar-gomez-sedas/

<!-- Tech Badges -->
[Java.com]: https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://www.java.com/
[SpringBoot.io]: https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white
[SpringBoot-url]: https://spring.io/projects/spring-boot
[PostgreSQL.org]: https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white
[PostgreSQL-url]: https://www.postgresql.org/
[Maven.com]: https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white
[Maven-url]: https://maven.apache.org/
[JWT.io]: https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JWT&logoColor=white
[JWT-url]: https://jwt.io/ 
