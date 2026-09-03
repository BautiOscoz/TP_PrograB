from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, PageBreak,
    Table, TableStyle, Preformatted, KeepTogether, Flowable
)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.lib.utils import simpleSplit
from pathlib import Path
import html

ROOT = Path(r"C:\Java\TP_PrograB")
OUTPUT = ROOT / "output" / "pdf" / "guia_completa_tp_prograb.pdf"
OUTPUT.parent.mkdir(parents=True, exist_ok=True)

pdfmetrics.registerFont(TTFont("Arial", r"C:\Windows\Fonts\arial.ttf"))
pdfmetrics.registerFont(TTFont("Arial-Bold", r"C:\Windows\Fonts\arialbd.ttf"))
pdfmetrics.registerFont(TTFont("Consolas", r"C:\Windows\Fonts\consola.ttf"))

NAVY = colors.HexColor("#17324D")
BLUE = colors.HexColor("#2369A2")
LIGHT_BLUE = colors.HexColor("#EAF3FA")
GREEN = colors.HexColor("#2E7D58")
LIGHT_GREEN = colors.HexColor("#EAF5EF")
AMBER = colors.HexColor("#A65F00")
LIGHT_AMBER = colors.HexColor("#FFF4DF")
RED = colors.HexColor("#A33A3A")
LIGHT_RED = colors.HexColor("#FCECEC")
INK = colors.HexColor("#222A31")
MUTED = colors.HexColor("#5D6975")
LINE = colors.HexColor("#CBD5DE")
PAPER = colors.HexColor("#F8FAFC")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="CoverTitle", fontName="Arial-Bold", fontSize=28, leading=33,
                          textColor=colors.white, alignment=TA_LEFT, spaceAfter=18))
styles.add(ParagraphStyle(name="CoverSub", fontName="Arial", fontSize=13, leading=19,
                          textColor=colors.HexColor("#DCEAF5"), alignment=TA_LEFT))
styles.add(ParagraphStyle(name="H1x", fontName="Arial-Bold", fontSize=19, leading=24,
                          textColor=NAVY, spaceBefore=8, spaceAfter=10, keepWithNext=True))
styles.add(ParagraphStyle(name="H2x", fontName="Arial-Bold", fontSize=14, leading=18,
                          textColor=BLUE, spaceBefore=11, spaceAfter=6, keepWithNext=True))
styles.add(ParagraphStyle(name="H3x", fontName="Arial-Bold", fontSize=11, leading=14,
                          textColor=INK, spaceBefore=8, spaceAfter=4, keepWithNext=True))
styles.add(ParagraphStyle(name="BodyX", fontName="Arial", fontSize=9.4, leading=13.3,
                          textColor=INK, spaceAfter=6))
styles.add(ParagraphStyle(name="SmallX", fontName="Arial", fontSize=7.8, leading=10.5,
                          textColor=MUTED))
styles.add(ParagraphStyle(name="BulletX", fontName="Arial", fontSize=9.2, leading=13,
                          leftIndent=14, firstLineIndent=-8, bulletIndent=5, textColor=INK, spaceAfter=3))
styles.add(ParagraphStyle(name="CodeX", fontName="Consolas", fontSize=7.3, leading=9.4,
                          textColor=colors.HexColor("#26445F"), backColor=LIGHT_BLUE,
                          borderPadding=8, spaceBefore=4, spaceAfter=8))
styles.add(ParagraphStyle(name="TocHead", fontName="Arial-Bold", fontSize=11, leading=15,
                          textColor=NAVY, leftIndent=0, spaceAfter=3))
styles.add(ParagraphStyle(name="TocSub", fontName="Arial", fontSize=9, leading=12,
                          textColor=INK, leftIndent=16, spaceAfter=2))


class GuideDocTemplate(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(filename, pagesize=A4, rightMargin=1.7*cm, leftMargin=1.7*cm,
                         topMargin=1.8*cm, bottomMargin=1.65*cm,
                         title="Guía completa del proyecto TP_PrograB",
                         author="Documentación técnica del proyecto")
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="main")
        self.addPageTemplates(PageTemplate(id="content", frames=[frame], onPage=self._header_footer))
        self._bookmark_counter = 0

    def beforeDocument(self):
        # multiBuild realiza varias pasadas para resolver el indice. Los
        # identificadores deben repetirse de forma estable en cada pasada.
        self._bookmark_counter = 0

    def _header_footer(self, canvas, doc):
        if doc.page == 1:
            return
        canvas.saveState()
        canvas.setStrokeColor(LINE)
        canvas.setLineWidth(0.5)
        canvas.line(self.leftMargin, A4[1] - 1.15*cm, A4[0] - self.rightMargin, A4[1] - 1.15*cm)
        canvas.setFont("Arial", 7.5)
        canvas.setFillColor(MUTED)
        canvas.drawString(self.leftMargin, A4[1] - 0.88*cm, "TP_PrograB - Guía de diseño y funcionamiento")
        canvas.drawRightString(A4[0] - self.rightMargin, 0.85*cm, f"Página {doc.page}")
        canvas.restoreState()

    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph):
            style = flowable.style.name
            if style in ("H1x", "H2x"):
                level = 0 if style == "H1x" else 1
                text = flowable.getPlainText()
                self._bookmark_counter += 1
                key = f"heading-{self._bookmark_counter}"
                self.canv.bookmarkPage(key)
                self.canv.addOutlineEntry(text, key, level=level, closed=False)
                self.notify("TOCEntry", (level, text, self.page, key))


class CoverBlock(Flowable):
    def __init__(self, width, height=12.5*cm):
        super().__init__()
        self.width = width
        self.height = height

    def draw(self):
        c = self.canv
        c.setFillColor(NAVY)
        c.roundRect(0, 0, self.width, self.height, 14, fill=1, stroke=0)
        c.setFillColor(BLUE)
        c.rect(0, 0, 0.35*cm, self.height, fill=1, stroke=0)
        c.setFillColor(colors.white)
        c.setFont("Arial-Bold", 26)
        c.drawString(1.1*cm, self.height - 2.1*cm, "TP_PrograB")
        c.setFont("Arial-Bold", 20)
        lines = simpleSplit("Guía completa de diseño y funcionamiento", "Arial-Bold", 20, self.width - 2.2*cm)
        y = self.height - 3.2*cm
        for line in lines:
            c.drawString(1.1*cm, y, line)
            y -= 0.82*cm
        c.setFillColor(colors.HexColor("#DCEAF5"))
        c.setFont("Arial", 11)
        for line in [
            "Copa Internacional de Clubes",
            "Java 24 · POO · JSON · JDBC · PostgreSQL · JavaFX",
            "Estado documentado: septiembre de 2026",
        ]:
            c.drawString(1.1*cm, y - 0.2*cm, line)
            y -= 0.65*cm
        c.setFillColor(colors.white)
        c.setFont("Arial-Bold", 10)
        c.drawString(1.1*cm, 1.15*cm, "Objetivo: entender el proyecto de punta a punta y poder defender cada decisión.")


def p(text, style="BodyX"):
    return Paragraph(text, styles[style])


def h1(text):
    return Paragraph(text, styles["H1x"])


def h2(text):
    return Paragraph(text, styles["H2x"])


def h3(text):
    return Paragraph(text, styles["H3x"])


def bullets(items):
    return [Paragraph("- " + item, styles["BulletX"]) for item in items]


def code(text):
    return Preformatted(text.strip("\n"), styles["CodeX"], maxLineLength=100)


def status(label, title, body):
    palette = {
        "IMPLEMENTADO": (GREEN, LIGHT_GREEN),
        "PARCIAL": (AMBER, LIGHT_AMBER),
        "PENDIENTE": (RED, LIGHT_RED),
    }
    fg, bg = palette[label]
    table = Table([
        [Paragraph(f"<b>{label}</b>", ParagraphStyle("Badge", parent=styles["SmallX"], textColor=fg)),
         Paragraph(f"<b>{title}</b><br/>{body}", styles["BodyX"])],
    ], colWidths=[2.4*cm, 14.0*cm])
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, 0), bg),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("BOX", (0, 0), (-1, -1), 0.5, LINE),
        ("INNERGRID", (0, 0), (-1, -1), 0.35, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return table


def info_box(title, body, color=LIGHT_BLUE):
    t = Table([[Paragraph(f"<b>{title}</b><br/>{body}", styles["BodyX"])]], colWidths=[16.4*cm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), color),
        ("BOX", (0, 0), (-1, -1), 0.6, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 9),
        ("RIGHTPADDING", (0, 0), (-1, -1), 9),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
    ]))
    return t


def matrix(rows, widths=None):
    data = [[Paragraph(f"<b>{cell}</b>", styles["SmallX"]) for cell in rows[0]]]
    for row in rows[1:]:
        data.append([Paragraph(str(cell), styles["SmallX"]) for cell in row])
    t = Table(data, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PAPER]),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    return t


story = []
story.append(CoverBlock(16.6*cm))
story.append(Spacer(1, 0.8*cm))
story.append(p("Este documento describe el código que existe actualmente. No presenta como terminadas las funciones que solo tienen una clase preliminar. Está pensado como material de estudio, documentación técnica y apoyo para la defensa oral.", "SmallX"))
story.append(PageBreak())

story.append(h1("Contenido"))
toc = TableOfContents()
toc.levelStyles = [styles["TocHead"], styles["TocSub"]]
story.append(toc)
story.append(PageBreak())

story.append(h1("1. Qué sistema se está construyendo"))
story.append(p("El proyecto modela una Copa Internacional de Clubes. La información inicial de equipos, personas y países llega desde un archivo JSON; las ciudades y los estadios se leen desde PostgreSQL; el campeonato organiza zonas y partidos; un simulador produce resultados e incidencias; un presentador los muestra por terminal; y un repositorio binario permite guardar y recuperar el estado."))
story.append(matrix([
    ["Fuente", "Información", "Componente que la transforma"],
    ["torneo.json", "Equipos, jugadores, DT, árbitros y países", "TournamentLoader + DTO"],
    ["PostgreSQL", "Países relacionados, ciudades y estadios", "DatabaseConnection + repositories"],
    ["Memoria", "Zonas, partidos, formaciones e incidencias", "Championship + MatchSimulator"],
    ["campeonato.dat", "Estado serializado del torneo", "ChampionshipRepository"],
], [3.0*cm, 6.4*cm, 7.0*cm]))
story.append(Spacer(1, 0.3*cm))
story.append(status("IMPLEMENTADO", "Flujo de grupos", "Carga JSON, validación, sorteo equilibrado, generación de 24 partidos, árbitros válidos, formaciones, resultados e incidencias."))
story.append(Spacer(1, 0.15*cm))
story.append(status("PARCIAL", "Infraestructura", "La lectura JDBC de ciudades y estadios funciona y Championship puede almacenarlos. El ABM todavía no existe."))
story.append(Spacer(1, 0.15*cm))
story.append(status("PENDIENTE", "Competencia completa", "Faltan tabla de posiciones, clasificados, eliminatorias, penales completos, estadios asignados a partidos y reportes."))

story.append(h2("El recorrido principal"))
story.append(code("""
torneo.json
    -> Gson y DTO
    -> TournamentLoader
    -> TournamentValidator
    -> TournamentData
    -> Championship
         -> sorteo de zonas
         -> calendario de 24 partidos
         -> MatchSimulator
         -> MatchConsoleReporter
         -> ChampionshipRepository (campeonato.dat)

PostgreSQL Championship
    -> DatabaseConnection
    -> CityRepository
    -> StadiumRepository
    -> championship.loadVenues(cities, stadiums)
"""))

story.append(PageBreak())
story.append(h1("2. Organización de carpetas"))
story.append(matrix([
    ["Carpeta", "Responsabilidad"],
    ["Core/domain", "Entidades y reglas básicas del problema: personas, equipos, partidos, formaciones, ciudades y estadios."],
    ["Core/dto", "Forma intermedia que refleja la estructura del JSON."],
    ["Core/loader", "Lectura, conversión y validación de los datos iniciales."],
    ["Core/simulation", "Algoritmo que resuelve un partido y genera incidencias."],
    ["Core/incidents", "Tipos de sucesos ocurridos durante un partido."],
    ["Core/console", "Transforma un Match terminado en texto legible."],
    ["Core/persistance", "Guarda y recupera Championship mediante serialización."],
    ["Core/Run", "Puntos de entrada y coordinación del campeonato."],
    ["Infrastructure/database", "Conexión JDBC y consultas a PostgreSQL."],
    ["GUI + Controller + resources", "Inicio de interfaz JavaFX; todavía no opera el torneo."],
    ["src/test/java", "Verificaciones automáticas del flujo de grupos."],
], [5.1*cm, 11.3*cm]))
story.append(h2("Regla de dependencias"))
story.append(p("El dominio no debería depender de PostgreSQL, JavaFX ni la terminal. Una ciudad sabe qué país tiene, pero no sabe cómo fue leída. Un partido conoce sus incidencias, pero no sabe cómo se imprimen. Esta separación permite cambiar una interfaz o una base de datos sin reescribir las reglas deportivas."))
story.extend(bullets([
    "<b>Domain:</b> representa y protege el estado.",
    "<b>Loader/Repository:</b> obtiene datos externos y construye objetos.",
    "<b>Simulation:</b> aplica reglas para cambiar el estado de un partido.",
    "<b>Console/GUI:</b> presenta el estado sin decidir resultados.",
    "<b>Run:</b> conecta los componentes para ejecutar un caso de uso.",
]))

story.append(h1("3. Configuración del proyecto"))
story.append(h2("pom.xml"))
story.append(p("Maven describe el proyecto y sus bibliotecas. El código apunta a Java 24. Gson interpreta JSON, el driver PostgreSQL implementa JDBC y JavaFX aporta controles y carga FXML."))
story.append(matrix([
    ["Dependencia", "Versión", "Uso"],
    ["Gson", "2.10.1", "Deserializar torneo.json en DTO."],
    ["PostgreSQL JDBC", "42.7.8", "Abrir conexiones y ejecutar SQL."],
    ["JavaFX Controls", "21.0.2", "Botones, escenas y controles visuales."],
    ["JavaFX FXML", "21.0.2", "Construir la interfaz desde view.fxml."],
], [5.0*cm, 2.5*cm, 8.9*cm]))
story.append(info_box("Atención", "Existe también <font name='Consolas'>lib/gson-2.11.0.jar</font>. El proyecto mezcla una copia local 2.11.0 con la dependencia Maven 2.10.1. Conviene unificar la versión para evitar diferencias entre IntelliJ, terminal y Maven.", LIGHT_AMBER))

story.append(PageBreak())
story.append(h1("4. Entrada JSON y DTO"))
story.append(h2("Por qué existen DTO y dominio"))
story.append(p("El JSON usa nombres y una estructura pensados para intercambio de datos. El dominio usa objetos con comportamiento. Los DTO son el puente: Gson los completa y TournamentLoader decide cómo convertirlos."))
story.append(code("""
JSON                       DTO                         DOMINIO
"pais": "Argentina"  -> CountryDto{name}       -> Country("Argentina")
"posicion": "arquero"-> PlayerDto.position     -> Position.GOALKEEPER
"persona": {...}      -> PersonDto              -> Player / Coach / Referee
"equipo": [...]       -> List<TeamDto>          -> List<Team>
"""))
story.append(h2("Jerarquía de DTO"))
story.append(code("""
TournamentRootDto
└── TournamentDto
    ├── TeamsDto
    │   └── TeamDto
    │       └── SquadDto
    │           ├── PlayersDto -> PlayerDto -> PersonDto
    │           └── CoachDto   -> PersonDto
    └── RefereesDto -> RefereeDto -> PersonDto
"""))
story.append(h2("Adaptador de CountryDto"))
story.append(p("En el archivo, <font name='Consolas'>pais</font> es un texto, no un objeto. Sin embargo, los DTO ahora usan CountryDto. TournamentLoader registra un adaptador que envuelve ese texto en un DTO."))
story.append(code("""
private static final Gson GSON = new GsonBuilder()
    .registerTypeAdapter(
        CountryDto.class,
        (JsonDeserializer<CountryDto>)
            (json, type, context) -> new CountryDto(json.getAsString())
    )
    .create();
"""))
story.append(info_box("Idea central", "Un DTO no decide reglas deportivas. Solo transporta datos. La validación y la creación de objetos compartidos ocurren después, en TournamentLoader y TournamentValidator."))

story.append(h1("5. TournamentLoader: del archivo a los objetos"))
story.append(h2("Paso 1 - leer y deserializar"))
story.append(code("""
String json = Files.readString(Path.of(filePath));
TournamentRootDto root = GSON.fromJson(json, TournamentRootDto.class);
"""))
story.append(p("Files.readString obtiene todo el texto. Gson recorre la jerarquía de DTO basándose en los nombres de campos y en @SerializedName."))
story.append(h2("Paso 2 - catálogo único de países"))
story.append(code("""
Map<String, Country> countriesByName = new HashMap<>();

String key = name.trim().toLowerCase(Locale.ROOT);
return countriesByName.computeIfAbsent(
    key,
    ignored -> new Country(name)
);
"""))
story.append(p("El mapa evita crear varias instancias para el mismo nombre. Un equipo, un DT y un árbitro argentinos apuntan al mismo Country. La clave se normaliza para que las mayúsculas no creen duplicados."))
story.append(h2("Paso 3 - personas e IDs internos"))
story.append(p("El loader separa el identificador interno del número de documento. También divide el nombre completo del archivo, transforma la fecha con el formato dd/MM/yyyy y convierte las posiciones a Position."))
story.append(h2("Paso 4 - validación"))
story.append(code("""
TournamentData data = new TournamentData(teams, referees, countries);
TournamentValidator.requireValid(data);
return data;
"""))
story.append(p("No se entrega el objeto si el archivo incumple la estructura mínima. Así Championship nunca comienza con un plantel inválido."))

story.append(PageBreak())
story.append(h1("6. Validación de datos"))
story.append(p("TournamentValidator revisa la coherencia global después de mapear el archivo."))
story.extend(bullets([
    "El torneo debe contener exactamente 16 equipos.",
    "Debe existir al menos un árbitro.",
    "Cada equipo debe tener 18 jugadores.",
    "Cada plantel debe incluir 2 arqueros, 6 defensores, 5 mediocampistas y 5 delanteros.",
    "Los errores se acumulan y se informan juntos mediante IllegalArgumentException.",
]))
story.append(h2("Documentos repetidos"))
story.append(p("findDuplicateDocuments reúne personas por tipo y número de documento y devuelve una lista de colisiones. Es diagnóstico, no bloqueo automático, porque el archivo de origen ya contiene repeticiones. Antes de la entrega conviene decidir con la cátedra si esos números deben corregirse."))

story.append(h1("7. Modelo de personas y países"))
story.append(h2("Person"))
story.append(p("Person es abstracta: concentra id, nombre, apellido, tipo y número de documento, y fecha de nacimiento. Player, Coach y Referee heredan esos datos. El constructor rechaza nombres vacíos, documentos no válidos y fechas futuras."))
story.append(code("""
Person (abstracta, Serializable)
├── Player
├── Coach
└── Referee
"""))
story.append(h2("Country"))
story.append(p("Country posee solo name, como se decidió para el proyecto. equals y hashCode comparan sin distinguir mayúsculas. También implementa Serializable para formar parte de campeonato.dat."))
story.append(h2("Player"))
story.extend(bullets([
    "shirtNumber: número asignado por el loader.",
    "position: enum Position.",
    "characteristics: valores deportivos del JSON.",
    "statistics: estadísticas históricas del archivo, no estadísticas del campeonato actual.",
    "isSuspended: indica si debe perderse su próximo partido.",
]))
story.append(code("""
public double getAverageRating() {
    return characteristics.values().stream()
        .mapToInt(Integer::intValue)
        .average()
        .orElse(0);
}
"""))
story.append(p("serveSuspensionIfNeeded devuelve true si el jugador estaba suspendido y, a la vez, lo vuelve a habilitar. Lineup lo filtra, por lo que pierde exactamente esa convocatoria."))
story.append(h2("Coach y Referee"))
story.append(p("Coach agrega Country nationality y titlesWon. Referee agrega Country nationality y refereeYears. Los títulos influyen en la fuerza del equipo. La nacionalidad del árbitro determina si puede dirigir un encuentro."))

story.append(PageBreak())
story.append(h1("8. Team y la fuerza competitiva"))
story.append(p("Team agrega jugadores de forma controlada, impide nulos y limita el plantel a 18. getPlayers devuelve una vista no modificable para que el exterior no altere la lista sin pasar por las reglas de Team."))
story.append(h2("Cálculo actual"))
story.append(code("""
double rankingScore = Math.max(0, 101 - ranking);
double coachScore = Math.min(100, coach.getTitlesWon() * 5.0);

strength = rankingScore * 0.35
         + averagePlayerRating * 0.55
         + coachScore * 0.10;
"""))
story.append(matrix([
    ["Factor", "Peso", "Interpretación"],
    ["Ranking", "35 %", "Un ranking numéricamente menor produce más fuerza."],
    ["Plantel", "55 %", "Es el promedio de las características de los 18 jugadores."],
    ["DT", "10 %", "Cada título suma 5 hasta un máximo de 100."],
], [5.0*cm, 2.2*cm, 9.2*cm]))
story.append(info_box("Decisión de diseño", "La fórmula no pertenece a la reglamentación real: es una regla creativa del equipo. Debe documentarse y poder explicarse. El componente aleatorio del simulador evita que esta fuerza determine siempre el ganador."))

story.append(h1("9. Championship: coordinador del torneo"))
story.append(p("Championship es la raíz del estado. Conserva países, equipos, árbitros, zonas, partidos, ciudades y estadios. Implementa Serializable para que todo el grafo pueda guardarse."))
story.append(h2("Construcción"))
story.append(code("""
TournamentData tournament = TournamentLoader.load(jsonPath);
teams      = new ArrayList<>(tournament.getTeams());
referees   = new ArrayList<>(tournament.getReferees());
countries  = new ArrayList<>(tournament.getCountries());
cities     = new ArrayList<>();
stadiums   = new ArrayList<>();
random     = randomReceived;
simulator  = new MatchSimulator(random);
zones      = drawBalancedZones();
matches    = generateGroupMatches(LocalDate.now());
"""))
story.append(p("Los datos JSON se cargan de inmediato. Las ciudades y estadios comienzan vacíos porque provienen de PostgreSQL y se incorporan después con loadVenues."))
story.append(h2("Dos formas de aleatoriedad"))
story.extend(bullets([
    "<font name='Consolas'>new Championship(path)</font>: usa una semilla variable; cada ejecución puede cambiar.",
    "<font name='Consolas'>new Championship(path, 2026L)</font>: usa una semilla fija; sirve para pruebas reproducibles.",
]))

story.append(PageBreak())
story.append(h1("10. Sorteo equilibrado de zonas"))
story.append(p("Los 16 equipos se ordenan por ranking y se dividen en cuatro bombos de cuatro. Cada bombo se mezcla y entrega exactamente un equipo a cada zona."))
story.append(code("""
Equipos ordenados por ranking
├── Bombo 1: índices  0..3
├── Bombo 2: índices  4..7
├── Bombo 3: índices  8..11
└── Bombo 4: índices 12..15

Cada zona recibe: 1 del Bombo 1 + 1 del Bombo 2
                 + 1 del Bombo 3 + 1 del Bombo 4
"""))
story.append(code("""
for (int potIndex = 0; potIndex < 4; potIndex++) {
    List<Team> pot = new ArrayList<>(
        rankedTeams.subList(potIndex * 4, potIndex * 4 + 4)
    );
    Collections.shuffle(pot, random);
    for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
        zones.get(zoneIndex).addTeam(pot.get(zoneIndex));
    }
}
"""))
story.append(p("Esto cumple simultáneamente aleatoriedad y equilibrio. No pueden quedar cuatro equipos del primer bombo en una misma zona."))

story.append(h1("11. Generación de los 24 partidos"))
story.append(p("En cada zona de cuatro equipos se recorren pares sin repetir. El segundo índice comienza en homeIndex + 1, por eso no aparecen A vs A ni se duplica A vs B como B vs A."))
story.append(code("""
for (int homeIndex = 0; homeIndex < 4; homeIndex++) {
    for (int awayIndex = homeIndex + 1; awayIndex < 4; awayIndex++) {
        // crear GroupMatch
    }
}

Pares por zona: 6
Zonas: 4
Total: 6 * 4 = 24 partidos
"""))
story.append(p("Se asigna una fecha consecutiva por partido. Las formaciones se pasan inicialmente como null porque todavía no fueron elegidas: MatchSimulator las crea al comenzar la simulación."))

story.append(h1("12. Regla de árbitros"))
story.append(p("Si los clubes son de países distintos, el árbitro no puede compartir nacionalidad con ninguno. Si ambos clubes son del mismo país, se aplica la excepción y cualquier árbitro resulta elegible."))
story.append(code("""
if (home.getCountry().equals(away.getCountry())) {
    return true;
}
return !referee.getNationality().equals(home.getCountry())
    && !referee.getNationality().equals(away.getCountry());
"""))
story.append(p("chooseEligibleReferee filtra la lista, verifica que no esté vacía y elige una posición aleatoria."))

story.append(PageBreak())
story.append(h1("13. Match y sus subtipos"))
story.append(p("Match es abstracta y reúne el estado común: fecha, local, visitante, árbitro, marcador, incidencias, dos Lineup y el indicador played."))
story.append(code("""
Match (abstracta)
├── GroupMatch
├── FirstLegMatch
├── SecondLegMatch
└── FinalMatch
"""))
story.append(h2("Estado inicial y estado final"))
story.append(matrix([
    ["Momento", "homeLineup / awayLineup", "goles", "played"],
    ["Al generar calendario", "null", "0 - 0", "false"],
    ["Al iniciar simulación", "Lineup de 11", "todavía calculándose", "false"],
    ["Al finalizar", "Lineup conservada", "resultado final", "true"],
], [4.3*cm, 5.0*cm, 3.5*cm, 3.6*cm]))
story.append(h2("Subtipos eliminatorios"))
story.append(p("FirstLegMatch, SecondLegMatch y FinalMatch existen y son serializables. SecondLegMatch conserva goles de la ida; SecondLegMatch y FinalMatch tienen settledByPenalties. Aún no hay un servicio que genere cruces, calcule una serie, aplique goles de visitante o ejecute penales. Por eso estas clases son estructura parcial, no eliminatorias funcionales."))

story.append(h1("14. Lineup y FormationType"))
story.append(p("FormationType encapsula cantidades de defensores, mediocampistas y delanteros. Actualmente declara 4-3-3, 4-4-2, 3-5-2 y 4-5-1."))
story.append(h2("Selección"))
story.extend(bullets([
    "Filtra jugadores suspendidos mediante serveSuspensionIfNeeded.",
    "Ordena los disponibles por valoración promedio, de mayor a menor.",
    "Selecciona un arquero y las cantidades solicitadas por FormationType.",
    "Si una suspensión impide completar una posición, rellena con los mejores jugadores restantes.",
    "Si ni siquiera hay once disponibles, lanza IllegalStateException.",
]))
story.append(code("""
available.stream()
    .filter(player -> !lineupList.contains(player))
    .limit(11 - lineupList.size())
    .forEach(lineupList::add);
"""))
story.append(info_box("Detalle importante", "El relleno permite que el partido continúe aunque la formación solicitada no pueda cumplirse exactamente. La táctica pasa a ser una preferencia, no una garantía absoluta cuando hay suspendidos."))

story.append(PageBreak())
story.append(h1("15. MatchSimulator paso a paso"))
story.append(h2("1 - impedir doble ejecución"))
story.append(code("if (match.isPlayed()) throw new IllegalStateException(...);"))
story.append(h2("2 - decidir tácticas"))
story.append(code("""
FormationType homeTactics = FormationType.FOUR_FOUR_TWO;
FormationType awayTactics = FormationType.THREE_FIVE_TWO;
Lineup homeLineup = new Lineup(match.getHomeTeam(), homeTactics);
Lineup awayLineup = new Lineup(match.getAwayTeam(), awayTactics);
"""))
story.append(p("Actualmente las tácticas están fijas: todos los locales usan 4-4-2 y todos los visitantes 3-5-2. FormationType permite evolucionar luego a una elección por DT o aleatoria."))
story.append(h2("3 - fuerza, localía y sorpresa"))
story.append(code("""
double homeStrength = home.getCompetitiveStrength() + 3.0;
double awayStrength = away.getCompetitiveStrength();
double surprise = random.nextGaussian() * 12.0;
"""))
story.append(p("El local recibe una ventaja de 3. El valor gaussiano puede mejorar o empeorar a cualquiera y produce resultados sorpresa."))
story.append(h2("4 - cantidad de goles"))
story.append(p("sampleGoals implementa un muestreo tipo Poisson: cuanto mayor es la expectativa, más probable resulta obtener varios goles. La expectativa se limita entre 0,25 y 3,5, y el ciclo limita el resultado práctico a seis."))
story.append(code("""
homeExpected = 1.25 + (homeStrength - awayStrength + surprise) / 35.0;
awayExpected = 1.10 + (awayStrength - homeStrength - surprise) / 35.0;
"""))
story.append(h2("5 - incidencias"))
story.extend(bullets([
    "Cada gol recibe minuto aleatorio entre 1 y 90.",
    "Un gol tiene 12 % de probabilidad de ser penal.",
    "Si no es penal, tiene 3 % de probabilidad de ser en contra.",
    "Se generan de 0 a 3 amarillas por equipo.",
    "Dos amarillas al mismo jugador producen una roja un minuto después.",
    "Además existe 10 % de probabilidad de roja directa por equipo.",
    "Se intentan hasta tres sustituciones por equipo entre los minutos 55 y 90.",
]))
story.append(h2("6 - cierre"))
story.append(code("match.setPlayed(true);"))
story.append(p("El motor calcula primero todo el encuentro y luego el presentador ordena los eventos. La salida parece una línea temporal, pero no hay un reloj que avance minuto a minuto."))

story.append(PageBreak())
story.append(h1("16. Incidencias"))
story.append(code("""
Incident (abstracta: minute)
├── Goal
├── YellowCard
├── RedCard
├── Substitution
└── PenaltyShootout
"""))
story.append(matrix([
    ["Clase", "Datos", "Estado"],
    ["Goal", "Minuto, autor, arquero, penal, gol en contra", "Funcional"],
    ["YellowCard", "Minuto y jugador amonestado", "Funcional"],
    ["RedCard", "Minuto y jugador expulsado", "Funcional"],
    ["Substitution", "Minuto, jugador que entra y que sale", "Funcional"],
    ["PenaltyShootout", "Solo minuto", "Incompleta: faltan pateador y convertido/fallado"],
], [3.7*cm, 7.8*cm, 4.9*cm]))
story.append(h2("Orden y almacenamiento"))
story.append(p("Match guarda las incidencias en el orden en que el simulador las crea, no en orden cronológico. MatchConsoleReporter copia la lista y la ordena por minute sin modificar el partido."))

story.append(h1("17. Salida por terminal"))
story.append(p("MatchConsoleReporter recibe un Match ya terminado. Imprime cabecera, fecha, árbitro, eventos y resultado. El patrón instanceof recupera los datos particulares de cada subtipo."))
story.append(code("""
if (incident instanceof Goal goal) { ... }
if (incident instanceof YellowCard card) { ... }
if (incident instanceof RedCard card) { ... }
if (incident instanceof Substitution substitution) { ... }
"""))
story.append(p("Championship usa un Consumer&lt;Match&gt;. Después de simular cada partido, ejecuta afterEachMatch.accept(match). En main se pasa reporter::printMatch, una referencia al método de impresión."))
story.append(code("championship.simulateGroupStage(reporter::printMatch);"))
story.append(h2("Partidas ya terminadas"))
story.append(p("El main cuenta partidos pendientes. Si hay alguno, los simula. Si todos estaban jugados porque se recuperó campeonato.dat, imprime los resultados guardados en vez de terminar silenciosamente."))

story.append(PageBreak())
story.append(h1("18. Persistencia por serialización"))
story.append(p("Core/persistance/ChampionshipRepository guarda el objeto Championship completo en campeonato.dat mediante ObjectOutputStream y lo recupera mediante ObjectInputStream."))
story.append(code("""
try (ObjectOutputStream out =
         new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
    out.writeObject(championship);
}
"""))
story.append(h2("Por qué tantas clases implementan Serializable"))
story.append(p("Al guardar Championship, Java recorre todas sus referencias: zonas, equipos, personas, partidos, formaciones, incidencias, ciudades y estadios. Cada objeto alcanzable debe ser serializable o estar marcado transient."))
story.append(h2("Random y MatchSimulator son transient"))
story.append(p("Esos objetos se excluyen del archivo. readResolve los reconstruye después de cargar para que el campeonato recuperado pueda seguir simulando."))
story.append(code("""
@Serial
private Object readResolve() {
    this.random = new Random();
    this.matchSimulator = new MatchSimulator(this.random);
    return this;
}
"""))
story.append(h2("Decisión del main"))
story.append(p("Si existe campeonato.dat, el usuario elige continuar o crear uno nuevo. La opción nueva reemplazará el archivo al guardar. La persistencia cumple parcialmente la consigna: conserva el estado, pero el flujo actual simula toda la fase de grupos de una vez, por lo que aún no se observa una pausa natural entre partidos."))

story.append(h1("19. PostgreSQL y JDBC"))
story.append(h2("Modelo relacional observado"))
story.append(code("""
PAIS
  id BIGINT (PK)
  nombre
    │
    └── CIUDAD
          id BIGINT (PK)
          nombre
          id_pais (FK -> pais.id)
              │
              └── ESTADIO
                    id BIGINT (PK)
                    nombre
                    ciudad (FK -> ciudad.id)
"""))
story.append(p("Esta normalización evita repetir el país en cada estadio. Java conserva la misma relación: Stadium -> City -> Country."))

story.append(PageBreak())
story.append(h1("20. DatabaseConnection"))
story.append(p("La clase sigue el flujo JDBC enseñado: carga el driver, obtiene la contraseña desde una variable de entorno y abre Connection con DriverManager."))
story.append(code("""
Class.forName("org.postgresql.Driver");

String password = System.getenv("CHAMPIONSHIP_DB_PASSWORD");

return DriverManager.getConnection(
    "jdbc:postgresql://localhost:5432/Championship",
    "postgres",
    password
);
"""))
story.append(info_box("Seguridad", "La contraseña no se guarda en Git ni en el código. Cada configuración de ejecución de IntelliJ que use la base debe recibir CHAMPIONSHIP_DB_PASSWORD."))
story.append(h2("Responsabilidades"))
story.extend(bullets([
    "DatabaseConnection conoce driver, URL y usuario.",
    "Los repositories conocen SQL.",
    "Championship conoce City y Stadium, pero no Connection ni ResultSet.",
]))

story.append(h1("21. CityRepository"))
story.append(p("findAll recibe los países ya creados desde JSON. Ejecuta un JOIN entre ciudad y pais, luego busca el Country correspondiente por nombre y crea City."))
story.append(code("""
SELECT
    ciudad.id,
    ciudad.nombre AS ciudad,
    pais.nombre AS pais
FROM ciudad
JOIN pais ON ciudad.id_pais = pais.id
ORDER BY pais.nombre, ciudad.nombre
"""))
story.append(code("""
while (resultSet.next()) {
    long id = resultSet.getLong("id");
    String cityName = resultSet.getString("ciudad");
    String countryName = resultSet.getString("pais");
    Country country = findCountry(countries, countryName);
    cities.add(new City(id, cityName, country));
}
"""))
story.append(p("findCountry no crea un país nuevo. Si la base contiene un nombre inexistente en JSON, lanza IllegalArgumentException. Esto preserva el catálogo compartido, aunque exige que los nombres coincidan."))

story.append(PageBreak())
story.append(h1("22. StadiumRepository e integración"))
story.append(p("StadiumRepository recibe las City ya cargadas. La consulta trae ciudad.id como ciudad_id; findCity encuentra el mismo objeto y construye Stadium."))
story.append(code("""
SELECT
    estadio.id,
    estadio.nombre AS estadio,
    ciudad.id AS ciudad_id
FROM estadio
JOIN ciudad ON estadio.ciudad = ciudad.id
JOIN pais ON ciudad.id_pais = pais.id
ORDER BY pais.nombre, ciudad.nombre, estadio.nombre
"""))
story.append(h2("Carga encadenada"))
story.append(code("""
Championship championship = new Championship("torneo.json");

List<City> cities = cityRepository.findAll(
    championship.getCountries()
);

List<Stadium> stadiums = stadiumRepository.findAll(cities);

championship.loadVenues(cities, stadiums);
"""))
story.append(h2("Qué valida loadVenues"))
story.append(p("Rechaza listas nulas y verifica que cada Stadium referencie una City cuyo id exista en la lista recibida. Después copia ambas listas en Championship y expone vistas no modificables."))
story.append(status("PARCIAL", "Requisito de estadios", "Los datos relacionales se leen y quedan integrados. Aún falta asignar estadios aleatoriamente a eliminatorias, controlar que no se repitan y realizar Alta/Baja/Modificación."))

story.append(h1("23. Clases de prueba"))
story.append(matrix([
    ["Clase", "Qué comprueba"],
    ["DatabaseTest", "Abre Connection y muestra la base conectada."],
    ["CityRepositoryTest", "Carga países del JSON y ciudades de PostgreSQL."],
    ["StadiumRepositoryTest", "Carga ciudades, estadios y ejecuta loadVenues."],
    ["ChampionshipTest", "Zonas, bombos, 24 partidos, árbitros, formaciones e incidencias."],
], [5.4*cm, 11.0*cm]))
story.append(p("Los tres tests JDBC están actualmente dentro de src/main/java. Para una entrega prolija deberían moverse a src/test/java y, más adelante, convertirse a JUnit. Por ahora son ejecutables manuales útiles para aislar errores."))

story.append(PageBreak())
story.append(h1("24. JavaFX: qué existe y qué falta"))
story.append(p("AppLauncher delega en View. View carga /view.fxml, crea Scene y muestra Stage. view.fxml contiene un AnchorPane con dos botones vinculados por @FXML a Controller.controller."))
story.append(code("""
AppLauncher.main
    -> View.main
        -> Application.launch
            -> View.start(Stage)
                -> FXMLLoader.load("/view.fxml")
                -> Scene
                -> stage.show()
"""))
story.append(status("PARCIAL", "Interfaz", "La ventana puede abrirse, pero los botones no tienen acciones, no existe navegación y no se muestran zonas, partidos, tablas, ciudades ni estadios."))
story.append(info_box("Consigna de inglés", "El código está mayormente en inglés, pero el título 'Gestión de Torneo', nombres de tablas SQL en español y algunos mensajes de consola mezclan idiomas. La consigna pide código e interfaces en inglés; conviene acordar con la cátedra si la base también debe renombrarse."))

story.append(h1("25. Programación Orientada a Objetos"))
story.append(h2("Encapsulamiento"))
story.append(p("Los atributos son privados. Varias colecciones se exponen mediante Collections.unmodifiableList. Los métodos addPlayer, addTeam, setInitialLineups y loadVenues controlan cambios."))
story.append(h2("Herencia"))
story.append(p("Person, Match e Incident concentran datos comunes que reutilizan sus subclases."))
story.append(h2("Abstracción"))
story.append(p("Person, Match e Incident son conceptos incompletos por sí solos y se declaran abstractos."))
story.append(h2("Polimorfismo"))
story.append(p("Una List&lt;Incident&gt; almacena Goal, RedCard, YellowCard y Substitution; una List&lt;Match&gt; puede almacenar GroupMatch y futuros tipos eliminatorios. El polimorfismo de comportamiento todavía es débil: los subtipos de Match no sobrescriben simulate o determineWinner."))
story.append(h2("Composición"))
story.append(p("Team contiene Coach y Player; Stadium contiene City; City contiene Country; Match contiene Lineup e Incident. Estas relaciones modelan objetos formados por otros objetos."))
story.append(h2("Separación de responsabilidades"))
story.append(p("TournamentLoader carga, MatchSimulator simula, MatchConsoleReporter imprime, Championship coordina y los repositories consultan. Es una de las fortalezas actuales del diseño."))

story.append(PageBreak())
story.append(h1("26. Correspondencia con la consigna"))
story.append(matrix([
    ["Requisito", "Estado", "Implementación actual"],
    ["16 equipos y 4 zonas", "Implementado", "Validator + drawBalancedZones"],
    ["Sorteo equilibrado", "Implementado", "Cuatro bombos mezclados"],
    ["Planteles 2/6/5/5", "Implementado", "TournamentValidator"],
    ["Partidos únicos de zona", "Implementado", "24 GroupMatch"],
    ["Árbitro por nacionalidad", "Implementado", "isRefereeEligible"],
    ["Simulación multifactor y aleatoria", "Implementado", "Team strength + Gaussian + Poisson"],
    ["Formaciones", "Implementado", "Lineup + FormationType"],
    ["Goles, cambios y expulsiones", "Implementado", "Incident subclasses + simulator"],
    ["Suspensión de una fecha", "Implementado", "isSuspended + serveSuspensionIfNeeded"],
    ["Tabla antes/después", "Pendiente", "No existe TeamStanding"],
    ["Desempates y clasificados", "Pendiente", "No existe cálculo de posiciones"],
    ["Cuartos/semis/final", "Parcial", "Clases creadas; flujo no implementado"],
    ["Definición por penales", "Parcial", "Clase insuficiente; no hay algoritmo"],
    ["Estadios desde BD", "Parcial", "Lectura e integración completas"],
    ["Estadio aleatorio no repetido", "Pendiente", "Match no posee Stadium"],
    ["ABM ciudad/estadio", "Pendiente", "Solo findAll"],
    ["Persistencia del torneo", "Implementado", "Serialización campeonato.dat"],
    ["Reportes y rankings", "Pendiente", "No hay servicios de reporte"],
    ["JavaFX", "Parcial", "Ventana mínima sin operaciones"],
], [5.0*cm, 2.5*cm, 8.9*cm]))

story.append(h1("27. Riesgos y puntos a mejorar"))
story.extend(bullets([
    "La estadística del mapa Player.statistics pertenece al archivo original; no se actualiza con los partidos simulados.",
    "Las tácticas local/visitante están fijas y no dependen del DT.",
    "Los incidentes pueden ser cronológicamente incoherentes entre sí porque se generan por grupos y luego solo se ordenan para imprimir.",
    "Una roja puede aparecer antes de un gol o cambio del mismo jugador porque no existe un motor minuto a minuto.",
    "Match no guarda Stadium, por lo que todavía no puede cumplir la regla de eliminatorias.",
    "Country se enlaza entre JSON y BD por nombre; diferencias de acento o escritura provocan error.",
    "ChampionshipRepository captura errores y devuelve null; para una GUI convendría propagar una excepción clara.",
    "La carpeta se llama persistance; en inglés técnico la forma habitual es persistence.",
    "Los DTO tienen campos públicos, aceptable para transporte, pero no para entidades de dominio.",
    "La GUI todavía está desacoplada por ausencia de funcionalidad, no porque exista una capa completa de casos de uso.",
]))

story.append(PageBreak())
story.append(h1("28. Cómo ejecutar y qué esperar"))
story.append(h2("Championship"))
story.append(p("Ejecutar Core.Run.Championship. Si existe campeonato.dat, elegir 1 para continuar o 2 para crear un torneo nuevo. Un torneo nuevo simula y muestra 24 partidos. Uno ya completo vuelve a imprimir sus resultados guardados."))
story.append(h2("DatabaseTest"))
story.append(p("Ejecutar Infrastructure.database.DatabaseTest con CHAMPIONSHIP_DB_PASSWORD configurada. Debe mostrar conexión correcta y el catálogo Championship."))
story.append(h2("CityRepositoryTest y StadiumRepositoryTest"))
story.append(p("Necesitan la misma variable de entorno. El primero imprime ciudades; el segundo carga la cadena completa y muestra estadio, ciudad y país."))
story.append(h2("ChampionshipTest"))
story.append(p("Usa semilla fija, no necesita PostgreSQL y debe finalizar con 'All championship checks passed.'."))
story.append(h2("JavaFX"))
story.append(p("GUI.AppLauncher abre la ventana. No debe confundirse con Championship: ejecutar JavaFX no muestra todavía la simulación porque el controlador no la invoca."))

story.append(h1("29. Guía de lectura del código"))
story.append(p("Para comprenderlo sin saltos, estudiar en este orden:"))
story.extend(bullets([
    "1. torneo.json y los DTO: qué datos entran.",
    "2. TournamentLoader y TournamentValidator: cómo se convierten y validan.",
    "3. Country, Person, Player, Coach, Referee y Team: qué representa cada objeto.",
    "4. Championship.drawBalancedZones y generateGroupMatches: cómo nace el calendario.",
    "5. FormationType y Lineup: cómo se eligen once jugadores.",
    "6. MatchSimulator: cómo se decide el resultado.",
    "7. Incident y MatchConsoleReporter: cómo se guarda y presenta lo ocurrido.",
    "8. ChampionshipRepository: cómo se conserva el estado.",
    "9. DatabaseConnection, CityRepository y StadiumRepository: cómo entra PostgreSQL.",
    "10. GUI y Controller: punto de partida de la futura interfaz.",
]))

story.append(PageBreak())
story.append(h1("30. Próxima evolución recomendada"))
story.append(h2("Primero: tabla de posiciones"))
story.extend(bullets([
    "Crear TeamStanding con puntos, PJ, PG, PE, PP, GF y GC.",
    "Actualizarla luego de cada GroupMatch.",
    "Ordenar por puntos, diferencia, goles a favor y resultado entre ambos.",
    "Mostrar la tabla antes y después de cada partido.",
    "Obtener dos clasificados por zona.",
]))
story.append(h2("Segundo: eliminatorias y estadios"))
story.extend(bullets([
    "Agregar Stadium a Match.",
    "Mantener una colección de estadios disponibles o utilizados.",
    "Crear cruces de cuartos con el orden indicado.",
    "Resolver ida, vuelta, diferencia, visitante y penales.",
    "Generar semifinales, final y campeón.",
]))
story.append(h2("Tercero: ABM y GUI"))
story.extend(bullets([
    "Agregar insert, update y delete a CityRepository y StadiumRepository.",
    "Dejar que PostgreSQL impida borrar ciudades con estadios.",
    "Crear pantallas JavaFX para administración y torneo.",
    "Conectar botones a casos de uso, no a SQL directo.",
]))
story.append(h2("Cuarto: reportes"))
story.append(p("Una vez que el torneo acumule estadísticas confiables, construir goleadores, fair play, minutos, árbitros, cuadro de cruces e identificaciones PDF."))

story.append(h1("31. Preguntas para defender el proyecto"))
story.extend(bullets([
    "¿Por qué Country es un objeto compartido y no un String independiente?",
    "¿Por qué un DTO no debería contener reglas deportivas?",
    "¿Cómo garantiza el algoritmo un sorteo equilibrado y aleatorio?",
    "¿Por qué se usa una semilla fija en pruebas?",
    "¿Qué diferencia hay entre la estadística histórica del JSON y la del torneo actual?",
    "¿Qué significa transient y por qué se reconstruye MatchSimulator?",
    "¿Cómo evita try-with-resources conexiones abiertas?",
    "¿Por qué StadiumRepository reutiliza City en lugar de crearla otra vez?",
    "¿Qué requisito impide borrar una ciudad con estadios?",
    "¿Qué partes prueban herencia, encapsulamiento, abstracción y polimorfismo?",
]))

story.append(h1("32. Glosario breve"))
story.append(matrix([
    ["Término", "Significado dentro del proyecto"],
    ["DTO", "Objeto temporal que refleja datos externos."],
    ["Dominio", "Clases que representan las reglas y entidades del torneo."],
    ["Repository", "Clase que recupera o guarda objetos en una fuente externa."],
    ["JDBC", "API de Java para comunicarse con una base relacional."],
    ["ResultSet", "Cursor sobre filas devueltas por un SELECT."],
    ["Serialización", "Conversión de un grafo de objetos a bytes persistentes."],
    ["transient", "Campo excluido de la serialización."],
    ["Semilla", "Valor inicial que permite repetir una secuencia aleatoria."],
    ["Composición", "Un objeto contiene y utiliza otros objetos."],
    ["Integridad referencial", "Una clave foránea solo apunta a registros válidos."],
], [4.2*cm, 12.2*cm]))

story.append(Spacer(1, 0.5*cm))
story.append(info_box("Cierre", "El proyecto ya posee un núcleo valioso: carga consistente, un modelo orientado a objetos, sorteo equilibrado, simulación con incidencias, persistencia y lectura JDBC. Para considerarlo completo según la consigna, la prioridad es transformar los resultados aislados en una competencia con tabla, clasificados, eliminatorias y estadísticas acumuladas.", LIGHT_GREEN))

doc = GuideDocTemplate(str(OUTPUT))
doc.multiBuild(story)
print(OUTPUT)
