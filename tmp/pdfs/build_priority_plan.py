from pathlib import Path
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, KeepTogether

ROOT = Path(r'C:\Java\TP_PrograB')
OUT = ROOT / 'output/pdf/estado_y_prioridades_tp_prograb.pdf'
OUT.parent.mkdir(parents=True, exist_ok=True)
pdfmetrics.registerFont(TTFont('Arial', r'C:\Windows\Fonts\arial.ttf'))
pdfmetrics.registerFont(TTFont('ArialB', r'C:\Windows\Fonts\arialbd.ttf'))
NAVY = colors.HexColor('#18344B')
BLUE = colors.HexColor('#235F8B')
INK = colors.HexColor('#23313C')
MUTED = colors.HexColor('#586775')
styles = {
 'title': ParagraphStyle('title',fontName='ArialB',fontSize=27,leading=33,textColor=NAVY,spaceAfter=18),
 'h1': ParagraphStyle('h1',fontName='ArialB',fontSize=19,leading=24,textColor=NAVY,spaceAfter=14),
 'h2': ParagraphStyle('h2',fontName='ArialB',fontSize=12,leading=16,textColor=BLUE,spaceBefore=10,spaceAfter=5,keepWithNext=True),
 'body': ParagraphStyle('body',fontName='Arial',fontSize=10,leading=14,textColor=INK,spaceAfter=7),
 'small': ParagraphStyle('small',fontName='Arial',fontSize=8.5,leading=12,textColor=MUTED,spaceAfter=7),
 'note': ParagraphStyle('note',fontName='ArialB',fontSize=10,leading=14,textColor=NAVY,backColor=colors.HexColor('#EAF1F6'),borderPadding=10,spaceBefore=7,spaceAfter=15),
}
story=[]
def p(t,style='body'): return Paragraph(t,styles[style])
def add(t,style='body'): story.append(p(t,style))
def bullet(t): add('- '+t)
def section(title): story.append(PageBreak()); add(title,'h1')
def task(code,title,kind,priority,change,done,where):
    story.append(KeepTogether([p(f'{code}. {title}','h2'),p(f'<b>{kind} | {priority}</b>','small')]))
    add(change)
    add('<b>Se considera terminado cuando:</b> '+done,'small')
    add('<b>Área de trabajo:</b> '+where,'small')

add('TP_PrograB','title')
add('Estado del trabajo y plan de prioridades','h1')
add('Qué conservar, qué corregir y qué desarrollar para cumplir la consigna.')
add('Evaluación: 13 de septiembre de 2026. Referencia de código: commit 47e0df9, que incorpora cuartos, semifinales, final y penales.','small')
add('Este plan no modifica el proyecto. Distingue implementación encontrada por inspección de funcionalidad validada mediante pruebas. Las eliminatorias nuevas requieren pruebas específicas.','note')
add('Cómo leer las prioridades','h2')
bullet('<b>P0 - Crítica:</b> corregir primero. Protege resultados, continuidad del torneo y participación válida de jugadores.')
bullet('<b>P1 - Alta:</b> completar reglas obligatorias y verificar clasificación y ganadores antes de expandir funcionalidades.')
bullet('<b>P2 - Media:</b> incorporar estadios, ABM y estadísticas sobre una base ya confiable. Sigue siendo obligatorio para la entrega.')
bullet('<b>P3 - Integración y cierre:</b> interfaz funcional, reportes y entregables. No significa opcional; significa que depende de la lógica anterior.')
add('Orden recomendado','h2')
add('1. Continuidad del torneo guardado.<br/>2. Suspensiones y coherencia de eventos.<br/>3. Desempates, penales y criterio del ganador.<br/>4. Estadios y ABM de ciudades/estadios.<br/>5. Estadísticas confiables.<br/>6. Interfaz y reportes.<br/>7. Validación integral y entrega.')
add('Decisión acordada: JavaFX queda separado por ahora. La terminal es una herramienta temporal de prueba; no es necesario desarrollar otra presentación completa de eliminatorias si se reemplazará por JavaFX.','note')

section('1. Lo ya implementado: conservar y verificar')
add('Estas tareas no deben rehacerse. Deben conservarse y cubrirse con pruebas de regresión.','small')
for title,body in [
 ('Datos y carga','Lectura desde JSON; datos personales de jugadores, DT y árbitros; equipos con país, ranking y DT; valoración y posición; títulos del DT y años de referato.'),
 ('Planteles y sorteo','Validación de 16 equipos y planteles de 18 jugadores en distribución 2/6/5/5. Sorteo de cuatro zonas equilibradas mediante bombos de ranking.'),
 ('Fase de grupos','24 encuentros únicos, tres jornadas de ocho partidos y puntuación 3/1/0. Tabla con PTS, PJ, PG, PE, PP, GF, GC y DG.'),
 ('Simulación y alineaciones','Resultado multifactorial con aleatoriedad; árbitros elegibles por nacionalidad; elección de formaciones compatibles y conservación de titulares.'),
 ('Incidencias','Registro de goles, minuto, autor, penal/en contra y arquero; cambios y expulsiones. Existe la sanción de una fecha, pero su cumplimiento efectivo necesita la corrección C2.'),
 ('Eliminatorias','Clasificados obtenidos de las tablas; cruces de cuartos y semis según la consigna; ida y vuelta y final única. Resolución por puntos, goles visitantes dobles y penales.'),
 ('Penales y persistencia','Penales con pateador y convertido/fallado, tanda inicial y muerte súbita. Serialización de Championship y reparación de jornadas antiguas; continuidad de eliminatorias aún incompleta.'),
 ('Base de datos e interfaz','Lectura JDBC de ciudades y estadios y relación Stadium -> City -> Country. Menú JavaFX con estilos y animaciones, todavía sin integración funcional.'),
 ('Diseño orientado a objetos','Uso de encapsulamiento, herencia, colecciones polimórficas, excepciones y separación inicial entre carga, simulación, persistencia y presentación.')
]:
 add(title,'h2'); add(body)
add('Atención: la clasificación de los dos primeros está implementada, pero puede ser incorrecta cuando se llega al último desempate de grupos. Corregir C4 antes de darla por cerrada.','note')

section('2. Cambiar lo realizado: prioridad P0')
task('C1','Continuidad de eliminatorias guardadas','CORREGIR','P0 - PRIMERO',
 'El main vuelve a ejecutar cuartos, semifinales y final al cargar un torneo. Guardar fase actual, series existentes, clasificados, ganadores y campeón; simular solo lo pendiente. Guardar también al finalizar cada etapa o paso recuperable.',
 'Salir y volver a cargar no agrega encuentros duplicados, no cambia resultados ya jugados ni vuelve a elegir otro campeón. Un torneo normal contiene 37 partidos: 24 + 8 + 4 + 1.',
 'Core/Run/Championship, Core/persistance/ChampionshipRepository y estado de las series.')
task('C2','Suspensión efectiva durante todo el partido','CORREGIR','P0 - SEGUNDO',
 'Un suspendido se excluye del once, pero al limpiarse su sanción puede reaparecer en el banco construido desde el plantel completo. Preparar una lista de jugadores habilitados para el encuentro y usarla también en los cambios.',
 'Un expulsado no aparece ni de titular ni como sustituto en su siguiente partido; después vuelve a estar disponible. Las consultas de formaciones no consumen sanciones.',
 'Core/domain/Player, Lineup y Core/simulation/MatchSimulator.')
task('C3','Coherencia cronológica y jugadores en cancha','CORREGIR','P0 - TERCERO',
 'Actualmente los grupos de eventos se generan por separado. Controlar el estado de jugadores en cancha, banco y expulsados en función del minuto. No es obligatorio implementar un reloj en tiempo real: sí un estado cronológico consistente.',
 'Nadie marca después de salir o ser expulsado; los cambios solo involucran jugadores válidos y no permiten regresar a un sustituido. Las formaciones iniciales se conservan como registro histórico.',
 'MatchSimulator, Lineup y clases Incident.')
add('Pruebas P0','h2')
bullet('Guardar/cargar tras grupos, ida, vuelta, semifinal y final; verificar resultados y cantidad de encuentros.')
bullet('Forzar una expulsión y comprobar la ausencia completa del jugador en el encuentro siguiente.')
bullet('Revisar líneas temporales con expulsiones y cambios antes y después de goles.')
add('No avanzar a estadísticas de minutos ni selección de pateadores finales sin resolver C2 y C3: esas funcionalidades dependen de saber quién participó realmente.','note')

section('3. Cambiar lo realizado: prioridad P1')
task('C4','Desempate reglamentario de grupos','CORREGIR','P1 - ALTA',
 'Reemplazar el ranking como último criterio obligatorio por el resultado entre ambos equipos empatados. Mantener el orden: puntos, diferencia de gol, goles a favor, resultado entre ambos. Si queda una igualdad que la consigna no resuelve, consultar a la cátedra.',
 'Una tabla de prueba con igualdad en PTS, DG y GF respeta el encuentro directo. Los clasificados cambian solo conforme al reglamento.',
 'Championship.getStandings y pruebas de clasificación.')
task('C5','Conservar y exponer el criterio del ganador','COMPLETAR','P1 - ALTA',
 'Registrar el ganador y si avanzó por puntos, diferencia ponderada o penales; en final, por resultado de los 90 minutos o penales. Utilizar settledByPenalties y conservar el resultado de la tanda.',
 'Se puede consultar el criterio después de cargar el torneo y mostrarlo en cualquier interfaz sin recalcular ni repetir penales.',
 'SecondLegMatch, FinalMatch, resolución de series y modelo de resultado.')
task('C6','Pateadores habilitados para la tanda','CORREGIR','P1 - ALTA | DEPENDE DE C2/C3',
 'La tanda toma el once inicial. Usar participantes habilitados al finalizar el encuentro, considerando sustituciones y expulsiones. No es un error por sí mismo que un arquero patee: lo importante es la elegibilidad.',
 'No patea ningún expulsado ni sustituido; se conserva cada ejecución con pateador y convertido/fallado.',
 'determinePenaltyShootoutWinner, simulatePenalty y PenaltyShootout.')
task('C7','Pruebas de reglas eliminatorias','DESARROLLAR PRUEBAS','P1 - ALTA',
 'Agregar pruebas de puntos en ida/vuelta, diferencia con visitante doble, penales, final empatada, cruces exactos y persistencia sin duplicación. Las pruebas se desarrollan junto con C1-C6, no solo al final.',
 'Casos controlados y reproducibles verifican cada camino de decisión y no requieren una interfaz.',
 'src/test/java.')
add('Regla ya correcta: no sustituir los puntos y goles visitantes dobles por reglas de fútbol actuales. El reglamento particular del TP exige precisamente esos criterios.','note')

section('4. Desarrollar: estadios y ABM - P2')
task('N1','Asociar y sortear estadios eliminatorios','NUEVO','P2 - OBLIGATORIA',
 'Agregar el estadio al partido. Elegir aleatoriamente entre los cargados de PostgreSQL y retirar el usado del conjunto disponible. Conservar los estadios usados dentro del estado serializado.',
 'Los 13 encuentros eliminatorios usan 13 estadios distintos, también después de salir y cargar. La falta de estadios suficientes produce una validación clara antes de comenzar.',
 'Match, Championship, Stadium y carga de sedes.')
task('N2','Alta, baja y modificación de ciudades','NUEVO','P2 - OBLIGATORIA',
 'Añadir operaciones JDBC de alta, baja y modificación con validación de datos y referencias. No hace falta conectar JavaFX todavía: puede probarse el repositorio aisladamente.',
 'Las operaciones persisten y la base rechaza eliminar una ciudad con estadios. Verificar la FK real: esa restricción no fue comprobada directamente en pgAdmin.',
 'Infrastructure/database/CityRepository y PostgreSQL.')
task('N3','Alta, baja y modificación de estadios','NUEVO','P2 - OBLIGATORIA',
 'Añadir operaciones JDBC y asegurar que cada estadio pertenece a una ciudad existente. Mantener una política clara si se intenta modificar o borrar una sede que ya forma parte de un torneo guardado.',
 'Alta/modificación/baja funcionan y una ciudad inexistente no puede asignarse. Se prueban integridad referencial y errores controlados.',
 'StadiumRepository, validaciones de dominio y PostgreSQL.')
add('Dependencias y trabajo paralelo','h2')
bullet('N2 y N3 pueden desarrollarse en paralelo con estadísticas, una vez estabilizadas las interfaces del dominio.')
bullet('N1 puede usar la lectura JDBC ya existente, pero debe completarse antes de considerar terminadas las eliminatorias.')
bullet('No confundir ABM con la carga inicial: que ya exista findAll no elimina la obligación de implementar alta, baja y modificación.')
add('La selección de estadios no corresponde a CSS ni a FXML. Primero resolver y probar la regla en el núcleo; luego una pantalla puede mostrar o administrar sus datos.','note')

section('5. Desarrollar: estadísticas y reportes')
task('N4','Estadísticas propias del campeonato','NUEVO','P2 - BASE DE LOS REPORTES',
 'Calcular partidos y minutos por jugador, goles normales y de penal, goles recibidos por arquero, tarjetas, resultados por equipo y partidos por árbitro. Separar estas estadísticas de las históricas cargadas en Player.statistics.',
 'Los valores se obtienen de encuentros e incidencias coherentes y no se duplican al cargar o consultar varias veces.',
 'Servicios de estadísticas y modelo de participación; depende de C2/C3.')
add('Reportes obligatorios - P3, después de sus datos base','h2')
for t in [
 '<b>N5. Goleadores:</b> goles de partidos y cuántos fueron de penal; excluir las tandas de definición.',
 '<b>N6. Fair Play por equipos:</b> acordar/documentar el puntaje utilizado y calcularlo desde sanciones del torneo.',
 '<b>N7. Minutos y partidos:</b> ranking de jugadores según participación real.',
 '<b>N8. Cuadro de cruces:</b> clasificados por zona, cuartos, semis, final y ganadores. Depende del estado persistente de C1/C5.',
 '<b>N9. Equipos alfabéticos:</b> edad promedio de jugadores, edad/nacionalidad del DT, GF/GC y efectividad sobre puntos posibles. Definir el tratamiento de tandas y final según la cátedra.',
 '<b>N10. Árbitros:</b> ranking por partidos dirigidos, años de referato y promedio de estos años al final.',
 '<b>N11. Jugadores por posición:</b> Todos o uno particular; datos completos, partidos, minutos y goles. Arqueros: recibidos y promedio por partido.',
 '<b>N12. Identificaciones PDF:</b> datos personales, rol, fotografía y código de barras de todas las personas participantes.'
]: bullet(t)
add('Orden para estos reportes','h2')
add('Primero N4. Luego N5/N6/N7/N9/N10/N11, que reutilizan estadísticas. N8 depende del estado de competencia y puede desarrollarse en paralelo. N12 requiere fotografías, un identificador y generación de códigos de barras, pero no depende de los minutos jugados.')
add('Se considera terminado un reporte cuando puede emitirse en cualquier momento del torneo, contempla valores todavía no acumulados y sigue siendo correcto después de cargar una partida.','note')

section('6. Presentación, calidad y entrega - P3')
task('C8','Tabla antes y después de cada resultado','COMPLETAR PRESENTACIÓN','P3 | REQUISITO OBLIGATORIO',
 'Hoy se muestra después de cada jornada. La entrega debe permitir ver el estado de la tabla antes y después de cada resultado registrado. Resolverlo en JavaFX cuando se conecte la interfaz; no duplicar necesariamente la solución en terminal.',
 'El usuario puede identificar ambos estados por partido y consultar todas las columnas exigidas.',
 'Presentación JavaFX y coordinación de simulación.')
task('N13','Conectar la interfaz final','NUEVO','P3 | POR AHORA POSPUESTO',
 'Crear/cargar torneo, avanzar por fases, consultar partidos y eventos, mostrar tablas, criterios del ganador, cuadro de cruces, reportes y ABM. Conservar el diseño visual y ejecutar operaciones a través de la lógica, no SQL en FXML.',
 'El flujo completo es operable desde JavaFX y no necesita la terminal para funciones obligatorias.',
 'Controller, GUI, recursos FXML/CSS y capa de coordinación.')
task('C9','Reducir responsabilidades de Championship','MEJORA DE DISEÑO','TRANSVERSAL',
 'Separar resolución de series, penales y estadísticas en componentes con responsabilidades claras. Evitar una reestructuración masiva antes de contar con pruebas.',
 'Las reglas pueden probarse aisladamente y la interfaz no contiene lógica deportiva.',
 'Core/Run y componentes de dominio/simulación.')
task('C10','Ordenar Git y archivos generados','MEJORA DE PROCESO','TRANSVERSAL - HACER PRONTO',
 'Revisar el seguimiento de target y campeonato.dat. Incorporar una política de .gitignore y retirar del índice lo que el equipo acuerde, sin borrar código ni partidas inadvertidamente.',
 'Los commits contienen fuentes y recursos intencionales, no cambios accidentales de compilación o partidas.',
 'Repositorio y acuerdos del equipo.')
add('Entregables y trabajo en inglés','h2')
bullet('<b>N14:</b> actualizar documentación técnica, decisiones de diseño y evidencias de POO.')
bullet('<b>N15:</b> conservar evidencias de gestión ágil, tareas/sprints y participación de cada integrante en inglés. Mantenerlo durante el desarrollo, no reconstruirlo al final.')
bullet('<b>N16:</b> preparar presentación y defensa individual/grupal, incluyendo la participación en inglés solicitada. Revisar también nombres y textos de la interfaz.')
add('Estos entregables pueden existir fuera del repositorio; su ausencia no se confirmó. Deben verificarse con el equipo.','small')

section('7. Orden de ejecución y controles de cierre')
add('Secuencia recomendada para el equipo','h2')
for t in [
 '<b>Bloque 1:</b> C1 - persistencia y fases. No comenzar otro torneo al continuar uno guardado.',
 '<b>Bloque 2:</b> C2/C3 - habilitados, cambios y cronología. Cubrirlos con pruebas.',
 '<b>Bloque 3:</b> C4/C5/C6/C7 - desempates, criterios, penales y clasificación comprobados.',
 '<b>Bloque 4:</b> N1/N2/N3 - estadios sin repetición y ABM relacional. N4 puede avanzar en paralelo tras el bloque 2.',
 '<b>Bloque 5:</b> N4 completo y cálculos de los reportes N5-N11. N12 puede desarrollarse independientemente.',
 '<b>Bloque 6:</b> C8/N13 - presentación JavaFX y reportes visibles. La integración de interfaz sigue pospuesta por decisión del usuario.',
 '<b>En todos los bloques:</b> pruebas, C9/C10 cuando convenga y N14-N16 como proceso continuo.'
]: bullet(t)
add('Lista de aceptación final','h2')
for t in [
 'Un torneo normal termina con 37 encuentros y un único campeón persistente.',
 'Los 13 encuentros eliminatorios usan sedes distintas.',
 'Clasificados y ganadores coinciden con el reglamento exacto del TP.',
 'Suspensiones, sustituciones, pateadores y minutos reflejan participantes válidos.',
 'Salir y continuar desde cada fase no repite ni pierde información.',
 'Se conservan formaciones, incidencias, tandas y criterios de resolución.',
 'La interfaz final permite el flujo completo y la tabla antes/después por resultado.',
 'Todos los reportes y los ABM funcionan y tienen pruebas.',
 'Fuentes, documentación, gestión en inglés y defensa están preparados.'
]: bullet(t)
add('Fuentes y alcance','h2')
add('Consigna: TPGrupal-2026-Campeonato Futbol (1).pdf, requisitos de páginas 3-7 y entregables/no funcionales de páginas 9-10. Código: versión local 79baa más cambios inspeccionados del commit remoto 47e0df9. No se hizo pull, no se modificó JavaFX ni se comprobó directamente el esquema vivo de PostgreSQL.','small')
add('<link href="https://github.com/BautiOscoz/TP_PrograB/commit/47e0df9d2fe4a8efc5cff2ae53ee451fe4446e6f" color="#235F8B">Referencia del commit revisado en GitHub</link>','small')

def footer(canvas,doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor('#CCD5DE'))
    canvas.line(1.7*cm,1.25*cm,A4[0]-1.7*cm,1.25*cm)
    canvas.setFont('Arial',8)
    canvas.setFillColor(MUTED)
    canvas.drawString(1.7*cm,.85*cm,'TP_PrograB - Estado y prioridades - 13/09/2026')
    canvas.drawRightString(A4[0]-1.7*cm,.85*cm,str(doc.page))
    canvas.restoreState()

SimpleDocTemplate(str(OUT),pagesize=A4,leftMargin=1.7*cm,rightMargin=1.7*cm,
                  topMargin=1.65*cm,bottomMargin=1.7*cm,
                  title='TP_PrograB - Estado y prioridades',author='Guía de trabajo').build(
                  story,onFirstPage=footer,onLaterPages=footer)
print(OUT)
