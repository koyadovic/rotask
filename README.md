# Rotask

App Android para repartir tu tiempo de trabajo entre varias tareas con pesos configurables, rotando hacia la más atrasada cada vez que pulsas "Iniciar trabajo".

## Idea

- Añades tareas con un nombre, una descripción opcional (qué entrenar) y un peso (entero).
- Cada tarea puede estar **activa** o **pausada**. Las pausadas no rotan pero siguen visibles y conservan su deuda.
- Configuras minutos por día.
- Cada tarea activa recibe un objetivo diario proporcional a su peso, repartido sobre la suma de pesos de las activas. Ej: 4 tareas activas de pesos 2/1/1/1 con 60 min/día → 24/12/12/12 min.
- Al pulsar **Iniciar trabajo** se elige la tarea activa con más segundos pendientes hoy y arranca un contador hasta cumplir su objetivo.
- Si un día no llegas al objetivo, el déficit se acarrea y se suma al objetivo del día siguiente. Trabajar de más no genera crédito. Las tareas pausadas no acumulan déficit nuevo.

## Stack

- Kotlin 2.0 + Jetpack Compose (Material 3)
- Room para persistencia local
- Navigation Compose
- minSdk 26, targetSdk 35

## Estructura

```
app/src/main/java/com/rotask/
├── data/        Entidades + DAOs + AppDatabase (Room)
├── domain/      TaskScheduler (settle + pick) y RotaskRepository
└── ui/
    ├── home/    HomeScreen + HomeViewModel
    ├── work/    WorkScreen + WorkViewModel
    ├── theme/   Colores, tipografía, Theme
    └── format/  Helpers de formateo (mm:ss)
```

## Build

```bash
./gradlew assembleDebug
```

Recomendado: abrir el proyecto en Android Studio (Hedgehog+) y dejar que sincronice Gradle la primera vez.

## Algoritmo (resumen)

`debtSeconds` por tarea acumula lo que se debe trabajar de días anteriores. Al pasar la medianoche, `ensureSettled` re-juega los días pendientes:

```
para cada día d entre lastSettleDate y hoy:
  para cada tarea t:
    base   = dailyMinutes * 60 * peso_t / suma_pesos
    hecho  = trabajado en t durante d
    deuda_t = max(0, deuda_t + base - hecho)
lastSettleDate = hoy
```

Hoy, el objetivo efectivo de cada tarea es `base_hoy + deuda_t`. `pickNext()` devuelve la tarea con mayor `objetivo - trabajado_hoy`.
