# iu1920

Código para las prácticas de Interfaces de Usuario en la FdI UCM, curso 2019-20

Para lanzar el servidor en local,
* instala [Java 8](https://openjdk.java.net/install/) ó superior
* instala [Maven](https://maven.apache.org/download.cgi)
* clona este repositorio
* desde el directorio `server`, ejecuta `mvn spring-boot:run`
* la URL de acceso será `http://localhost:8080/'. La BD y el servidor estarán sólo en memoria, de forma que si cierras la aplicación se perderán también los datos guardados en ella.
* la BD estará inicialmente vacía. Para poder hacer login, entra en [http://localhost:8080/api/initialize](http://localhost:8080/api/initialize) - y apunta bien lo que diga, porque sólo se puede inicializar la BD cuando está vacía, e initialize crea varias instancias independientes y un administrador para cada una.

Durante las prácticas, habrá una versión de este servidor ejecutándose sobre una BD persistente en [http://gin.fdi.ucm.es:8080](http://gin.fdi.ucm.es:8080). Pregunta al profesor por tu login y contraseña.



