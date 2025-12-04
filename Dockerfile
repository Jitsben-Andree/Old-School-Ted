#  CONSTRUCCIÓN (BUILD)
FROM maven:3.9.6-amazoncorretto-21 AS build

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el archivo de dependencias primero (para aprovechar la caché de Docker)
COPY pom.xml .
# Descargamos las dependencias (esto se guarda en caché si no cambias el pom.xml)
RUN mvn dependency:go-offline -B

# Copiamos el código fuente
COPY src ./src

# Compilamos el proyecto y creamos el .jar (saltando los tests para ir más rápido)
RUN mvn clean package -DskipTests

#  EJECUCIÓN (RUNTIME)
# Usamos una imagen súper ligera de Java 21 (Alpine) para correr la app
FROM amazoncorretto:21-alpine-jdk

#  Instalamos el cliente de PostgreSQL (pg_dump)
# Alpine usa 'apk' como gestor de paquetes
RUN apk add --no-cache postgresql-client

WORKDIR /app

# Copiamos solo el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Copiamos la carpeta de scripts para que Java los encuentre
COPY scripts ./scripts

# Le damos permisos de ejecución al script .sh por si acaso se perdieron al copiar
RUN chmod +x ./scripts/*.sh

# Creamos las carpetas necesarias para logs y uploads dentro del contenedor
RUN mkdir -p logs uploads

# Exponemos el puerto 8080
EXPOSE 8080
# Comando para iniciar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]