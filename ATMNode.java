import java.io.*;
import java.net.*;
import java.nio.file.*;

/**
 * Progetto di Intelligenza Artificiale Distribuita
 * Codice corso: 0322509INGINF05I
 * Sistema di transazioni bancarie distribuite con Token Ring.
 *
 * Studente: Vincenzo Esposito
 * Matricola: 0322500120
 * Repository: https://github.com/937enzo-crypto/token-ring-bank
 *
 * Ogni istanza di questa classe rappresenta un ATM indipendente.
 * I nodi comunicano esclusivamente tramite socket TCP su localhost.
 *
 * Topologia:
 * ATM1 -> ATM2 -> ATM3 -> ATM4 -> ATM1
 *
 * Solo il nodo che possiede il TOKEN può accedere alla sezione critica
 * e quindi leggere/modificare il saldo bancario.
 */
public class ATMNode {

    private static final String HOST = "localhost";
    private static final String BALANCE_FILE = "balance.txt";

    private static final long TOKEN_DELAY_MS = 1000;
    private static final long RETRY_DELAY_MS = 1000;

    private final int nodeId;
    private final int myPort;
    private final int successorPort;
    private final int successorId;

    private final TransactionType transactionType;
    private final int transactionAmount;

    private boolean transactionExecuted = false;

    enum TransactionType {
        NONE,
        DEPOSIT,
        WITHDRAW
    }

    public ATMNode(int nodeId, TransactionType transactionType, int transactionAmount) {
        this.nodeId = nodeId;
        this.transactionType = transactionType;
        this.transactionAmount = transactionAmount;

        switch (nodeId) {
            case 1:
                myPort = 5001;
                successorPort = 5002;
                successorId = 2;
                break;
            case 2:
                myPort = 5002;
                successorPort = 5003;
                successorId = 3;
                break;
            case 3:
                myPort = 5003;
                successorPort = 5004;
                successorId = 4;
                break;
            case 4:
                myPort = 5004;
                successorPort = 5001;
                successorId = 1;
                break;
            default:
                throw new IllegalArgumentException("L'ID del nodo deve essere compreso tra 1 e 4.");
        }
    }

    public void start() {
        log("Avvio ATM" + nodeId);
        log("Porta locale: " + myPort);
        log("Successore: ATM" + successorId + " sulla porta " + successorPort);

        if (transactionType == TransactionType.NONE) {
            log("Nessuna transazione programmata.");
        } else {
            log("Transazione programmata: " + transactionType + " " + transactionAmount);
        }

        if (nodeId == 1) {
            initializeBalance();
        }

        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            log("In ascolto...");

            if (nodeId == 1) {
                Thread.sleep(2000);
                log("Possesso iniziale del TOKEN.");
                handleToken();
            }

            while (true) {
                try (
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream())
                    )
                ) {
                    String message = in.readLine();

                    if ("TOKEN".equals(message)) {
                        log("TOKEN ricevuto.");
                        handleToken();
                    } else {
                        log("Messaggio sconosciuto ricevuto: " + message);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ATM" + nodeId + "] Errore di rete: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[ATM" + nodeId + "] Processo interrotto.");
        }
    }

    private void handleToken() throws InterruptedException {
        if (!transactionExecuted && transactionType != TransactionType.NONE) {
            executeTransaction();
        } else {
            if (transactionType == TransactionType.NONE) {
                log("Nessuna transazione pendente.");
            } else {
                log("Transazione già eseguita.");
            }
        }

        Thread.sleep(TOKEN_DELAY_MS);
        sendToken();
    }

    private void executeTransaction() {
        log(">>> INIZIO TRANSAZIONE / SEZIONE CRITICA");

        try {
            int currentBalance = readBalance();
            log("Saldo corrente: " + currentBalance);

            int newBalance = currentBalance;

            switch (transactionType) {
                case DEPOSIT:
                    newBalance = currentBalance + transactionAmount;
                    log("Deposito di " + transactionAmount);
                    break;

                case WITHDRAW:
                    log("Richiesta prelievo di " + transactionAmount);
                    if (transactionAmount > currentBalance) {
                        log("OPERAZIONE RIFIUTATA: saldo insufficiente.");
                        transactionExecuted = true;
                        log("<<< FINE TRANSAZIONE / SEZIONE CRITICA");
                        return;
                    }
                    newBalance = currentBalance - transactionAmount;
                    break;

                case NONE:
                    return;
            }

            writeBalance(newBalance);

            log("Saldo aggiornato: " + currentBalance + " -> " + newBalance);
            transactionExecuted = true;

        } catch (IOException e) {
            System.err.println("[ATM" + nodeId + "] Errore durante la transazione: " + e.getMessage());
        }

        log("<<< FINE TRANSAZIONE / SEZIONE CRITICA");
    }

    private int readBalance() throws IOException {
        String value = Files.readString(Path.of(BALANCE_FILE)).trim();
        return Integer.parseInt(value);
    }

    private void writeBalance(int newBalance) throws IOException {
        Files.writeString(
            Path.of(BALANCE_FILE),
            Integer.toString(newBalance),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void initializeBalance() {
        try {
            Files.writeString(
                Path.of(BALANCE_FILE),
                "1000",
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            log("Saldo iniziale impostato a 1000.");
        } catch (IOException e) {
            System.err.println("[ATM1] Impossibile inizializzare il saldo: " + e.getMessage());
            System.exit(1);
        }
    }

    private void sendToken() throws InterruptedException {
        while (true) {
            try (
                Socket socket = new Socket(HOST, successorPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                out.println("TOKEN");
                log("TOKEN inviato ad ATM" + successorId + ".");
                return;
            } catch (IOException e) {
                log("ATM" + successorId + " non raggiungibile. Nuovo tentativo tra " + RETRY_DELAY_MS + " ms.");
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
    }

    private void log(String message) {
        System.out.println("[ATM" + nodeId + "] " + message);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        try {
            int nodeId = Integer.parseInt(args[0]);
            TransactionType type = TransactionType.valueOf(args[1].toUpperCase());

            int amount = 0;

            if (type != TransactionType.NONE) {
                if (args.length < 3) {
                    printUsage();
                    return;
                }

                amount = Integer.parseInt(args[2]);

                if (amount <= 0) {
                    System.out.println("L'importo deve essere positivo.");
                    return;
                }
            }

            ATMNode node = new ATMNode(nodeId, type, amount);
            node.start();

        } catch (IllegalArgumentException e) {
            System.out.println("Parametri non validi: " + e.getMessage());
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Utilizzo:");
        System.out.println("  java ATMNode <id> NONE");
        System.out.println("  java ATMNode <id> DEPOSIT <importo>");
        System.out.println("  java ATMNode <id> WITHDRAW <importo>");
        System.out.println();
        System.out.println("Esempio:");
        System.out.println("  java ATMNode 2 WITHDRAW 200");
    }
}
