public class LamportClock {

    // Clase interna para representar un Proceso
    static class Proceso {
        String id;
        int reloj;

        public Proceso(String id) {
            this.id = id;
            this.reloj = 0;
        }

        // Simula un evento local.
        public void eventoLocal() {
            this.reloj++; // C_i = C_i + 1
            System.out.println("[" + this.id + "] Evento Local. Reloj: " + this.reloj);
        }

        // Simula el envío de un mensaje.
        public Mensaje enviarMensaje(String contenido) {
            this.reloj++; // C_i = C_i + 1 (antes de enviar)
            int marcaTiempo = this.reloj;
            System.out.println("\n[" + this.id + "] ENVIANDO: '" + contenido + "' con T=" + marcaTiempo);
            return new Mensaje(contenido, marcaTiempo);
        }

        // Simula la recepción de un mensaje y aplica la regla de Lamport.
        public void recibirMensaje(Mensaje mensaje) {
            int T_local = this.reloj;
            int T_recibido = mensaje.T_envio;

            // Regla de Lamport: C_i = max(C_i, T_envio) + 1
            this.reloj = Math.max(T_local, T_recibido) + 1;

            System.out.println("[" + this.id + "] RECIBIENDO: '" + mensaje.contenido + "'");
            System.out.println("    - T_Local Anterior: " + T_local);
            System.out.println("    - T_Mensaje: " + T_recibido);
            System.out.println("    - T_Nuevo: " + this.reloj + " (max(" + T_local + ", " + T_recibido + ") + 1)");
        }
    }

    // Clase interna para representar un Mensaje con su marca de tiempo.
    static class Mensaje {
        String contenido;
        int T_envio;

        public Mensaje(String contenido, int T_envio) {
            this.contenido = contenido;
            this.T_envio = T_envio;
        }
    }

    public static void main(String[] args) {
        System.out.println("--- INICIO DE SIMULACIÓN LAMPORT ---");
        Proceso P1 = new Proceso("P1");
        Proceso P2 = new Proceso("P2");
        System.out.println("Estado Inicial: P1=" + P1.reloj + ", P2=" + P2.reloj);
        System.out.println("------------------------------------");

        // 1. P1 tiene un evento local
        P1.eventoLocal(); // P1: 1

        // 2. P2 tiene dos eventos locales
        P2.eventoLocal(); // P2: 1
        P2.eventoLocal(); // P2: 2

        // 3. P1 envía un mensaje a P2
        P1.eventoLocal(); // Evento local antes de enviar, P1: 2
        Mensaje mensaje_P1_P2 = P1.enviarMensaje("Hola desde P1"); // P1: 3

        // 4. P2 recibe el mensaje de P1 y aplica ajuste
        // P2 T_local=2, T_mensaje=3. Nuevo T = max(2, 3) + 1 = 4
        P2.recibirMensaje(mensaje_P1_P2); 

        // 5. P2 tiene un evento local y luego envía a P1
        P2.eventoLocal(); // P2: 5
        Mensaje mensaje_P2_P1 = P2.enviarMensaje("Recibido en P2"); // P2: 6

        // 6. P1 recibe el mensaje de P2 y aplica ajuste
        // P1 T_local=3, T_mensaje=6. Nuevo T = max(3, 6) + 1 = 7
        P1.recibirMensaje(mensaje_P2_P1); 

        System.out.println("------------------------------------");
        System.out.println("--- ESTADO FINAL ---");
        System.out.println("Reloj Final de P1: " + P1.reloj);
        System.out.println("Reloj Final de P2: " + P2.reloj);
        System.out.println("--- FIN DE SIMULACIÓN ---");
    }
}