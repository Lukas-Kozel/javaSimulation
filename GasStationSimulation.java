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
    Random random = new Random(5);
    double totalServiceTime;
    int noOfServedCars, maxQueueLength,noOfCarsLeft, currentCarsOnStation,noOfGasolineCars,noOfDieselCars = 0;
    int maxCapacity = 5; //celková maximální kapacita aut na stanici
    long startTime = System.currentTimeMillis();

    GasStationSimulation(int gasolinePumps, int dieselPumps) {
        this.noOfGasolinePumps = gasolinePumps;
        this.noOfDieselPumps = dieselPumps;
    }
    public void actions() {
        for (int i = 0; i < noOfGasolinePumps; i++)
            new FuelPump("gasoline").into(gasolinePumps);
        for (int i = 0; i < noOfDieselPumps; i++)
            new FuelPump("diesel").into(dieselPumps);
        activate(new CarGenerator());
        hold(simPeriod + 1000000);
        report();
    }

    void report() {
        //System.out.println(noOfPumps + " fuel pump simulation");
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
        private double fuelLevel;
        private String fuelType; // Benzín nebo nafta

        public Car(String fuelType){
            Random rand = new Random();
            this.fuelLevel = Math.max(0, Math.min(rand.nextGaussian() * 10 + 30, 100));
            this.fuelType = fuelType; // Nastavení typu paliva pro auto
            if(this.fuelType.equals("gasoline")){
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
            Head waitingLine = this.fuelType.equals("gasoline") ? waitingGasolineCars : waitingDieselCars;
            if (waitingLine.cardinal() + currentCarsOnStation < maxCapacity) {
                into(waitingLine);
                currentCarsOnStation++;
                if (maxQueueLength < waitingLine.cardinal()) maxQueueLength = waitingLine.cardinal();
                Head pumpLine = this.fuelType.equals("gasoline") ? gasolinePumps : dieselPumps;
                if (!pumpLine.empty()) activate((FuelPump) pumpLine.first());
                passivate();
                currentCarsOnStation--;
                noOfServedCars++;
                totalServiceTime += time() - arrivalTime;
            } else {
                noOfCarsLeft++;
            }
        }
    }

    class FuelPump extends Process {
        private String fuelType; // Benzín nebo nafta

        public FuelPump(String fuelType) {
            this.fuelType = fuelType;
        }

        public void actions() {
            while (true) {
                out();
                Head waitingLine = this.fuelType.equals("gasoline") ? waitingGasolineCars : waitingDieselCars;
                while (!waitingLine.empty()) {
                    Car car = (Car) waitingLine.first();
                    car.out();
                    double fuelNeeded = 100 - car.getCarFuelLevel();
                    double refuelingTime = calculateRefuelingTime(fuelNeeded);
                    hold(refuelingTime);
                    activate(car);
                }
                wait(this.fuelType.equals("gasoline") ? gasolinePumps : dieselPumps);
            }
        }

        private double calculateRefuelingTime(double fuelNeeded) {
            return fuelNeeded / 10;
        }
    }

    class CarGenerator extends Process {
        public void actions() {
            while (time() <= simPeriod) {
                String fuelType = random.nextBoolean() ? "gasoline" : "diesel";
                activate(new Car(fuelType));
                hold(random.negexp(1 / 15.0));
            }
        }
    }
    public static void main(String args[]) {
        activate(new GasStationSimulation(1,1));
        activate(new GasStationSimulation(2,2));
        activate(new GasStationSimulation(4,4));
    } 
}
