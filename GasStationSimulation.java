import javaSimulation.*;
import javaSimulation.Process;

public class GasStationSimulation extends Process {
    int noOfGasolinePumps;
    int noOfDieselPumps;
    double simPeriod = 300;
    Head gasolinePumps = new Head();
    Head dieselPumps = new Head();
    Head waitingGasolineCars = new Head();
    Head waitingDieselCars = new Head();
    Head waitingForPaymentCars = new Head(); // New queue for cars waiting for payment.
    Head cashiers = new Head(); 
    Random random = new Random(10);
    double totalServiceTime;
    int noOfServedCars, maxQueueLength,noOfCarsLeft, currentCarsOnStation,noOfGasolineCars,noOfDieselCars = 0;
    int noOfCashiers;
    int maxCapacity = 6; //celková maximální kapacita aut na stanici
    long startTime = System.currentTimeMillis();

    GasStationSimulation(int gasolinePumps, int dieselPumps, int noOfCashiers) {
        this.noOfGasolinePumps = gasolinePumps;
        this.noOfDieselPumps = dieselPumps;
        this.noOfCashiers = noOfCashiers;
    }
    public void actions() {
        for (int i = 0; i < noOfGasolinePumps; i++)
            new FuelPump(FuelType.GASOLINE).into(gasolinePumps);
        for (int i = 0; i < noOfDieselPumps; i++)
            new FuelPump(FuelType.DIESEL).into(dieselPumps);
        for (int i = 0; i < noOfCashiers; i++)
            new Cashier().into(cashiers);

        activate(new CarGenerator());
        hold(simPeriod + 1000000);
        report();
    }

    void report() {
        System.out.println(noOfGasolinePumps + " gasoline pumps, " + noOfDieselPumps + " diesel pumps, and " + noOfCashiers + " cashiers simulation");
        System.out.println("Number of cars served = " + noOfServedCars);
        System.out.println("Number of cars that left = "+ noOfCarsLeft);
        System.out.println("Number of diesel cars = "+noOfDieselCars);
        System.out.println("Number of gasoline cars = "+noOfGasolineCars);
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(2);
        System.out.println("Average service time = " + fmt.format(totalServiceTime / noOfServedCars));
        System.out.println("Maximum queue length = " + maxQueueLength);
        System.out.println("\nExecution time: " + fmt.format((System.currentTimeMillis() - startTime) / 1000.0) + " secs.\n");
    }

    class Car extends Process {
        private double fuelLevel; // Množství paliva v procentech
        private FuelType fuelType; // Benzín nebo nafta

        // Konstruktor inicializuje auto s určitým typem paliva a náhodně generovaným množstvím paliva
        public Car(FuelType fuelType){
            double mean = 30;
            double std = 10;
            // Generování množství paliva s normálním rozdělením a omezení hodnot mezi 0 a 100
            double fuelLevel = random.nextGaussian() *std + mean;
            this.fuelLevel = Math.max(0, Math.min(fuelLevel, 100)); //omezení kvůli předpokladu množství paliva v procentech
            this.fuelType = fuelType;
            if(this.fuelType.equals(FuelType.GASOLINE)){
                noOfGasolineCars+=1;
            }
            else{
                noOfDieselCars+=1;
            }
        }
        private double getCarFuelLevel(){
            return this.fuelLevel;
        }
        public void actions() {
            double arrivalTime = time();
            Head waitingLine = this.fuelType.equals(FuelType.GASOLINE) ? waitingGasolineCars : waitingDieselCars;
            if (waitingLine.cardinal() + currentCarsOnStation < maxCapacity) {
                into(waitingLine);
                currentCarsOnStation++;
                if (maxQueueLength < waitingLine.cardinal()) maxQueueLength = waitingLine.cardinal();
                Head pumpLine = this.fuelType.equals(FuelType.GASOLINE) ? gasolinePumps : dieselPumps;
                if (!pumpLine.empty()) activate((FuelPump) pumpLine.first());
                passivate(); // čekání na natankování
                into(waitingForPaymentCars); //přesun k placení
                if (!cashiers.empty()) {
                    Cashier cashier = (Cashier) cashiers.first();
                    activate(cashier);
                }
                passivate(); //čekání na zaplacení
                currentCarsOnStation--;
                noOfServedCars++;
                totalServiceTime += time() - arrivalTime;
            } else {
                noOfCarsLeft++;
            }
        }
    }

    class FuelPump extends Process {
        // Atribut fuelType určuje typ paliva, které pumpa poskytuje - buď GASOLINE (benzín) nebo DIESEL (nafta)
        private FuelType fuelType;

        // Konstruktor FuelPump inicializuje pumpu s určitým typem paliva
        public FuelPump(FuelType fuelType) {
            this.fuelType = fuelType;
        }
        public void actions() {
            while (true) {
                out();
                Head waitingLine = this.fuelType.equals(FuelType.GASOLINE) ? waitingGasolineCars : waitingDieselCars;
                while (!waitingLine.empty()) {
                    Car car = (Car) waitingLine.first();
                    car.out();
                    double fuelNeeded = 100 - car.getCarFuelLevel();
                    double refuelingTime = calculateRefuelingTime(fuelNeeded);
                    hold(refuelingTime); // simulace tankování
                    activate(car);
                }
                wait(this.fuelType.equals(FuelType.GASOLINE) ? gasolinePumps : dieselPumps);
            }
        }
       // Metoda pro výpočet doby tankování - předpoklad, že existuje přímá závislost mezi chybějícím palivem a dobou tankování
        // předpokladem je, že natankování 100% nádrže trvá 10 minut
        private double calculateRefuelingTime(double fuelNeeded) {
            return fuelNeeded / 10;
        }
    }

    // Nová třída Cashier
    class Cashier extends Process {
        public void actions() {
            while (true) {
                out();
                while (!waitingForPaymentCars.empty()) {
                    Car car = (Car) waitingForPaymentCars.first();
                    car.out(); //odstranění auta z fronty
                    hold(calculatePaymentTime()); //simulace času placení
                    activate(car);
                }
                wait(cashiers);
            }
        }
        // výpočet doby placení u kasy, který se řídí exponenciálním rozdělením
        private double calculatePaymentTime() {
            return random.negexp(1 / 2.0);
        }
    }

    class CarGenerator extends Process {
        public void actions() {
            while (time() <= simPeriod) {
                FuelType fuelType = random.nextBoolean() ? FuelType.GASOLINE : FuelType.DIESEL; // náhodná volba paliva auta
                activate(new Car(fuelType));
                hold(random.negexp(1 / 2.0));
            }
        }
    }
    public static void main(String args[]) {
        activate(new GasStationSimulation(2,2,1));
        activate(new GasStationSimulation(2,2,2));
        activate(new GasStationSimulation(2,2,3));
    }
    enum FuelType{
        GASOLINE, DIESEL
    } 
}