import javaSimulation.*;
import javaSimulation.Process;

public class GasStationSimulation extends Process {
    // Počet čerpacích stanic a pokladních
    int noOfPumps;
    int payed;
    // Doba simulace
    double simPeriod = 300;
    // Seznamy pro čerpací stanice, čekající auta a auta čekající na platbu
    Head pumps = new Head();
    Head waitingCars = new Head();
    Head waitingForPaymentCars = new Head();
    Head cashiers = new Head();
    Random random = new Random(5);
    double totalServiceTime;
    int noOfServedCars, maxQueueLength, noOfCarsLeft, currentCarsOnStation = 0;
    int noOfCashiers;
    int maxCapacity = 10; // Maximální kapacita aut na stanici
    long startTime = System.currentTimeMillis();

    // Sledování využití čerpacích stanic a pokladních
    int currentPumpUsage = 0;
    int maxPumpUsage = 0;
    int currentCashierUsage = 0;
    int maxCashierUsage = 0;


    GasStationSimulation(int pumps, int noOfCashiers) {
        this.noOfPumps = pumps;
        this.noOfCashiers = noOfCashiers;
    }

    public void actions() {
        // Inicializace čerpacích stanic a pokladních
        for (int i = 0; i < noOfPumps; i++)
            new FuelPump().into(pumps);
        for (int i = 0; i < noOfCashiers; i++)
            new Cashier().into(cashiers);

        // Aktivace generátoru aut
        activate(new CarGenerator());
        hold(simPeriod + 1000000);
        // Výpis výsledků simulace
        report();
    }

    void report() {
        // Zobrazí základní informace o simulaci a její výsledky
        System.out.println(noOfPumps + " fuel pumps and " + noOfCashiers + " cashiers simulation");
        System.out.println("Number of cars served = " + noOfServedCars);
        System.out.println("Number of cars that left = " + noOfCarsLeft);
        System.out.println("Number of cars that paid: "+payed);
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(2);
        System.out.println("Average service time = " + fmt.format(totalServiceTime / noOfServedCars));
        System.out.println("Maximum queue length = " + maxQueueLength);
        System.out.println("Maximum pump usage = " + maxPumpUsage);
        System.out.println("Maximum cashier usage = " + maxCashierUsage);
        System.out.println("\nExecution time: " + fmt.format((System.currentTimeMillis() - startTime) / 1000.0) + " secs.\n");
    }
    
    // Třída Car reprezentuje auto přijíždějící na čerpací stanici
    class Car extends Process {
        double fuelLevel;
        FuelPump assignedPump; // Přiřazená čerpací stanice

        public Car(){
            // Generování náhodného množství paliva s normálním rozdělením
            double mean = 30;
            double std = 10;
            double fuelLevel = random.nextGaussian() * std + mean;
            // Omezení množství paliva mezi 0 a 100
            this.fuelLevel = Math.max(0, Math.min(fuelLevel, 100));
        }

        public void actions() {
            double arrivalTime = time();
            // Pokud je kapacita stanice dostatečná, auto se zařadí do fronty
            if (waitingCars.cardinal() + currentCarsOnStation < maxCapacity) {
                into(waitingCars);
                currentCarsOnStation++;
                // Aktualizace maximální délky fronty
                if (maxQueueLength < waitingCars.cardinal()) maxQueueLength = waitingCars.cardinal();
                
                // Hledání volné čerpací stanice
                while (assignedPump == null) {
                    for (Link l = pumps.first(); l != null; l = l.suc()) {
                        FuelPump pump = (FuelPump) l;
                        if (!pump.isInUse()) {
                            pump.setInUse(this); // Přiřazení stanice autu
                            assignedPump = pump;
                            activate(pump);
                            break;
                        }
                    }
                    if (assignedPump == null) hold(1); // Pokud není volná stanice, auto čeká a cyklicky kontroluje dostupné pumpy
                }
                
                passivate(); // Čekání na dokončení tankování a platby
                
                out(); // Odstranění auta ze simulace
                currentCarsOnStation--;
                noOfServedCars++;
                totalServiceTime += time() - arrivalTime;
            } else {
                noOfCarsLeft++; // Auto opouští stanici, pokud není dostatek místa
            }
        }
    }

    // Třída FuelPump reprezentuje jednotlivé čerpací stanice
    class FuelPump extends Process {
        private Car inUseBy = null; // Auto, které využívá stanici
        
        public boolean isInUse() {
            return inUseBy != null; // Kontrola, zda je stanice využívána
        }
        
        public void setInUse(Car car) {
            this.inUseBy = car; // Přiřazení auta k stanici
            if (car != null) {
                currentPumpUsage++;
                maxPumpUsage = Math.max(maxPumpUsage, currentPumpUsage);
            } else {
                currentPumpUsage--;
            }
        }

        public void actions() {
            while (true) {
                if (inUseBy != null) {
                    // Výpočet potřebného množství paliva a času tankování
                    double fuelNeeded = 100 - inUseBy.fuelLevel;
                    double refuelingTime = calculateRefuelingTime(fuelNeeded);
                    hold(refuelingTime); // Simulace tankování

                    inUseBy.into(waitingForPaymentCars);
                    // Aktivace pokladen pro zpracování platby
                    for (Link l = cashiers.first(); l != null; l = l.suc()) {
                        Cashier cashier = (Cashier) l;
                        if (!cashier.isBusy) {
                            activate(cashier);
                            break; // Aktivace první volné pokladny
                        }
                    }
                    passivate();
                } else {
                    passivate(); // Čekání na další využití
                }
            }
        }

        private double calculateRefuelingTime(double fuelNeeded) {
            return fuelNeeded / 100; // Výpočet času tankování za předpokladu, že plná nádrž se natankuje za 10 minut
        }
    }

    // Třída Cashier reprezentuje pokladnu
    class Cashier extends Process {
        boolean isBusy = false; // Indikátor, zda je pokladna zaneprázdněna
        
        public void actions() {
            while (true) {
                out();
                while (!waitingForPaymentCars.empty()) {
                    isBusy = true;
                    currentCashierUsage++;
                    maxCashierUsage = Math.max(maxCashierUsage, currentCashierUsage);

                    Car car = (Car) waitingForPaymentCars.first();
                    car.out(); // Odstranění auta z fronty na platbu
                    hold(calculatePaymentTime()); // Simulace času platby
                    car.assignedPump.setInUse(null); // Uvolnění čerpací stanice
                    activate(car); // Opětovná aktivace auta pro odjezd ze stanice
                    payed++;

                    isBusy = false;
                    currentCashierUsage--;
                }
                wait(cashiers); // Čekání na další auto pro platbu
            }
        }

        private double calculatePaymentTime() {
            return random.negexp(1 / 2.0); // doba placení se řídí exponenciálním rozdělením
        }
    }

    // Třída CarGenerator generuje auta v pravidelných intervalech
    //obsahuje vlastní instanci Random kvůli tomu, aby byl zajištěn stejný počet vygenerovaných aut v každé simulaci.
    class CarGenerator extends Process {
        Random localRandom;
        public CarGenerator() {
            this.localRandom = new Random(5); // Inicializace s pevným seedem
        }
        public void actions() {
            while (time() <= simPeriod) {
                activate(new Car());
                hold(localRandom.negexp(1 / 5.0)); // Generování nových aut v náhodných intervalech
            }
        }
    }

    public static void main(String args[]){
        activate(new GasStationSimulation(3,1));
        activate(new GasStationSimulation(3,2));
        activate(new GasStationSimulation(3,3));
    }
}
