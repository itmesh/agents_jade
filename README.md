# 1. Uruchomienie JAVA

Aby uruchomić projekt należy poprawnie skonfigurować proces uruchamiania. W folderze doc znajdują się zrzuty ekranu zawierającą przykładową poprawną konfigurację.

## 1.1. Main
Main class
```xml
jade.Boot
```
<img src="/doc/configuration_1.PNG" height="200">

## 1.2. Arguments
Program arguments
```xml
-gui -agents SIMULATION:agents_jade.Simulation;
```
<img src="/doc/configuration_2.PNG" height="200">

# 2. Uruchomienie JAR

Plik agents_jade.jar znajduję się w folderze output. Plik jade.jar znajdziemy w lib.
```xml
set CLASSPATH=%CLASSPATH%;<lokalizacja>/jade.jar;<lokalizacja>/agents_jade.jar
```
Uruchomienie
```xml
java jade.Boot -gui -agents SIMULATION:agents_jade.Simulation
```



# 3. Zasada działania:

1. Jako pierwszy zostaje uruchomiony agent Simulation odpowiadający za start symulacji komunikacji agentów. Do jego zadania należy uruchomienie pozostałych agentów.
2. Następnie zostają uruchomieni agenci IMAgent, którzy nasłuchują wiadomości.
3. Zostaje uruchomiony agent IMMasterAgent, który co dany wyznaczony czas wysyła wiadomość pokolei do każdego z agentów.
4. IMAgent po otrzymaniu wiadomości wysyła odpowiedź.
5. IMMasterAgent po otrzymaniu odpowiedzi wysyła pozdrowienia.
