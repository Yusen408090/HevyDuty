package com.example.myapplication.data.local

import com.example.myapplication.data.local.entities.ExerciseEntity
import com.example.myapplication.domain.model.MuscleGroup

object SeedData {
    val exercises = buildList {
        fun add(id: Long, name: String, muscle: MuscleGroup, equipment: String, instructions: String) =
            add(ExerciseEntity(id, name, muscle.name, equipment, instructions))
        add(1, "Press de banca", MuscleGroup.CHEST, "Barra", "Apoya los pies y la espalda. Baja la barra con control hacia el pecho y empuja sin rebotar.")
        add(2, "Press inclinado", MuscleGroup.CHEST, "Mancuernas", "Ajusta el banco a una inclinación moderada. Baja las mancuernas a los lados del pecho y extiende los brazos.")
        add(3, "Aperturas en polea", MuscleGroup.CHEST, "Polea", "Con los codos ligeramente flexionados, acerca las manos frente al pecho y vuelve con control.")
        add(4, "Flexiones", MuscleGroup.CHEST, "Peso corporal", "Mantén el cuerpo alineado. Flexiona los codos para acercar el pecho al suelo y empuja.")
        add(5, "Press de pecho en máquina", MuscleGroup.CHEST, "Máquina", "Ajusta el asiento para alinear las asas con el pecho. Empuja y regresa de forma controlada.")
        add(6, "Aperturas con mancuernas", MuscleGroup.CHEST, "Mancuernas", "Tumbado en un banco, abre los brazos con una ligera flexión del codo y vuelve sin forzar el recorrido.")
        add(7, "Dominadas", MuscleGroup.BACK, "Peso corporal", "Desde la barra, lleva el pecho hacia arriba sin balancearte. Desciende de forma controlada.")
        add(8, "Jalón al pecho", MuscleGroup.BACK, "Polea", "Mantén el torso estable. Lleva la barra hacia la parte superior del pecho y controla el retorno.")
        add(9, "Remo con barra", MuscleGroup.BACK, "Barra", "Inclina el torso desde la cadera. Lleva la barra hacia el abdomen sin redondear la espalda.")
        add(10, "Remo sentado", MuscleGroup.BACK, "Polea", "Con el torso estable, lleva las asas hacia el abdomen y deja avanzar los brazos con control.")
        add(11, "Remo con mancuerna", MuscleGroup.BACK, "Mancuerna", "Apoya una mano en el banco. Lleva el codo hacia la cadera y baja la mancuerna lentamente.")
        add(12, "Pullover en polea", MuscleGroup.BACK, "Polea", "Con los brazos casi extendidos, baja la barra hacia los muslos sin mover el torso.")
        add(13, "Sentadilla con barra", MuscleGroup.LEGS, "Barra", "Apoya toda la planta del pie. Flexiona caderas y rodillas y sube manteniendo el torso firme.")
        add(14, "Prensa de piernas", MuscleGroup.LEGS, "Máquina", "Apoya la espalda en el respaldo. Flexiona las rodillas sin despegar la cadera y empuja.")
        add(15, "Peso muerto rumano", MuscleGroup.LEGS, "Barra", "Lleva la cadera hacia atrás con una ligera flexión de rodillas. Mantén la barra cerca de las piernas.")
        add(16, "Extensión de cuádriceps", MuscleGroup.LEGS, "Máquina", "Alinea la rodilla con el eje de la máquina. Extiende las piernas y baja con control.")
        add(17, "Curl femoral", MuscleGroup.LEGS, "Máquina", "Ajusta el rodillo sobre los tobillos. Flexiona las rodillas sin levantar la cadera.")
        add(18, "Elevación de gemelos", MuscleGroup.LEGS, "Máquina", "Eleva los talones manteniendo el equilibrio y vuelve lentamente al punto inicial.")
        add(19, "Zancadas", MuscleGroup.LEGS, "Mancuernas", "Da un paso y flexiona ambas rodillas. Empuja con el pie delantero para regresar.")
        add(20, "Hip thrust", MuscleGroup.LEGS, "Barra", "Apoya la espalda alta en un banco. Eleva la cadera manteniendo el abdomen firme y baja con control.")
        add(21, "Press militar", MuscleGroup.SHOULDERS, "Barra", "Desde los hombros, empuja la barra sobre la cabeza sin arquear en exceso la espalda.")
        add(22, "Elevaciones laterales", MuscleGroup.SHOULDERS, "Mancuernas", "Eleva los brazos hacia los lados hasta una altura cómoda, sin impulso, y baja lentamente.")
        add(23, "Pájaros", MuscleGroup.SHOULDERS, "Mancuernas", "Inclina el torso y abre los brazos hacia los lados con una ligera flexión de codos.")
        add(24, "Face pull", MuscleGroup.SHOULDERS, "Polea", "Tira de la cuerda hacia la cara separando las manos. Mantén los hombros controlados.")
        add(25, "Press de hombro sentado", MuscleGroup.SHOULDERS, "Mancuernas", "Con la espalda apoyada, empuja las mancuernas desde los hombros y baja con control.")
        add(26, "Curl de bíceps", MuscleGroup.ARMS, "Barra", "Mantén los codos cerca del torso. Flexiona los brazos sin balancear la espalda.")
        add(27, "Curl martillo", MuscleGroup.ARMS, "Mancuernas", "Con las palmas enfrentadas, flexiona los codos y vuelve lentamente.")
        add(28, "Extensión de tríceps", MuscleGroup.ARMS, "Polea", "Mantén los codos junto al cuerpo y extiende los brazos sin mover los hombros.")
        add(29, "Curl en banco inclinado", MuscleGroup.ARMS, "Mancuernas", "Apoya la espalda y deja los brazos a los lados. Flexiona los codos sin elevarlos.")
        add(30, "Press francés", MuscleGroup.ARMS, "Barra EZ", "Tumbado, flexiona los codos para bajar la barra con control y vuelve a extenderlos.")
        add(31, "Fondos asistidos", MuscleGroup.ARMS, "Máquina", "Ajusta la asistencia. Flexiona los codos hasta un recorrido cómodo y empuja.")
        add(32, "Crunch", MuscleGroup.CORE, "Peso corporal", "Eleva ligeramente los hombros contrayendo el abdomen, sin tirar del cuello.")
        add(33, "Elevación de rodillas", MuscleGroup.CORE, "Peso corporal", "Acerca las rodillas al torso controlando la pelvis y evitando balanceos.")
        add(34, "Crunch en polea", MuscleGroup.CORE, "Polea", "De rodillas, flexiona el torso contrayendo el abdomen y mantén las caderas estables.")
        add(35, "Dead bug", MuscleGroup.CORE, "Peso corporal", "Boca arriba, extiende brazo y pierna contrarios manteniendo la zona lumbar estable. Alterna los lados.")
        add(36, "Giro ruso", MuscleGroup.CORE, "Disco", "Sentado con el torso ligeramente inclinado, gira de forma controlada de un lado al otro.")
    }
}
