# Guía detallada del proyecto TP_PrograB

Esta guía explica cómo está organizado el proyecto, qué función cumple cada carpeta y clase, cómo fluye la información y qué parte de la consigna cubre cada componente.

## 1. Idea general del sistema

El proyecto representa una Copa Internacional de Clubes. Actualmente puede:

1. Leer los equipos, jugadores, directores técnicos y árbitros desde `torneo.json`.
2. Convertir esos datos en objetos Java.
3. Validar que existan 16 equipos y que cada plantel tenga la composición exigida.
4. Sortear cuatro zonas equilibradas según el ranking.
5. Crear los 24 partidos de la fase de grupos.
6. Asignar un árbitro válido a cada partido.
7. Seleccionar las formaciones iniciales.
8. Simular los resultados con factores deportivos y aleatorios.
9. Registrar goles, tarjetas, expulsiones y cambios.
10. Mostrar cada partido y sus incidentes por terminal.

Todavía no calcula las tablas de posiciones ni ejecuta cuartos, semifinales y final.

## 2. Recorrido completo de una ejecución

El punto de entrada funcional de la simulación es:

```text
src/main/java/Core/Run/Championship.java
```

Su método `main` ejecuta este recorrido:

```text
torneo.json
    ↓
TournamentLoader
    ↓
DTO del paquete Core.dto
    ↓
Objetos Team, Player, Coach y Referee
    ↓
TournamentValidator
    ↓
Sorteo equilibrado de zonas
    ↓
Generación de 24 GroupMatch
    ↓
Asignación de árbitros
    ↓
MatchSimulator
    ↓
Incidencias guardadas dentro de cada Match
    ↓
MatchConsoleReporter
    ↓
Salida por terminal
```

El código principal es:

```java
Championship championship = new Championship("torneo.json");
MatchConsoleReporter reporter = new MatchConsoleReporter();
championship.simulateGroupStage(reporter::printMatch);
```

La primera línea construye el campeonato. La segunda crea el objeto encargado de mostrar los partidos. La tercera simula cada encuentro y, después de cada simulación, lo imprime.

## 3. Archivos ubicados en la raíz

### `torneo.json`

Ubicación:

```text
TP_PrograB/torneo.json
```

Contiene la carga inicial proporcionada para el campeonato:

- equipos;
- país y ranking de cada equipo;
- planteles;
- características y estadísticas previas de los jugadores;
- directores técnicos;
- árbitros.

Con respecto a la consigna, cubre el requerimiento no funcional que exige cargar equipos, integrantes y árbitros desde JSON, XML, CSV u otro archivo.

Este JSON no guarda todavía el estado del campeonato. Por eso no cumple aún la persistencia necesaria para cerrar el programa y continuar posteriormente.

### `pom.xml`

Ubicación:

```text
TP_PrograB/pom.xml
```

Es la configuración de Maven. Declara:

- Java como lenguaje;
- Gson para leer JSON;
- JavaFX Controls;
- JavaFX FXML;
- `GUI.AppLauncher` como entrada de la interfaz.

No contiene lógica del torneo. Su función es configurar cómo se compila y ejecuta el proyecto y qué bibliotecas externas necesita.

### `lib/gson-2.11.0.jar`

Es una copia local de Gson. Gson convierte el texto JSON en objetos Java. Maven también declara Gson como dependencia, por lo que en el futuro convendría elegir una sola estrategia para evitar versiones duplicadas.

## 4. Carpeta `Core/domain`: modelo del problema

Ubicación:

```text
src/main/java/Core/domain
```

Esta carpeta contiene las entidades principales del campeonato. Representa el dominio: las cosas que existen dentro del problema real.

### `Person.java`

Es una clase abstracta con los datos comunes de todas las personas:

```java
private int id;
private String name;
private String lastName;
private String documentType;
private int documentNumber;
private LocalDate birthDate;
```

Es abstracta porque el sistema no necesita crear una persona sin rol. Cada persona será un jugador, entrenador o árbitro.

Aplica herencia de esta forma:

```text
Person
├── Player
├── Coach
└── Referee
```

El constructor valida nombre, apellido, documento y fecha de nacimiento. Esto cubre parcialmente la indicación de lanzar excepciones desde el dominio para desacoplar las validaciones de la interfaz.

Con respecto a la consigna, representa los datos personales comunes de jugadores, DT y árbitros.

### `Country.java`

Representa un país únicamente mediante su nombre. Al leer el JSON, el sistema construye un catálogo sin duplicados y hace que `Team`, `Coach`, `Referee` y `City` referencien objetos de ese catálogo en lugar de guardar nombres independientes como texto.

`equals()` y `hashCode()` comparan el nombre sin distinguir mayúsculas y minúsculas. Así, `Argentina` y `ARGENTINA` se consideran el mismo país.

### `Player.java`

Representa un jugador. Agrega a los datos de `Person`:

- número de camiseta;
- posición;
- estado de suspensión;
- mapa de características;
- mapa de estadísticas provenientes del JSON.

`getAverageRating()` obtiene el promedio de las características:

```java
return characteristics.values()
        .stream()
        .mapToInt(Integer::intValue)
        .average()
        .orElse(0);
```

Este promedio se utiliza para calcular la fuerza del equipo durante la simulación.

`serveSuspensionIfNeeded()` permite cumplir una fecha de suspensión. Si el jugador estaba suspendido, devuelve `true`, lo excluye del próximo encuentro y luego vuelve a habilitarlo.

Con respecto a la consigna, cubre posición, valoración y suspensión por expulsión. Las estadísticas específicas del campeonato —minutos, partidos y goles de este torneo— todavía no están implementadas como un modelo separado.

### `Coach.java`

Representa al director técnico. Agrega:

```java
private String nationality;
private int titlesWon;
```

Los títulos intervienen en la fuerza competitiva del equipo. Cubre los datos de nacionalidad y títulos solicitados.

### `Referee.java`

Representa un árbitro. Agrega:

```java
private String nationality;
private int refereeYears;
```

La nacionalidad se usa al asignarlo a un partido. Los años todavía no afectan la simulación ni se utilizan en el ranking de árbitros.

### `Team.java`

Representa un equipo y contiene:

- identificador;
- nombre;
- país;
- ranking;
- director técnico;
- lista de jugadores.

`addPlayer()` impide agregar valores nulos o superar los 18 jugadores.

`getAveragePlayerRating()` promedia la valoración de todo el plantel.

`getCompetitiveStrength()` combina tres componentes:

```java
return rankingScore * 0.35
        + getAveragePlayerRating() * 0.55
        + coachScore * 0.10;
```

Esto significa:

- 35 % depende del ranking;
- 55 % depende de la calidad de los jugadores;
- 10 % depende de los títulos del DT.

La lista de jugadores se entrega como una lista no modificable:

```java
return Collections.unmodifiableList(players);
```

Esto protege la colección y es una aplicación de encapsulamiento.

Con respecto a la consigna, cubre los datos básicos del equipo y aporta factores para la simulación. Todavía no almacena puntos, partidos jugados, goles a favor o goles en contra.

### `TournamentZone.java`

Representa una zona de grupos. Tiene un nombre y hasta cuatro equipos.

```java
public void addTeam(Team team) {
    if (teams.size() < 4) {
        teams.add(team);
    } else {
        throw new IllegalStateException(...);
    }
}
```

Cubre la división del torneo en cuatro zonas de cuatro equipos. Todavía no contiene una tabla de posiciones.

### `Match.java`

Es la clase abstracta común a todos los partidos. Guarda:

- fecha;
- local y visitante;
- árbitro;
- goles de ambos equipos;
- incidencias;
- formación inicial de ambos equipos;
- indicador `played`.

`setInitialLineups()` valida que cada formación tenga exactamente 11 jugadores.

La jerarquía es:

```text
Match
├── GroupMatch
├── FirstLegMatch
├── SecondLegMatch
└── FinalMatch
```

Esto aplica herencia. Sin embargo, el polimorfismo todavía es parcial porque las subclases no redefinen un método de simulación o determinación de ganador.

Con respecto a la consigna, cubre fecha, equipos, árbitro, resultado, formaciones e incidencias. Todavía no contiene estadio y no guarda explícitamente el criterio que determinó al ganador.

### `GroupMatch.java`

Representa un partido de grupos. Actualmente no agrega datos o comportamiento a `Match`; permite distinguir conceptualmente el tipo de encuentro.

Es el único tipo de partido que se genera y simula en el flujo actual.

### `FirstLegMatch.java`

Representa un partido de ida de cuartos o semifinal. Existe como estructura, pero todavía no se utiliza.

### `SecondLegMatch.java`

Representa un partido de vuelta. Guarda goles del partido de ida y si la serie se resolvió por penales.

Es una implementación parcial: todavía no calcula puntos acumulados, diferencia, goles de visitante ni tandas de penales.

### `FinalMatch.java`

Representa la final a partido único y permite marcar si terminó por penales. Todavía no se genera ni se simula.

### `City.java` y `Stadium.java`

Representan ciudades y estadios. Ahora una ciudad referencia un `Country` del catálogo del campeonato, y un estadio contiene una referencia a una ciudad:

```java
private City city;
```

Esto refleja la relación solicitada, pero solo a nivel de objetos. Faltan:

- base de datos relacional;
- alta, baja y modificación;
- restricción para no eliminar ciudades con estadios;
- selección aleatoria de estadios;
- impedir reutilizarlos en eliminatorias.

## 5. Carpeta `Core/enums`

### `Position.java`

Ubicación:

```text
src/main/java/Core/enums/Position.java
```

Define las únicas posiciones aceptadas:

```java
GOALKEEPER,
DEFENDER,
MIDFIELDER,
FORWARD
```

Usar un `enum` evita valores inválidos escritos libremente y facilita seleccionar formaciones y validar planteles.

## 6. Carpeta `Core/dto`: lectura del JSON

Ubicación:

```text
src/main/java/Core/dto
```

DTO significa Data Transfer Object. Estas clases representan exactamente la forma del JSON, pero no contienen reglas del campeonato.

La estructura es:

```text
TournamentRootDto
└── TournamentDto
    ├── TeamsDto
    │   └── TeamDto
    │       └── SquadDto
    │           ├── PlayersDto
    │           │   └── PlayerDto
    │           │       └── PersonDto
    │           └── CoachDto
    │               └── PersonDto
    └── RefereesDto
        └── RefereeDto
            └── PersonDto
```

`@SerializedName` conecta el nombre español del JSON con el atributo inglés de Java:

```java
@SerializedName("posicion")
public String position;
```

`CountryDto` recibe los países que en el JSON aparecen como textos simples, por ejemplo `"pais": "Argentina"`. Un adaptador de Gson realiza esa conversión. Las clases DTO cumplen una función técnica para la carga exigida por la consigna. No deberían contener simulación, validaciones complejas o cálculos deportivos.

`PersonaDto.java` parece una versión antigua de `PersonDto.java` y actualmente no se utiliza. Puede eliminarse en una limpieza futura después de comprobar referencias.

## 7. Carpeta `Core/loader`: carga y validación

### `TournamentLoader.java`

Ubicación:

```text
src/main/java/Core/loader/TournamentLoader.java
```

Lee el archivo:

```java
String json = Files.readString(Path.of(filePath));
TournamentRootDto root = GSON.fromJson(json, TournamentRootDto.class);
```

Después convierte los DTO en objetos del dominio. Este paso es importante porque el resto del programa trabaja con `Team`, `Player` y `Referee`, no directamente con estructuras JSON.

También convierte posiciones en español al `enum` inglés:

```java
case "arquero" -> Position.GOALKEEPER;
case "defensor" -> Position.DEFENDER;
case "mediocampista" -> Position.MIDFIELDER;
case "delantero" -> Position.FORWARD;
```

Finalmente llama a:

```java
TournamentValidator.requireValid(data);
```

Por eso un archivo estructuralmente incorrecto no llega a utilizarse para crear el campeonato.

### `TournamentData.java`

Es un contenedor del resultado de la carga. Mantiene las listas de equipos y árbitros y devuelve vistas no modificables.

### `TournamentValidator.java`

Comprueba:

- exactamente 16 equipos;
- al menos un árbitro;
- exactamente 18 jugadores por equipo;
- 2 arqueros;
- 6 defensores;
- 5 mediocampistas;
- 5 delanteros.

Si existen errores, `requireValid()` lanza una excepción con todos ellos.

`findDuplicateDocuments()` detecta documentos compartidos por varias personas, pero actualmente no se invoca automáticamente ni bloquea la carga. Se dejó separado porque el JSON original contiene numerosos documentos repetidos y no corresponde inventar reemplazos.

Esto cubre parcialmente las validaciones y consistencia requeridas por la consigna.

## 8. Carpeta `Core/Run`: coordinación del sistema

### `Main.java`

Es una utilidad sencilla para probar la carga. Lee el JSON y muestra cuántos equipos y árbitros se cargaron.

Los métodos que imprimen todos los equipos y árbitros están comentados en el `main`:

```java
// printTeams(tournament);
// printReferees(tournament);
```

No inicia la simulación. Sirve para diagnóstico de datos.

### `Championship.java`

Es el coordinador principal del torneo. Sus responsabilidades actuales son:

1. Cargar datos.
2. Mantener equipos y árbitros.
3. Sortear las zonas.
4. Generar partidos.
5. Elegir árbitros.
6. Pedir al simulador que juegue los encuentros.

#### Constructores y aleatoriedad

```java
public Championship(String jsonPath)
public Championship(String jsonPath, long seed)
```

El primero usa aleatoriedad diferente en cada ejecución. El segundo recibe una semilla, útil en pruebas:

```java
new Championship("torneo.json", 2026L);
```

Con la misma semilla se repiten el sorteo y los resultados, permitiendo reproducir fallos.

#### Sorteo equilibrado

`drawBalancedZones()` ordena los equipos por ranking y crea cuatro bombos:

```text
Bombo 1: posiciones 1 a 4
Bombo 2: posiciones 5 a 8
Bombo 3: posiciones 9 a 12
Bombo 4: posiciones 13 a 16
```

Cada bombo se mezcla con `Collections.shuffle()` y entrega un equipo a cada zona. De esa manera todas las zonas reciben uno de cada nivel.

Esto cumple el sorteo aleatorio equilibrado solicitado.

#### Generación de partidos

`generateGroupMatches()` combina cada equipo con los otros tres de su zona. Cuatro equipos producen seis combinaciones. Cuatro zonas producen:

```text
6 × 4 = 24 partidos
```

Actualmente agrega un día a la fecha por cada partido, por lo que los 24 encuentros se programan en días consecutivos. Es una decisión provisional; la consigna solo exige registrar la fecha.

#### Selección de árbitros

`isRefereeEligible()` aplica la regla de nacionalidad.

Si los equipos tienen países distintos, el árbitro no puede compartir nacionalidad con ninguno. Si ambos equipos tienen el mismo país, se permite cualquier nacionalidad, tal como indica la excepción de la consigna.

Después se selecciona aleatoriamente un árbitro entre los habilitados.

#### Simulación con observador

```java
public void simulateGroupStage(Consumer<Match> afterEachMatch)
```

Simula cada encuentro y luego comunica el partido terminado al objeto recibido. En el `main`, ese objeto es el presentador de terminal:

```java
championship.simulateGroupStage(reporter::printMatch);
```

Esto mantiene separada la lógica deportiva de la forma de mostrarla.

## 9. Carpeta `Core/simulation`

### `MatchSimulator.java`

Es responsable de resolver un partido. No imprime y no crea zonas; solamente modifica el `Match` recibido.

### Paso 1: evitar doble simulación

```java
if (match.isPlayed()) {
    throw new IllegalStateException(...);
}
```

### Paso 2: seleccionar formaciones

Ordena los jugadores disponibles por valoración y selecciona un esquema 4-3-3:

- 1 arquero;
- 4 defensores;
- 3 mediocampistas;
- 3 delanteros.

Los jugadores suspendidos cumplen la fecha y no ingresan en la formación.

### Paso 3: calcular fuerza

Obtiene la fuerza competitiva de cada equipo y suma una pequeña ventaja al local:

```java
double homeStrength = home.getCompetitiveStrength() + 3.0;
double awayStrength = away.getCompetitiveStrength();
```

### Paso 4: generar sorpresa

```java
double surprise = random.nextGaussian() * 12.0;
```

Este componente evita resultados determinísticos. Un equipo inferior puede ganar si el valor aleatorio lo favorece.

### Paso 5: generar goles

`sampleGoals()` utiliza una distribución semejante a Poisson. Recibe una expectativa de gol calculada desde las fuerzas, la limita y produce un número aleatorio.

Se limita a un máximo práctico de seis goles por equipo.

### Paso 6: registrar goles

Por cada gol crea un objeto `Goal` con:

- minuto;
- autor;
- arquero que lo recibió;
- indicador de penal;
- indicador de gol en contra.

La probabilidad actual es aproximadamente 12 % para penal y 3 % para gol en contra cuando no fue penal.

### Paso 7: tarjetas y expulsiones

Genera entre cero y tres tarjetas amarillas por equipo. Cada equipo tiene 10 % de probabilidad de sufrir una expulsión.

El expulsado queda suspendido para el siguiente partido de su equipo.

### Paso 8: cambios

Intenta generar tres cambios por equipo entre los minutos 55 y 90. Cada jugador sale una sola vez y el suplente tiene la misma posición que el reemplazado.

### Paso 9: finalizar

```java
match.setPlayed(true);
```

Importante: el simulador no avanza realmente segundo a segundo. Primero calcula el resultado y genera los eventos; después el presentador los ordena por minuto. La terminal muestra una línea de tiempo, pero no es una animación en tiempo real.

## 10. Carpeta `Core/incidents`

### `Incident.java`

Es la clase abstracta base y contiene el minuto. Aplica herencia:

```text
Incident
├── Goal
├── YellowCard
├── RedCard
├── Substitution
└── PenaltyShootout
```

### `Goal.java`

Guarda autor, arquero, penal y gol en contra. Cubre todos los datos de goles solicitados.

### `YellowCard.java`

Guarda el jugador amonestado. No es un requisito explícito individual de la lista de incidencias, pero será útil para el ranking de fair play.

### `RedCard.java`

Guarda el jugador expulsado. El simulador también activa su suspensión.

### `Substitution.java`

Guarda el jugador que entra y el que sale, además del minuto heredado.

### `PenaltyShootout.java`

Actualmente solo guarda un minuto y está incompleta. La consigna exige registrar cada pateador y si convirtió o falló. Todavía no participa en la simulación.

## 11. Carpeta `Core/console`

### `MatchConsoleReporter.java`

Recibe un partido ya simulado y lo muestra por terminal.

Primero copia y ordena las incidencias:

```java
List<Incident> timeline = new ArrayList<>(match.getIncidents());
timeline.sort(Comparator.comparingInt(Incident::getMinute));
```

Luego usa polimorfismo mediante `instanceof` para mostrar los datos específicos de cada incidencia:

```java
if (incident instanceof Goal goal) { ... }
if (incident instanceof YellowCard card) { ... }
if (incident instanceof RedCard card) { ... }
if (incident instanceof Substitution substitution) { ... }
```

Finalmente imprime el resultado. Esta clase no modifica el partido; solo presenta información. Eso reduce el acoplamiento entre lógica y salida.

## 12. Carpetas de interfaz JavaFX

### `GUI/AppLauncher.java`

Es el punto de entrada configurado por Maven para lanzar JavaFX. Delega en `View.main()`.

### `GUI/View.java`

Carga `view.fxml`, crea la escena y abre la ventana.

### `resources/view.fxml`

Define visualmente un panel con dos botones. Declara `Controller.controller` como controlador.

### `Controller/controller.java`

Recibe las referencias a los dos botones mediante `@FXML`, pero todavía no contiene acciones.

### `Utils/Paths.java`

Contiene una constante con la ruta del FXML. Actualmente `View` escribe `"/view.fxml"` directamente, por lo que la constante no se utiliza.

Con respecto a la consigna, JavaFX es una tecnología permitida, pero la interfaz está apenas iniciada y todavía no permite operar el campeonato. Además, el título está en español y los botones conservan textos genéricos, mientras que la consigna pide interfaces en inglés.

## 13. Carpeta de pruebas

### `src/test/java/Core/Run/ChampionshipTest.java`

Es una prueba ejecutable sin framework externo. Usa una semilla fija y verifica:

- cuatro zonas;
- cuatro equipos en cada zona;
- un equipo de cada bombo por zona;
- 24 partidos;
- árbitros válidos;
- todos los partidos jugados;
- dos formaciones de 11 jugadores;
- correspondencia entre marcador y objetos `Goal`.

Si todo funciona muestra:

```text
All championship checks passed.
```

No cubre todavía todos los casos extremos. Por ejemplo, no verifica estadísticamente la variedad de resultados ni prueba archivos JSON inválidos de manera individual.

## 14. Relación directa con la consigna

### Equipos y zonas

Implementado:

- 16 equipos;
- cuatro zonas de cuatro;
- planteles validados;
- ranking, país, jugadores y DT;
- sorteo aleatorio equilibrado;
- 24 enfrentamientos únicos.

Pendiente:

- puntos;
- tabla antes y después de cada partido;
- criterios de desempate;
- clasificación de los dos primeros.

### Eliminación directa

Parcial:

- existen clases de ida, vuelta y final.

Pendiente:

- creación de cruces;
- cuartos y semifinales;
- puntos de la serie;
- diferencia de gol;
- goles de visitante;
- penales;
- final y campeón.

### Partidos

Implementado:

- fecha;
- equipos;
- árbitro compatible;
- simulación aleatoria basada en ranking, jugadores y DT;
- formaciones;
- goles;
- cambios;
- amarillas;
- expulsiones;
- una fecha de suspensión;
- visualización por terminal.

Pendiente o parcial:

- actualización de estadísticas acumuladas;
- mostrar tabla antes y después;
- criterio explícito de ganador en eliminatorias;
- tandas de penales completas;
- asociación con estadio.

### Estadios

Solo están modeladas las clases `City` y `Stadium`. Toda la persistencia relacional y el ABM están pendientes.

### Reportes y rankings

Todavía no están implementados los carnets PDF, goleadores, fair play, minutos, cuadro de cruces, listado de equipos, árbitros ni filtros de jugadores.

### Requerimientos no funcionales

Implementado:

- carga inicial desde JSON;
- uso de listas, mapas y streams;
- algunas excepciones y validaciones;
- base JavaFX;
- nombres de clases y salida principal mayormente en inglés.

Pendiente:

- persistencia del progreso;
- base de datos relacional;
- interfaz completa en inglés;
- documentación técnica formal adicional;
- gestión ágil y presentación;
- pruebas más amplias.

## 15. Diferencia entre guardar y mostrar

Las incidencias se guardan dentro de `Match`:

```java
match.addIncident(new Goal(...));
```

Después se consultan con:

```java
match.getIncidents();
```

`MatchConsoleReporter` solo las muestra. Esta separación permite reutilizar los mismos datos en una futura pantalla JavaFX, un reporte PDF o un archivo persistido sin tener que volver a simular.

## 16. Cómo ejecutar cada parte

Para comprobar únicamente la carga, se ejecuta:

```text
Core.Run.Main
```

Para sortear, generar, simular y mostrar los 24 partidos, se ejecuta:

```text
Core.Run.Championship
```

Para realizar las verificaciones automáticas, se ejecuta:

```text
Core.Run.ChampionshipTest
```

Desde un IDE, se puede abrir la clase correspondiente y ejecutar su método `main`.

## 17. Conceptos de Programación Orientada a Objetos aplicados

### Encapsulamiento

Los atributos son privados y se accede mediante métodos. Las listas principales se devuelven como no modificables.

### Herencia

Se utiliza en personas, partidos e incidencias.

### Polimorfismo

Existe estructuralmente porque una lista de `Incident` puede contener distintas subclases y una referencia `Match` puede apuntar a `GroupMatch`. Sin embargo, falta fortalecerlo con métodos abstractos redefinidos por cada tipo de partido.

### Abstracción

`Person`, `Match` e `Incident` representan conceptos generales y son abstractas.

### Bajo acoplamiento

La carga, el dominio, la simulación y la presentación están en paquetes separados. El simulador no imprime y el presentador no calcula resultados.

## 18. Próximo paso recomendado

El siguiente bloque debería ser la tabla de posiciones:

1. Crear una clase `TeamStanding`.
2. Guardar puntos, PJ, PG, PE, PP, GF y GC.
3. Actualizarla después de cada `GroupMatch`.
4. Ordenarla por puntos, diferencia y goles a favor.
5. Resolver el resultado entre dos equipos si continúa el empate.
6. Mostrarla antes y después de cada encuentro.
7. Obtener los dos clasificados de cada zona.

Ese paso completa la fase de grupos y proporciona la información necesaria para comenzar los cuartos de final.
