import javaSimulation.*;
import javaSimulation.Process;

public class GasStationSimulation extends Process {
    int noOfPumps;
    double simPeriod = 300;
    Head availablePumps = new Head();
    Head waitingCars = new Head();
    Random random = new Random(5);
    double totalServiceTime;
    int noOfServedCars, maxQueueLength,noOfCarsLeft, currentCarsOnStation = 0;
    int maxCapacity = 5; //celková maximální kapacita aut na stanici
    long startTime = System.currentTimeMillis();

    GasStationSimulation(int n) { noOfPumps = n; }

    public void actions() {
        for (int i = 0; i < noOfPumps; i++)
            new FuelPump().into(availablePumps);
        activate(new CarGenerator());
        hold(simPeriod + 1000000);
        report();
    }

    void report() {
        System.out.println(noOfPumps + " fuel pump simulation");
        System.out.println("Number of cars served = " + noOfServedCars);
        System.out.println("Number of cars that left = "+ noOfCarsLeft);
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(2);
        System.out.println("Average service time = " + fmt.format(totalServiceTime / noOfServedCars));
        System.out.println("Maximum queue length = " + maxQueueLength);
        System.out.println("\nExecution time: " + fmt.format((System.currentTimeMillis() - startTime) / 1000.0) + " secs.\n");
    }

    class Car extends Process {
        private double fuelLevel;
        //konstruktor pro vyvtáření objektu Car() s náhodným vygenerováním aktuálního množství paliva na základě normálního rozdělení
        public Car(){
            Random rand = new Random();
            double mean = 30;
            double std = 10;
            double fuelLevel = rand.nextGaussian() *std + mean;
            this.fuelLevel = Math.max(0, Math.min(fuelLevel, 100)); //omezení kvůli předpokladu množství paliva v procentech
        };
        public double getCarFuelLevel(){
            return this.fuelLevel;
        };
        public void actions() {
            double arrivalTime = time();
            // kontrola, zda kapacita fronty a počet dostupných pump nedosáhl maximální kapacity
            if (waitingCars.cardinal() < maxCapacity) {
                into(waitingCars);
                currentCarsOnStation++;
                if (maxQueueLength < waitingCars.cardinal()) maxQueueLength = waitingCars.cardinal();
                if (!availablePumps.empty()) activate((FuelPump) availablePumps.first());
                passivate();
                currentCarsOnStation--;
                noOfServedCars++;
                totalServiceTime += time() - arrivalTime;
            } else {
                // Auto odjíždí, protože kapacita byla překročena
                noOfCarsLeft++;
            }
        }
    }

    class FuelPump extends Process {
        public void actions() {
            while (true) {
                out();
                while (!waitingCars.empty()) {
                    Car car = (Car) waitingCars.first();
                    car.out();
                    // Přepočet doby tankování v závislosti na chybějícím palivu
                    double fuelNeeded = 100 - car.getCarFuelLevel(); // maximální kapacita nádrže je 100%
                    double refuelingTime = calculateRefuelingTime(fuelNeeded);
                    hold(refuelingTime); // Simulace doby tankování
                    activate(car);
                }
                wait(availablePumps);
            }
        }
        // Metoda pro výpočet doby tankování - předpoklad, že existuje přímá závislost mezi chybějícím palivem a dobou tankování
        // předpokladem je, že natankování 100% nádrže trvá 10 minut
        private double calculateRefuelingTime(double fuelNeeded) {
            return fuelNeeded / 10;
        }
    }

    class CarGenerator extends Process {
        public void actions() {
            while (time() <= simPeriod) {
                activate(new Car());
                hold(random.negexp(1 / 5.0));
            }
        }
    }
    public static void main(String args[]) {
        activate(new GasStationSimulation(1));
        activate(new GasStationSimulation(2));
        activate(new GasStationSimulation(4));
    } 
}
