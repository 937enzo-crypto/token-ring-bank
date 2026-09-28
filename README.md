# Sistema di transazioni bancarie distribuite con Token Ring

**Progetto finale di Intelligenza Artificiale Distribuita**  
**Codice corso:** 0322509INGINF05I  
**Studente:** Vincenzo Esposito  
**Matricola:** 0322500120  
**Repository GitHub:** https://github.com/937enzo-crypto/token-ring-bank

## Obiettivo

Il progetto simula quattro ATM indipendenti che operano su un unico saldo bancario. I quattro nodi sono processi Java separati, eseguiti su `localhost` e collegati in un anello logico:

```text
ATM1 -> ATM2 -> ATM3 -> ATM4 -> ATM1
```

La mutua esclusione e' garantita esclusivamente dal Token Ring: solo il nodo che possiede il token puo' entrare nella sezione critica ed eseguire una transazione.

## Tecnologia utilizzata

- Java 17 o superiore
- Socket TCP della libreria standard Java
- Nessuna libreria esterna
- Comunicazione su `localhost`

Il progetto e' stato verificato anche con OpenJDK 21.

## File del progetto

- `ATMNode.java`: implementazione dei quattro nodi ATM
- `balance.txt`: saldo bancario condiviso come risorsa applicativa; viene inizializzato automaticamente da ATM1 a `1000`
- `.gitignore`: esclude dal repository file compilati e file di log locali
- `README.md`: istruzioni complete di compilazione ed esecuzione

Non vengono usati `FileLock`, lock di sistema o altri meccanismi di mutua esclusione. Il file rappresenta solo la risorsa protetta; il diritto di accedervi deriva unicamente dal possesso del token.

## Porte utilizzate

| Nodo | Porta locale | Successore | Porta successore |
|---|---:|---|---:|
| ATM1 | 5001 | ATM2 | 5002 |
| ATM2 | 5002 | ATM3 | 5003 |
| ATM3 | 5003 | ATM4 | 5004 |
| ATM4 | 5004 | ATM1 | 5001 |

Ogni nodo conosce soltanto il proprio successore nell'anello.

## Compilazione

Aprire un terminale nella cartella del progetto ed eseguire:

```bash
javac ATMNode.java
```

## Esecuzione

Aprire quattro terminali distinti, tutti posizionati nella stessa cartella del progetto.

Per rendere l'avvio piu' semplice, avviare prima ATM2, ATM3 e ATM4 e infine ATM1.

### Terminale 2 - ATM2

```bash
java ATMNode 2 WITHDRAW 200
```

### Terminale 3 - ATM3

```bash
java ATMNode 3 DEPOSIT 100
```

### Terminale 4 - ATM4

```bash
java ATMNode 4 WITHDRAW 500
```

### Terminale 1 - ATM1

```bash
java ATMNode 1 NONE
```

ATM1 inizializza il saldo a `1000` e possiede il token iniziale.

## Sequenza attesa

La demo usa le transazioni indicate nella traccia:

1. ATM1 non esegue alcuna transazione e inoltra il token.
2. ATM2 preleva 200: `1000 -> 800`.
3. ATM3 deposita 100: `800 -> 900`.
4. ATM4 preleva 500: `900 -> 400`.
5. ATM4 inoltra il token ad ATM1 e l'anello continua a funzionare.

Le transazioni sono eseguite una sola volta. Nei giri successivi ogni nodo inoltra il token senza ripetere l'operazione gia' completata.

## Log attesi

Ogni nodo stampa chiaramente:

- ricezione del token;
- inizio della transazione / sezione critica;
- saldo letto;
- operazione eseguita;
- saldo aggiornato;
- fine della transazione / sezione critica;
- inoltro del token al successore.

Esempio per ATM2:

```text
[ATM2] TOKEN ricevuto.
[ATM2] >>> INIZIO TRANSAZIONE / SEZIONE CRITICA
[ATM2] Saldo corrente: 1000
[ATM2] Richiesta prelievo di 200
[ATM2] Saldo aggiornato: 1000 -> 800
[ATM2] <<< FINE TRANSAZIONE / SEZIONE CRITICA
[ATM2] TOKEN inviato ad ATM3.
```

## Sintassi generale

Nodo senza transazione:

```bash
java ATMNode <id> NONE
```

Deposito:

```bash
java ATMNode <id> DEPOSIT <importo>
```

Prelievo:

```bash
java ATMNode <id> WITHDRAW <importo>
```

Esempio:

```bash
java ATMNode 2 WITHDRAW 200
```

## Arresto

Per terminare la demo, premere `Ctrl+C` in ciascuno dei quattro terminali.

## Nota sulla correttezza

In condizioni normali viene creato un solo token, inizialmente posseduto da ATM1. Un nodo esegue la sezione critica solo mentre detiene il token e lo inoltra al successore al termine. Di conseguenza, in assenza di duplicazione del token, due ATM non possono eseguire contemporaneamente una transazione sul saldo.

## Video dimostrativo

Prima della consegna, aggiungere al repository il video richiesto dalla traccia. Il video deve mostrare chiaramente i quattro terminali, la circolazione del token e le transazioni fino al saldo finale `400`.
